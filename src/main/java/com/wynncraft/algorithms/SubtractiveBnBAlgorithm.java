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
 *
 * <p><b>Incremental setup cache:</b> when successive {@code run()} calls
 * share a common item prefix (e.g. one-by-one sweeps), per-item data
 * (req, bonus, weights, masks) is reused for the unchanged prefix and
 * only new items are extracted. T is adjusted by the manual-SP delta.
 */
@Information(name = "Subtractive BnB", version = 1, authors = {"Azael"})
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

    /* ── Reusable per-run arrays ── */

    private int[][] req     = new int[0][];
    private int[][] bonus   = new int[0][];
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

    /** Number of items whose data is currently valid in req/bonus/weights/masks/T. */
    private int cachedN = 0;
    /** Item references from the previous run, for prefix-match detection. */
    private IEquipment[] cachedItemRefs = new IEquipment[0];
    /** Manual SP values from the previous run, used to compute T delta. */
    private final int[] cachedManual = new int[SK];
    /** Sum of weights[i] for i in 0..cachedN-1 plus any new items added this run. */
    private int cachedTotalWeight = 0;

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int eqSize = equipment.size();
        n = eqSize;

        // Read manual SP for this run
        for (int s = 0; s < SK; s++) manual[s] = player.allocated(SP_VALS[s]);

        // Grow arrays if needed
        if (req.length < eqSize) {
            int cap = Math.max(eqSize, 32);
            req      = new int[cap][];
            bonus    = new int[cap][];
            weights  = new int[cap];
            itemArr  = new IEquipment[cap];
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
                T[0] += bonus[i][0]; T[1] += bonus[i][1]; T[2] += bonus[i][2];
                T[3] += bonus[i][3]; T[4] += bonus[i][4];
            }
        } else {
            // Incremental: adjust T for manual-SP delta, then add only new items
            T[0] += manual[0] - cachedManual[0]; T[1] += manual[1] - cachedManual[1];
            T[2] += manual[2] - cachedManual[2]; T[3] += manual[3] - cachedManual[3];
            T[4] += manual[4] - cachedManual[4];
            for (int i = startFrom; i < n; i++) {
                setupItem(i, equipment.get(i));
                cachedTotalWeight += weights[i];
                T[0] += bonus[i][0]; T[1] += bonus[i][1]; T[2] += bonus[i][2];
                T[3] += bonus[i][3]; T[4] += bonus[i][4];
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

        // Initialise visited arena
        useVisited = (n <= N_VISITED_BITS);
        if (useVisited) {
            int wordsToClear = (1 << n) / 64 + 1;
            if (wordsToClear > VISITED_WORDS) wordsToClear = VISITED_WORDS;
            Arrays.fill(visited, 0, wordsToClear, 0L);
        }

        dfs(fullMask, n, cachedTotalWeight);

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
        req[i]   = item.requirements();
        bonus[i] = item.bonuses();
        boolean hr = false;
        int w = 0;
        for (int s = 0; s < SK; s++) {
            if (req[i][s] > 0) hr = true;
            if (bonus[i][s] < 0) negMask[s] |= (1L << i);
            w += bonus[i][s];
        }
        weights[i] = w;
        if (!hr) noReqMask |= (1L << i);
        itemArr[i] = item;
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
            int[] ri = req[i], bi = bonus[i];
            if (ri[0] > 0) { int d = ri[0] + bi[0] - T[0]; if (d > worstD) { worstD = d; worstI = i; worstS = 0; } }
            if (ri[1] > 0) { int d = ri[1] + bi[1] - T[1]; if (d > worstD) { worstD = d; worstI = i; worstS = 1; } }
            if (ri[2] > 0) { int d = ri[2] + bi[2] - T[2]; if (d > worstD) { worstD = d; worstI = i; worstS = 2; } }
            if (ri[3] > 0) { int d = ri[3] + bi[3] - T[3]; if (d > worstD) { worstD = d; worstI = i; worstS = 3; } }
            if (ri[4] > 0) { int d = ri[4] + bi[4] - T[4]; if (d > worstD) { worstD = d; worstI = i; worstS = 4; } }
        }

        if (worstI != -1) {
            tryRemove(active, count, weight, worstI);
            long negs = active & negMask[worstS] & ~(1L << worstI);
            while (negs != 0L) {
                long bit = negs & -negs; negs ^= bit;
                tryRemove(active, count, weight, Long.numberOfTrailingZeros(bit));
            }
            return;
        }

        // ── Step 2: Ordering check ─────────────────────────────────────
        // A set may pass cascade yet have no valid equipping sequence
        // (co-bootstrap). Detect with a two-phase greedy fixed-point.
        //
        // Phase 2a: seed ordering state with manual plus positive bonuses
        // from no-req items (always equippable first, only help others).
        int os0 = manual[0], os1 = manual[1], os2 = manual[2],
            os3 = manual[3], os4 = manual[4];
        long nra = active & noReqMask;
        while (nra != 0L) {
            long bit = nra & -nra; nra ^= bit;
            int i = Long.numberOfTrailingZeros(bit);
            if (bonus[i][0] > 0) os0 += bonus[i][0];
            if (bonus[i][1] > 0) os1 += bonus[i][1];
            if (bonus[i][2] > 0) os2 += bonus[i][2];
            if (bonus[i][3] > 0) os3 += bonus[i][3];
            if (bonus[i][4] > 0) os4 += bonus[i][4];
        }

        // Phase 2b: greedy equipping for req-items only.
        // No-req items with negative bonuses are deferred to last (always
        // equippable there; cascade already passed so isValid still holds).
        long remaining = active & ~noReqMask;
        boolean progress = true;
        while (progress && remaining != 0L) {
            progress = false;
            long ri2 = remaining;
            while (ri2 != 0L) {
                long bit = ri2 & -ri2; ri2 ^= bit;
                int i = Long.numberOfTrailingZeros(bit);
                int[] r = req[i];
                if ((r[0] <= 0 || os0 >= r[0]) &&
                    (r[1] <= 0 || os1 >= r[1]) &&
                    (r[2] <= 0 || os2 >= r[2]) &&
                    (r[3] <= 0 || os3 >= r[3]) &&
                    (r[4] <= 0 || os4 >= r[4])) {
                    os0 += bonus[i][0]; os1 += bonus[i][1]; os2 += bonus[i][2];
                    os3 += bonus[i][3]; os4 += bonus[i][4];
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
            int[] r = req[i];
            if (r[0] > 0) { int d = r[0] - os0; if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 0; } }
            if (r[1] > 0) { int d = r[1] - os1; if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 1; } }
            if (r[2] > 0) { int d = r[2] - os2; if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 2; } }
            if (r[3] > 0) { int d = r[3] - os3; if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 3; } }
            if (r[4] > 0) { int d = r[4] - os4; if (d > stuckD) { stuckD = d; stuckI = i; stuckS = 4; } }
        }

        if (stuckI == -1) {
            // All remaining were no-req; can't happen (they're excluded from remaining).
            bestCount = count; bestWeight = weight; bestMask = active;
            return;
        }

        tryRemove(active, count, weight, stuckI);
        long negs = active & negMask[stuckS] & ~(1L << stuckI);
        while (negs != 0L) {
            long bit = negs & -negs; negs ^= bit;
            tryRemove(active, count, weight, Long.numberOfTrailingZeros(bit));
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
            // For n > N_VISITED_BITS: skip memoisation (rare; accept possible
            // re-exploration rather than risk hash-table overflow).
        }

        T[0] -= bonus[i][0]; T[1] -= bonus[i][1]; T[2] -= bonus[i][2];
        T[3] -= bonus[i][3]; T[4] -= bonus[i][4];

        dfs(next, nextCount, nextWeight);

        T[0] += bonus[i][0]; T[1] += bonus[i][1]; T[2] += bonus[i][2];
        T[3] += bonus[i][3]; T[4] += bonus[i][4];
    }
}
