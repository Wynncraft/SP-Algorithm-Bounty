package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Capy's Skill Point allocation algorithm (V2).
 *
 * <p>Models the problem as: find the largest subset S of equipment such that
 * there exists a permutation of S where (a) each item's requirements are met
 * by the player's allocated SP plus the bonuses from items already equipped
 * before it (per-step canEquip), AND (b) after each addition, every
 * previously-active item's "exclude-self" requirements are still satisfied
 * — i.e. for every active item, {@code state[s] >= req[s] + bonus[s]} on
 * every required skill (cascade isValid). Among sets of equal size, the
 * tiebreaker is the highest sum of skill point bonuses.
 *
 * <p>The cascade check is critical and is the rule used by Wynncraft
 * (see {@code WynnSolverAlgorithm} in hppeng-wynn/WynnSolverBench). The
 * earlier V1 used per-step canEquip only, which was permissive enough to
 * pass the bounty's shipped tests but over-counts on cases like
 * {@code case5_negativeInvalidatesPrior}.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Pre-activate "free" items (no requirements AND no negative
 *       bonuses). These never break any cascade and never block any future
 *       item, so they are unconditionally part of the optimum.</li>
 *   <li>Backtracking DFS over the remaining items. At each frame: try each
 *       unactivated item that (a) currently canEquip, and (b) does not
 *       break the isValid of any currently-active item. Recurse with that
 *       item activated; restore on backtrack.</li>
 *   <li>Track best by (count, weight). Prune any branch whose remaining
 *       items cannot push count above the current best.</li>
 * </ol>
 *
 * <p>An item with at least one positive requirement on skill {@code s} can
 * have its isValid invalidated by a later item that subtracts from
 * {@code state[s]} — even if that earlier item has only non-negative
 * bonuses itself. The previous algorithm's "greedy positives first"
 * shortcut is therefore unsound under the cascade rule, and is omitted
 * here.
 */
