package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * DFS over deduplicated item groups. Identical items (same object reference)
 * are collapsed into a single group whose scaledBonus = N × bonus_per_copy and
 * count = N. This reduces the DFS branching factor from ~22 items to ~9-10
 * groups for typical Wynncraft builds.
 *
 * Free groups (no req, no negative bonus) are pre-activated, identical to
 * CapyTopoAlgorithm's phase-1. The DFS then backtracks over remaining groups
 * with cascade-isValid checks, upper-bound pruning, and a visited bitset.
 */
@Information(name = "Wynn Dedupe Branch", version = 1, authors = "azael")
public class WynnDeduplicatedBranchAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int N_SKILLS = SKILL_POINTS.length; // 5

    private static final int N_VISITED_BITS = 22;
    private static final int VISITED_WORDS  = (1 << N_VISITED_BITS) / 64;

    // ── per-run state ────────────────────────────────────────────────────────

    private final int[] state = new int[N_SKILLS];

    // per-group buffers; resized lazily
    private int cap = 0;
    private IEquipment[] groupTemplate = new IEquipment[0];
    private int[][]      groupReqs     = new int[0][];
    private int[][]      groupThresh   = new int[0][];  // req[s] + bonus_per_copy[s]
    private int[][]      groupScaled   = new int[0][];  // sum of all copies' bonuses
    private int[]        groupWeights  = new int[0];
    private int[]        groupCounts   = new int[0];
    private boolean[]    hasReq        = new boolean[0];
    private boolean[]    hasNegBonus   = new boolean[0];
    private int[]        groupsWithReq = new int[0];
    private int          groupsWithReqCount;

    // per-item group-id lookup (equipment index → group index)
    private int[] equipGroupId = new int[0];

    private int n;  // number of unique groups

    private final long[] visited    = new long[VISITED_WORDS];
    private boolean      useVisited;

    private int  bestCount;
    private int  bestWeight;
    private long bestMask;

    // ────────────────────────────────────────────────────────────────────────

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int eqSize = equipment.size();

        // Grow instance buffers if needed
        if (eqSize > cap) {
            int newCap = Math.max(eqSize, 32);
            groupTemplate = new IEquipment[newCap];
            groupReqs     = new int[newCap][];
            groupWeights  = new int[newCap];
            groupCounts   = new int[newCap];
            hasReq        = new boolean[newCap];
            hasNegBonus   = new boolean[newCap];
            groupsWithReq = new int[newCap];
            equipGroupId  = new int[newCap];
            // pre-alloc inner arrays that are reused in-place
            int[][] newThresh  = new int[newCap][];
            int[][] newScaled  = new int[newCap][];
            for (int i = 0; i < newCap; i++) {
                newThresh[i] = new int[N_SKILLS];
                newScaled[i] = new int[N_SKILLS];
            }
            groupThresh = newThresh;
            groupScaled = newScaled;
            cap = newCap;
        }

        // ── Deduplicate: O(N²) reference-equality grouping (N ≤ 22) ─────────
        n = 0;
        groupsWithReqCount = 0;
        for (int k = 0; k < eqSize; k++) {
            IEquipment item = equipment.get(k);

            // Find existing group by reference
            int gid = -1;
            for (int g = 0; g < n; g++) {
                if (groupTemplate[g] == item) { gid = g; break; }
            }

            if (gid == -1) {
                gid = n++;
                int[] req = item.requirements();
                int[] bon = item.bonuses();

                groupTemplate[gid] = item;
                groupReqs[gid]     = req;
                groupCounts[gid]   = 1;
                groupWeights[gid]  = weight(bon);

                int[] sc  = groupScaled[gid];
                sc[0] = bon[0]; sc[1] = bon[1]; sc[2] = bon[2]; sc[3] = bon[3]; sc[4] = bon[4];

                boolean hr = req[0]>0||req[1]>0||req[2]>0||req[3]>0||req[4]>0;
                boolean hn = bon[0]<0||bon[1]<0||bon[2]<0||bon[3]<0||bon[4]<0;
                hasReq[gid]      = hr;
                hasNegBonus[gid] = hn;

                int[] thr = groupThresh[gid];
                thr[0] = hr && req[0]>0 ? req[0]+bon[0] : Integer.MIN_VALUE;
                thr[1] = hr && req[1]>0 ? req[1]+bon[1] : Integer.MIN_VALUE;
                thr[2] = hr && req[2]>0 ? req[2]+bon[2] : Integer.MIN_VALUE;
                thr[3] = hr && req[3]>0 ? req[3]+bon[3] : Integer.MIN_VALUE;
                thr[4] = hr && req[4]>0 ? req[4]+bon[4] : Integer.MIN_VALUE;

                if (hr) groupsWithReq[groupsWithReqCount++] = gid;
            } else {
                // Merge: accumulate scaled bonus
                int[] bon = item.bonuses();
                int[] sc  = groupScaled[gid];
                sc[0] += bon[0]; sc[1] += bon[1]; sc[2] += bon[2]; sc[3] += bon[3]; sc[4] += bon[4];
                groupWeights[gid] += weight(bon);
                groupCounts[gid]++;
            }

            equipGroupId[k] = gid;
        }

        // ── Set state from player's allocated SP ─────────────────────────────
        for (int s = 0; s < N_SKILLS; s++) state[s] = player.allocated(SKILL_POINTS[s]);

        // ── Phase 1: pre-activate free groups ───────────────────────────────
        long activeMask   = 0L;
        int  activeCount  = 0;
        int  activeWeight = 0;
        for (int i = 0; i < n; i++) {
            if (!hasReq[i] && !hasNegBonus[i]) {
                applyBonus(i, +1);
                activeMask    |= 1L << i;
                activeCount   += groupCounts[i];
                activeWeight  += groupWeights[i];
            }
        }

        bestCount  = activeCount;
        bestWeight = activeWeight;
        bestMask   = activeMask;

        long allMask       = (n == 64) ? -1L : (1L << n) - 1L;
        long remainingMask = allMask & ~activeMask;

        // Pre-sum count of items in remaining (non-free) groups for pruning
        int remainingCount = 0;
        {
            long iter = remainingMask;
            while (iter != 0) {
                long bit = iter & -iter; iter ^= bit;
                remainingCount += groupCounts[Long.numberOfTrailingZeros(bit)];
            }
        }

        // ── Visited-set setup ────────────────────────────────────────────────
        useVisited = n <= N_VISITED_BITS;
        if (useVisited) {
            int wordsToClear = (1 << n) / 64 + 1;
            if (wordsToClear > VISITED_WORDS) wordsToClear = VISITED_WORDS;
            Arrays.fill(visited, 0, wordsToClear, 0L);
        }

        // ── Phase 2: backtracking DFS ────────────────────────────────────────
        bt(activeMask, remainingMask, activeCount, activeWeight, remainingCount);

        // ── Reconstruct result ───────────────────────────────────────────────
        List<IEquipment> valid   = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();
        for (int k = 0; k < eqSize; k++) {
            int gid = equipGroupId[k];
            if ((bestMask & (1L << gid)) != 0) valid.add(equipment.get(k));
            else                                 invalid.add(equipment.get(k));
        }

        player.reset();
        for (int i = 0; i < n; i++) {
            if ((bestMask & (1L << i)) != 0) player.modify(groupScaled[i], true);
        }

        return new Result(valid, invalid);
    }

    private void bt(long activeMask, long remainingMask, int count, int weight, int remainingCount) {
        if (count > bestCount || (count == bestCount && weight > bestWeight)) {
            bestCount  = count;
            bestWeight = weight;
            bestMask   = activeMask;
        }

        if (count + remainingCount <= bestCount) return;

        long iter = remainingMask;
        while (iter != 0) {
            long bit  = iter & -iter;
            iter ^= bit;
            int i = Long.numberOfTrailingZeros(bit);

            if (!canEquip(i)) continue;

            long newMask = activeMask | bit;
            if (useVisited) {
                int  idx       = (int) newMask;
                int  word      = idx >>> 6;
                long bitInWord = 1L << (idx & 63);
                if ((visited[word] & bitInWord) != 0L) continue;
                visited[word] |= bitInWord;
            }

            applyBonus(i, +1);

            boolean cascadeOk = !hasNegBonus[i] || cascadeValid(activeMask);

            if (cascadeOk) {
                bt(newMask, remainingMask & ~bit,
                   count + groupCounts[i], weight + groupWeights[i],
                   remainingCount - groupCounts[i]);
            }

            applyBonus(i, -1);
        }
    }

    private boolean cascadeValid(long activeMask) {
        for (int k = 0; k < groupsWithReqCount; k++) {
            int j = groupsWithReq[k];
            if ((activeMask & (1L << j)) == 0) continue;
            int[] thr = groupThresh[j];
            if (state[0] < thr[0]) return false;
            if (state[1] < thr[1]) return false;
            if (state[2] < thr[2]) return false;
            if (state[3] < thr[3]) return false;
            if (state[4] < thr[4]) return false;
        }
        return true;
    }

    private boolean canEquip(int i) {
        int[] r = groupReqs[i];
        if (r[0] > 0 && state[0] < r[0]) return false;
        if (r[1] > 0 && state[1] < r[1]) return false;
        if (r[2] > 0 && state[2] < r[2]) return false;
        if (r[3] > 0 && state[3] < r[3]) return false;
        if (r[4] > 0 && state[4] < r[4]) return false;
        return true;
    }

    private void applyBonus(int i, int sign) {
        int[] b = groupScaled[i];
        state[0] += sign * b[0];
        state[1] += sign * b[1];
        state[2] += sign * b[2];
        state[3] += sign * b[3];
        state[4] += sign * b[4];
    }

    private static int weight(int[] b) {
        return b[0] + b[1] + b[2] + b[3] + b[4];
    }
}
