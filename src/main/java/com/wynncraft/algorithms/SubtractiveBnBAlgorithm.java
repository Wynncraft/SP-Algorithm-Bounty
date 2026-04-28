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
 * Subtractive Branch-and-Bound with Witness-Driven Removal (SBBR).
 *
 * <p>Inverts the usual bottom-up DFS: starts from the full item set and
 * removes the minimum number of items needed to reach a valid state.
 * Search depth equals |N| − |S*| (items removed), typically 0–3 on real
 * Wynncraft builds where most items are mutually compatible.
 *
 * <p>Validity requires two conditions:
 * <ol>
 *   <li><b>Cascade</b>: for every active item i and required skill s,
 *       T[s] − bonus_i[s] ≥ req_i[s], where T is the total SP vector.</li>
 *   <li><b>Ordering</b>: there exists a permutation in which each item can
 *       be equipped when it is its turn (canEquip check). Detected via a
 *       two-phase greedy fixed-point.</li>
 * </ol>
 *
 * <p>Branching is complete: the chosen violator/stuck item plus negative
 * contributors on its worst skill cover all feasible supersets.
 *
 * <p>Items are never grouped or deduplicated: every {@link IEquipment}
 * occupies its own bit and array slot.
 */
@Information(name = "Subtractive BnB", version = 2, authors = {"Azael"})
public final class SubtractiveBnBAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final int SK = SkillPoint.values().length;
    private static final SkillPoint[] SP_VALS = SkillPoint.values();

    /**
     * Visited bitset arena (same scheme as CapyTopoAlgorithm). For n ≤ 22,
     * every explored mask is marked here; O(1) check/set with good cache
     * behaviour. Avoids the overflow risk of an open-addressing hash table
     * whose capacity can be exceeded for n ≥ 7 in top-down traversal.
     */
    private static final int N_VISITED_BITS = 22;
    private static final int VISITED_WORDS  = (1 << N_VISITED_BITS) / 64;

    /* ── Reusable per-run arrays (flat for cache locality) ── */

    /** Flat array: reqFlat[i*SK + s] = requirement of item i on skill s. */
    private int[] reqFlat   = new int[0];
    /** Flat array: bonusFlat[i*SK + s] = bonus of item i on skill s. */
    private int[] bonusFlat = new int[0];
    private int[]   weights = new int[0];
    private IEquipment[] itemArr = new IEquipment[0];
    private int n;

    /** T[s] = manual[s] + Σ bonus_j[s] for every j currently in active. */
    private final int[] T      = new int[SK];
    /** Manual SP allocation captured at each run() call. */
    private final int[] manual = new int[SK];

    /** negMask[s] = bitmask of items with bonus[i][s] < 0. */
    private final long[] negMask  = new long[SK];
    /** Bitmask of items with no SP requirement at all. */
    private long noReqMask;

    /* ── Best solution tracking ── */

    private int  bestCount;
    private int  bestWeight;
    private long bestMask;

    /* ── Visited state ── */

    private final long[] visited = new long[VISITED_WORDS];
    private boolean useVisited;

    /* ── Incremental setup cache ── */

    /** Number of items whose data is currently valid in the flat arrays. */
    private int cachedN = 0;
    /** Item references from the previous run, for prefix-match detection. */
    private IEquipment[] cachedItemRefs = new IEquipment[0];
    /** Manual SP values from the previous run, used to compute T delta. */
    private final int[] cachedManual = new int[SK];
    /** Sum of weights[i] for i in 0..cachedN-1 plus any new items added this run. */
    private int cachedTotalWeight = 0;

    /* ── Small scratch buffer for branch ordering ── */
    private final int[] branchOrder = new int[64];

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int eqSize = equipment.size();
        n = eqSize;

        // Read manual SP for this run
        for (int s = 0; s < SK; s++) manual[s] = player.allocated(SP_VALS[s]);

        // Grow arrays if needed
        if (reqFlat.length < eqSize * SK) {
            int cap = Math.max(eqSize, 32);
            reqFlat   = new int[cap * SK];
            bonusFlat = new int[cap * SK];
            weights   = new int[cap];
            itemArr   = new IEquipment[cap];
        }

        // Determine how many leading items are unchanged from previous run.
        // Incremental if: n >= cachedN and first cachedN item refs are identical.
        int startFrom = 0;
        if (n >= cachedN && cachedN > 0) {
            startFrom = cachedN;
            for (int i = 0; i < cachedN; i++) {
                if (equipment.get(i) != cachedItemRefs[i]) { startFrom = 0; break; }
            }
        }

        if (startFrom == 0) {
            // Full re-init: clear masks, rebuild T from scratch
            noReqMask = 0L;
            Arrays.fill(negMask, 0L);
            for (int s = 0; s < SK; s++) T[s] = manual[s];
            cachedTotalWeight = 0;
            for (int i = 0; i < n; i++) {
                setupItem(i, equipment.get(i));
                cachedTotalWeight += weights[i];
                T[0] += bonusFlat[i * SK];
                T[1] += bonusFlat[i * SK + 1];
                T[2] += bonusFlat[i * SK + 2];
                T[3] += bonusFlat[i * SK + 3];
                T[4] += bonusFlat[i * SK + 4];
            }
        } else {
            // Incremental: adjust T for manual-SP delta, then add only new items
            T[0] += manual[0] - cachedManual[0];
            T[1] += manual[1] - cachedManual[1];
            T[2] += manual[2] - cachedManual[2];
            T[3] += manual[3] - cachedManual[3];
            T[4] += manual[4] - cachedManual[4];
            for (int i = startFrom; i < n; i++) {
                setupItem(i, equipment.get(i));
                cachedTotalWeight += weights[i];
                T[0] += bonusFlat[i * SK];
                T[1] += bonusFlat[i * SK + 1];
                T[2] += bonusFlat[i * SK + 2];
                T[3] += bonusFlat[i * SK + 3];
                T[4] += bonusFlat[i * SK + 4];
            }
        }

        // Update cache for next run
        if (cachedItemRefs.length < n) cachedItemRefs = new IEquipment[Math.max(n, 32)];
        for (int i = startFrom; i < n; i++) cachedItemRefs[i] = itemArr[i];
        cachedN = n;
        System.arraycopy(manual, 0, cachedManual, 0, SK);

        long fullMask = (n == 64) ? -1L : (1L << n) - 1L;

        bestCount  = -1;
        bestWeight = Integer.MIN_VALUE;
        bestMask   = 0L;

        /* ── Fast path 0: full set already valid ── */
        boolean needsDfs = true;
        if (cascadePass(fullMask) && orderingPass(fullMask)) {
            bestCount  = n;
            bestWeight = cachedTotalWeight;
            bestMask   = fullMask;
            needsDfs   = false;
        }

        /* ── Fast path for n = 0 or 1 ── */
        if (needsDfs && n <= 1) {
            if (n == 0) {
                bestCount = 0; bestWeight = 0; bestMask = 0L;
            } else {
                // n == 1 and not valid → empty set is best
                bestCount = 0; bestWeight = 0; bestMask = 0L;
            }
            needsDfs = false;
        }

        /* ── Fast path 1: try removing a single item ── */
        if (needsDfs && n > 1) {
            // Find the worst violator (same logic as dfs root)
            int worstI = -1, worstS = -1, worstD = 0;
            long iter = fullMask & ~noReqMask;
            while (iter != 0L) {
                long bit = iter & -iter; iter ^= bit;
                int i = Long.numberOfTrailingZeros(bit);
                int b = i * SK;
                if (reqFlat[b] > 0) {
                    int d = reqFlat[b] + bonusFlat[b] - T[0];
                    if (d > worstD) { worstD = d; worstI = i; worstS = 0; }
                }
                if (reqFlat[b + 1] > 0) {
                    int d = reqFlat[b + 1] + bonusFlat[b + 1] - T[1];
                    if (d > worstD) { worstD = d; worstI = i; worstS = 1; }
                }
                if (reqFlat[b + 2] > 0) {
                    int d = reqFlat[b + 2] + bonusFlat[b + 2] - T[2];
                    if (d > worstD) { worstD = d; worstI = i; worstS = 2; }
                }
                if (reqFlat[b + 3] > 0) {
                    int d = reqFlat[b + 3] + bonusFlat[b + 3] - T[3];
                    if (d > worstD) { worstD = d; worstI = i; worstS = 3; }
                }
                if (reqFlat[b + 4] > 0) {
                    int d = reqFlat[b + 4] + bonusFlat[b + 4] - T[4];
                    if (d > worstD) { worstD = d; worstI = i; worstS = 4; }
                }
            }

            if (worstI != -1) {
                // Collect candidates: violator + negative contributors on worst skill
                int bc = 0;
                branchOrder[bc++] = worstI;
                long negs = fullMask & negMask[worstS] & ~(1L << worstI);
                while (negs != 0L) {
                    long bit = negs & -negs; negs ^= bit;
                    branchOrder[bc++] = Long.numberOfTrailingZeros(bit);
                }
                // Sort by ascending weight (most negative first → highest nextWeight)
                for (int a = 1; a < bc; a++) {
                    int key = branchOrder[a];
                    int j = a - 1;
                    while (j >= 0 && weights[branchOrder[j]] > weights[key]) {
                        branchOrder[j + 1] = branchOrder[j];
                        j--;
                    }
                    branchOrder[j + 1] = key;
                }

                for (int idx = 0; idx < bc; idx++) {
                    int i = branchOrder[idx];
                    int w = cachedTotalWeight - weights[i];
                    if (n - 1 < bestCount || (n - 1 == bestCount && w <= bestWeight)) continue;

                    int base = i * SK;
                    T[0] -= bonusFlat[base];
                    T[1] -= bonusFlat[base + 1];
                    T[2] -= bonusFlat[base + 2];
                    T[3] -= bonusFlat[base + 3];
                    T[4] -= bonusFlat[base + 4];

                    long m = fullMask ^ (1L << i);
                    if (cascadePass(m) && orderingPass(m)) {
                        bestCount  = n - 1;
                        bestWeight = w;
                        bestMask   = m;
                    }

                    T[0] += bonusFlat[base];
                    T[1] += bonusFlat[base + 1];
                    T[2] += bonusFlat[base + 2];
                    T[3] += bonusFlat[base + 3];
                    T[4] += bonusFlat[base + 4];
                }

                if (bestCount >= n - 1) {
                    needsDfs = false;
                }
            }
        }

        /* ── General DFS for deeper searches ── */
        if (needsDfs) {
            useVisited = (n <= N_VISITED_BITS);
            if (useVisited) {
                int wordsToClear = (1 << n) / 64 + 1;
                if (wordsToClear > VISITED_WORDS) wordsToClear = VISITED_WORDS;
                Arrays.fill(visited, 0, wordsToClear, 0L);
            }
            dfs(fullMask, n, cachedTotalWeight);
        }

        int validCount = (int) Long.bitCount(bestMask);
        List<IEquipment> valid   = new ArrayList<>(validCount);
        List<IEquipment> invalid = new ArrayList<>(n - validCount);
        for (int i = 0; i < n; i++) {
            if ((bestMask & (1L << i)) != 0L) valid.add(itemArr[i]);
            else invalid.add(itemArr[i]);
        }

        player.reset();
        for (int i = 0, sz = valid.size(); i < sz; i++) {
            player.modify(valid.get(i).bonuses(), true);
        }

        return new Result(valid, invalid);
    }

    /** Extract req/bonus/weight for item at index i and update masks. */
    private void setupItem(int i, IEquipment item) {
        int[] r = item.requirements();
        int[] b = item.bonuses();
        int base = i * SK;
        boolean hr = false;
        int w = 0;
        for (int s = 0; s < SK; s++) {
            reqFlat[base + s]   = r[s];
            bonusFlat[base + s] = b[s];
            if (r[s] > 0) hr = true;
            if (b[s] < 0) negMask[s] |= (1L << i);
            w += b[s];
        }
        weights[i] = w;
        if (!hr) noReqMask |= (1L << i);
        itemArr[i] = item;
    }

    /** Cascade check using current {@code T}. */
    private boolean cascadePass(long active) {
        long iter = active & ~noReqMask;
        while (iter != 0L) {
            long bit = iter & -iter; iter ^= bit;
            int i = Long.numberOfTrailingZeros(bit);
            int b = i * SK;
            if (reqFlat[b]     > 0 && reqFlat[b]     + bonusFlat[b]     > T[0]) return false;
            if (reqFlat[b + 1] > 0 && reqFlat[b + 1] + bonusFlat[b + 1] > T[1]) return false;
            if (reqFlat[b + 2] > 0 && reqFlat[b + 2] + bonusFlat[b + 2] > T[2]) return false;
            if (reqFlat[b + 3] > 0 && reqFlat[b + 3] + bonusFlat[b + 3] > T[3]) return false;
            if (reqFlat[b + 4] > 0 && reqFlat[b + 4] + bonusFlat[b + 4] > T[4]) return false;
        }
        return true;
    }

    /** Ordering check using current manual and bonuses. */
    private boolean orderingPass(long active) {
        int os0 = manual[0], os1 = manual[1], os2 = manual[2], os3 = manual[3], os4 = manual[4];
        long nra = active & noReqMask;
        while (nra != 0L) {
            long bit = nra & -nra; nra ^= bit;
            int i = Long.numberOfTrailingZeros(bit);
            int b = i * SK;
            if (bonusFlat[b]     > 0) os0 += bonusFlat[b];
            if (bonusFlat[b + 1] > 0) os1 += bonusFlat[b + 1];
            if (bonusFlat[b + 2] > 0) os2 += bonusFlat[b + 2];
            if (bonusFlat[b + 3] > 0) os3 += bonusFlat[b + 3];
            if (bonusFlat[b + 4] > 0) os4 += bonusFlat[b + 4];
        }

        long remaining = active & ~noReqMask;
        boolean progress = true;
        while (progress && remaining != 0L) {
            progress = false;
            long ri2 = remaining;
            while (ri2 != 0L) {
                long bit = ri2 & -ri2; ri2 ^= bit;
                int i = Long.numberOfTrailingZeros(bit);
                int b = i * SK;
                if ((reqFlat[b]     <= 0 || os0 >= reqFlat[b]) &&
                    (reqFlat[b + 1] <= 0 || os1 >= reqFlat[b + 1]) &&
                    (reqFlat[b + 2] <= 0 || os2 >= reqFlat[b + 2]) &&
                    (reqFlat[b + 3] <= 0 || os3 >= reqFlat[b + 3]) &&
                    (reqFlat[b + 4] <= 0 || os4 >= reqFlat[b + 4])) {
                    os0 += bonusFlat[b];
                    os1 += bonusFlat[b + 1];
                    os2 += bonusFlat[b + 2];
                    os3 += bonusFlat[b + 3];
                    os4 += bonusFlat[b + 4];
                    remaining ^= bit;
                    progress = true;
                }
            }
        }
        return remaining == 0L;
    }

    /**
     * Recursive core. T reflects exactly the bonuses of items in active on
     * entry; must be unchanged on exit.
     */
    private void dfs(long active, int count, int weight) {
        if (count <= 0) {
            if (bestCount < 0) { bestCount = 0; bestWeight = 0; bestMask = 0L; }
            return;
        }
        if (count < bestCount || (count == bestCount && weight <= bestWeight)) return;

        // ── Step 1: Cascade violation scan ─────────────────────────────
        int worstI = -1, worstS = -1, worstD = 0;
        long iter = active & ~noReqMask;
        while (iter != 0L) {
            long bit = iter & -iter; iter ^= bit;
            int i = Long.numberOfTrailingZeros(bit);
            int b = i * SK;
            if (reqFlat[b] > 0) {
                int d = reqFlat[b] + bonusFlat[b] - T[0];
                if (d > worstD) { worstD = d; worstI = i; worstS = 0; }
            }
            if (reqFlat[b + 1] > 0) {
                int d = reqFlat[b + 1] + bonusFlat[b + 1] - T[1];
                if (d > worstD) { worstD = d; worstI = i; worstS = 1; }
            }
            if (reqFlat[b + 2] > 0) {
                int d = reqFlat[b + 2] + bonusFlat[b + 2] - T[2];
                if (d > worstD) { worstD = d; worstI = i; worstS = 2; }
            }
            if (reqFlat[b + 3] > 0) {
                int d = reqFlat[b + 3] + bonusFlat[b + 3] - T[3];
                if (d > worstD) { worstD = d; worstI = i; worstS = 3; }
            }
            if (reqFlat[b + 4] > 0) {
                int d = reqFlat[b + 4] + bonusFlat[b + 4] - T[4];
                if (d > worstD) { worstD = d; worstI = i; worstS = 4; }
            }
        }

        if (worstI != -1) {
            // Cascade violated — branch on removing worstI or negative contributors.
            // Collect and sort candidates so best tie-break is found first.
            int bc = 0;
            branchOrder[bc++] = worstI;
            long negs = active & negMask[worstS] & ~(1L << worstI);
            while (negs != 0L) {
                long bit = negs & -negs; negs ^= bit;
                branchOrder[bc++] = Long.numberOfTrailingZeros(bit);
            }
            for (int a = 1; a < bc; a++) {
                int key = branchOrder[a];
                int j = a - 1;
                while (j >= 0 && weights[branchOrder[j]] > weights[key]) {
                    branchOrder[j + 1] = branchOrder[j];
                    j--;
                }
                branchOrder[j + 1] = key;
            }
            for (int idx = 0; idx < bc; idx++) {
                tryRemove(active, count, weight, branchOrder[idx]);
            }
            return;
        }

        // ── Step 2: Ordering check ─────────────────────────────────────
        int os0 = manual[0], os1 = manual[1], os2 = manual[2],
            os3 = manual[3], os4 = manual[4];
        long nra = active & noReqMask;
        while (nra != 0L) {
            long bit = nra & -nra; nra ^= bit;
            int i = Long.numberOfTrailingZeros(bit);
            int b = i * SK;
            if (bonusFlat[b]     > 0) os0 += bonusFlat[b];
            if (bonusFlat[b + 1] > 0) os1 += bonusFlat[b + 1];
            if (bonusFlat[b + 2] > 0) os2 += bonusFlat[b + 2];
            if (bonusFlat[b + 3] > 0) os3 += bonusFlat[b + 3];
            if (bonusFlat[b + 4] > 0) os4 += bonusFlat[b + 4];
        }

        long remaining = active & ~noReqMask;
        boolean progress = true;
        while (progress && remaining != 0L) {
            progress = false;
            long ri2 = remaining;
            while (ri2 != 0L) {
                long bit = ri2 & -ri2; ri2 ^= bit;
                int i = Long.numberOfTrailingZeros(bit);
                int b = i * SK;
                if ((reqFlat[b]     <= 0 || os0 >= reqFlat[b]) &&
                    (reqFlat[b + 1] <= 0 || os1 >= reqFlat[b + 1]) &&
                    (reqFlat[b + 2] <= 0 || os2 >= reqFlat[b + 2]) &&
                    (reqFlat[b + 3] <= 0 || os3 >= reqFlat[b + 3]) &&
                    (reqFlat[b + 4] <= 0 || os4 >= reqFlat[b + 4])) {
                    os0 += bonusFlat[b];
                    os1 += bonusFlat[b + 1];
                    os2 += bonusFlat[b + 2];
                    os3 += bonusFlat[b + 3];
                    os4 += bonusFlat[b + 4];
                    remaining ^= bit;
                    progress = true;
                }
            }
        }

        if (remaining == 0L) {
            bestCount  = count;
            bestWeight = weight;
            bestMask   = active;
            return;
        }

        // ── Step 3: Ordering stuck ────────────────────────────────────
        int stuckI = -1, stuckS = -1, stuckD = 0;
        long si = remaining;
        while (si != 0L) {
            long bit = si & -si; si ^= bit;
            int i = Long.numberOfTrailingZeros(bit);
            int b = i * SK;
            if (reqFlat[b] > 0) {
                int d = reqFlat[b] - os0;
                if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 0; }
            }
            if (reqFlat[b + 1] > 0) {
                int d = reqFlat[b + 1] - os1;
                if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 1; }
            }
            if (reqFlat[b + 2] > 0) {
                int d = reqFlat[b + 2] - os2;
                if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 2; }
            }
            if (reqFlat[b + 3] > 0) {
                int d = reqFlat[b + 3] - os3;
                if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 3; }
            }
            if (reqFlat[b + 4] > 0) {
                int d = reqFlat[b + 4] - os4;
                if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 4; }
            }
        }

        if (stuckI == -1) {
            bestCount = count; bestWeight = weight; bestMask = active;
            return;
        }

        // Branch on stuck item and negative contributors on stuck skill
        int bc = 0;
        branchOrder[bc++] = stuckI;
        long negs = active & negMask[stuckS] & ~(1L << stuckI);
        while (negs != 0L) {
            long bit = negs & -negs; negs ^= bit;
            branchOrder[bc++] = Long.numberOfTrailingZeros(bit);
        }
        for (int a = 1; a < bc; a++) {
            int key = branchOrder[a];
            int j = a - 1;
            while (j >= 0 && weights[branchOrder[j]] > weights[key]) {
                branchOrder[j + 1] = branchOrder[j];
                j--;
            }
            branchOrder[j + 1] = key;
        }
        for (int idx = 0; idx < bc; idx++) {
            tryRemove(active, count, weight, branchOrder[idx]);
        }
    }

    private void tryRemove(long active, int count, int weight, int i) {
        int nextCount  = count - 1;
        int nextWeight = weight - weights[i];
        if (nextCount < bestCount || (nextCount == bestCount && nextWeight <= bestWeight)) return;

        long next = active ^ (1L << i);

        if (next != 0L) {
            if (useVisited) {
                int idx  = (int) next;
                int word = idx >>> 6;
                long b   = 1L << (idx & 63);
                if ((visited[word] & b) != 0L) return;
                visited[word] |= b;
            }
        }

        int base = i * SK;
        T[0] -= bonusFlat[base];
        T[1] -= bonusFlat[base + 1];
        T[2] -= bonusFlat[base + 2];
        T[3] -= bonusFlat[base + 3];
        T[4] -= bonusFlat[base + 4];

        dfs(next, nextCount, nextWeight);

        T[0] += bonusFlat[base];
        T[1] += bonusFlat[base + 1];
        T[2] += bonusFlat[base + 2];
        T[3] += bonusFlat[base + 3];
        T[4] += bonusFlat[base + 4];
    }
}