@Information(name = "Capy Topo", version = 2, authors = "Capy")
public class CapyTopoAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final int N_SKILLS = SkillPoint.values().length;

    /**
     * Maximum input size for which we maintain a visited bitset. The
     * arena is sized to {@code 1 << N_VISITED_BITS} bits; for inputs of
     * size {@code n > N_VISITED_BITS} we just skip the dedup and accept
     * possible re-exploration (the typical realistic Wynncraft case is
     * {@code n ≤ 22}, well within bounds).
     */
    private static final int N_VISITED_BITS = 22;
    private static final int VISITED_WORDS = (1 << N_VISITED_BITS) / 64;

    /* ===================== Reusable per-run state ===================== */

    private final int[] state = new int[N_SKILLS];
    private int[][] reqs = new int[0][];
    private int[][] bonuses = new int[0][];
    private int[] weights = new int[0];
    private boolean[] hasReq = new boolean[0];
    private boolean[] hasNegBonus = new boolean[0];
    private IEquipment[] itemArr = new IEquipment[0];
    private int n;
    private final long[] visited = new long[VISITED_WORDS];
    private boolean useVisited;

    private int bestCount;
    private int bestWeight;
    private long bestMask;

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int eqSize = equipment.size();

        if (itemArr.length < eqSize) {
            int cap = Math.max(eqSize, 32);
            reqs = new int[cap][];
            bonuses = new int[cap][];
            weights = new int[cap];
            hasReq = new boolean[cap];
            hasNegBonus = new boolean[cap];
            itemArr = new IEquipment[cap];
        }

        n = eqSize;
        for (int i = 0; i < n; i++) {
            IEquipment item = equipment.get(i);
            int[] r = item.requirements();
            int[] b = item.bonuses();
            reqs[i] = r;
            bonuses[i] = b;
            int w = 0;
            boolean hr = false, hn = false;
            for (int s = 0; s < N_SKILLS; s++) {
                if (r[s] > 0) hr = true;
                if (b[s] < 0) hn = true;
                w += b[s];
            }
            weights[i] = w;
            hasReq[i] = hr;
            hasNegBonus[i] = hn;
            itemArr[i] = item;
        }

        SkillPoint[] points = SkillPoint.values();
        for (int s = 0; s < N_SKILLS; s++) {
            state[s] = player.allocated(points[s]);
        }

        // Phase 1: pre-activate "free" items.
        // No req → canEquip is always true. No negative bonus → applying
        // never decreases any skill, so never breaks any other item's
        // isValid. These items are unconditionally beneficial: they always
        // belong in the optimum.
        long activeMask = 0L;
        int activeCount = 0;
        int activeWeight = 0;
        for (int i = 0; i < n; i++) {
            if (!hasReq[i] && !hasNegBonus[i]) {
                applyBonus(i, +1);
                activeMask |= 1L << i;
                activeCount++;
                activeWeight += weights[i];
            }
        }

        bestCount = activeCount;
        bestWeight = activeWeight;
        bestMask = activeMask;

        long allMask = (n == 64) ? -1L : (1L << n) - 1L;
        long remainingMask = allMask & ~activeMask;

        // Reset the visited arena. Only meaningful when n ≤ N_VISITED_BITS.
        useVisited = n <= N_VISITED_BITS;
        if (useVisited) {
            int wordsToClear = (1 << n) / 64 + 1;
            if (wordsToClear > VISITED_WORDS) wordsToClear = VISITED_WORDS;
            java.util.Arrays.fill(visited, 0, wordsToClear, 0L);
        }

        // Phase 2: backtracking with cascade isValid check.
        bt(activeMask, remainingMask, activeCount, activeWeight);

        // Reconstruct result lists.
        List<IEquipment> valid = new ArrayList<>(Long.bitCount(bestMask));
        List<IEquipment> invalid = new ArrayList<>(n - Long.bitCount(bestMask));
        for (int i = 0; i < n; i++) {
            if ((bestMask & (1L << i)) != 0) valid.add(itemArr[i]);
            else invalid.add(itemArr[i]);
        }

        player.reset();
        for (int i = 0; i < valid.size(); i++) {
            player.modify(valid.get(i).bonuses(), true);
        }

        return new Result(valid, invalid);
    }

    /**
     * Backtracking with cascade isValid check. On entry, {@code state}
     * has every item in {@code activeMask} applied; the method must leave
     * state unchanged on exit.
     */
    private void bt(long activeMask, long remainingMask, int count, int weight) {
        if (count > bestCount || (count == bestCount && weight > bestWeight)) {
            bestCount = count;
            bestWeight = weight;
            bestMask = activeMask;
        }

        // Upper-bound pruning. Even if every remaining item could be
        // activated, can we still beat the current best?
        if (count + Long.bitCount(remainingMask) <= bestCount) return;

        long iter = remainingMask;
        while (iter != 0) {
            long bit = iter & -iter;
            iter ^= bit;
            int i = Long.numberOfTrailingZeros(bit);

            if (!canEquip(i)) continue;

            // Visited dedup. State at activeMask|bit is fully determined
            // by the bits set, so if we've already explored from this
            // mask, redo would yield identical results.
            long newMask = activeMask | bit;
            if (useVisited) {
                int idx = (int) newMask;
                int word = idx >>> 6;
                long bitInWord = 1L << (idx & 63);
                if ((visited[word] & bitInWord) != 0L) continue;
                visited[word] |= bitInWord;
            }

            applyBonus(i, +1);

            // Cascade isValid check: every previously-active item's
            // (req + own_bonus) must remain ≤ state on its required
            // skills. Items added with no negative bonus don't reduce any
            // skill, so this check only fires when {@code i} subtracts.
            boolean cascadeOk = !hasNegBonus[i] || cascadeValid(activeMask);

            if (cascadeOk) {
                bt(newMask, remainingMask & ~bit, count + 1, weight + weights[i]);
            }

            applyBonus(i, -1);
        }
    }

    /** @return true iff every active item is still isValid in the current state. */
    private boolean cascadeValid(long activeMask) {
        long iter = activeMask;
        while (iter != 0) {
            long bit = iter & -iter;
            iter ^= bit;
            int j = Long.numberOfTrailingZeros(bit);
            if (!hasReq[j]) continue; // free items always isValid
            if (!isValid(j)) return false;
        }
        return true;
    }

    /** @return whether the player can currently equip item index i. */
    private boolean canEquip(int i) {
        int[] r = reqs[i];
        if (r[0] > 0 && state[0] < r[0]) return false;
        if (r[1] > 0 && state[1] < r[1]) return false;
        if (r[2] > 0 && state[2] < r[2]) return false;
        if (r[3] > 0 && state[3] < r[3]) return false;
        if (r[4] > 0 && state[4] < r[4]) return false;
        return true;
    }

    /**
     * Item is isValid iff for every required skill,
     * {@code state[s] >= req[s] + bonus[s]} — i.e. the player's SP
     * excluding the item's own contribution still meets the requirement.
     */
    private boolean isValid(int i) {
        int[] r = reqs[i];
        int[] b = bonuses[i];
        if (r[0] > 0 && state[0] < r[0] + b[0]) return false;
        if (r[1] > 0 && state[1] < r[1] + b[1]) return false;
        if (r[2] > 0 && state[2] < r[2] + b[2]) return false;
        if (r[3] > 0 && state[3] < r[3] + b[3]) return false;
        if (r[4] > 0 && state[4] < r[4] + b[4]) return false;
        return true;
    }

    private void applyBonus(int i, int sign) {
        int[] b = bonuses[i];
        state[0] += sign * b[0];
        state[1] += sign * b[1];
        state[2] += sign * b[2];
        state[3] += sign * b[3];
        state[4] += sign * b[4];
    }

}
