package com.wynncraft.algorithms;

import com.wynncraft.core.NegativeMaskCache;
import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;
import speiger.src.collections.ints.lists.IntArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the maximum valid subset of equipment a player can equip given their
 * allocated Skill Points and the requirements/bonuses of each item.
 *
 * <h2>Problem structure</h2>
 * Each item has a 5-dimensional SP requirement vector and a 5-dimensional bonus
 * vector. Equipping an item adds its bonuses to the player's total SP, which may
 * unlock further items. Items are split into two groups:
 * <ul>
 *   <li><b>Positive items</b> — all bonuses are &ge; 0. They can only raise SP
 *       totals, so a greedy fixed-point pass is provably optimal for this group.</li>
 *   <li><b>Negative items</b> — at least one bonus is &lt; 0. They can lower SP
 *       and therefore invalidate previously-equipped items, requiring exhaustive
 *       enumeration.</li>
 * </ul>
 *
 * <h2>Algorithm (5 phases)</h2>
 * <ol>
 *   <li><b>Precompute flat arrays.</b> All interface calls ({@code requirements()},
 *       {@code bonuses()}, {@code hasNegativeBonus()}) happen once up-front into
 *       primitive {@code int[][]} arrays. The hot path never touches the heap.</li>
 *
 *   <li><b>Pre-filter infeasible items.</b> For each item {@code i}, the theoretical
 *       maximum SP in dimension {@code k} is {@code base[k] + sum of all positive
 *       bonuses[j][k] for j != i}. If this ceiling is below {@code reqs[i][k]} for
 *       any {@code k}, the item can never be equipped regardless of what else is
 *       active — it is removed before any enumeration.</li>
 *
 *   <li><b>Greedy positive pass.</b> Iteratively equip any positive item whose
 *       requirements are currently satisfied. This converges to the globally optimal
 *       positive subset in O(n²) and produces an optimistic SP state ({@code cur})
 *       used by the feasibility pre-check in phase 4.</li>
 *
 *   <li><b>Bitmask enumeration of negative items.</b> Masks are pre-sorted
 *       descending by bit-count ({@link NegativeMaskCache}), enabling an early break
 *       once the remaining masks can no longer beat the current best count.
 *       For each mask:
 *       <ul>
 *         <li><i>Feasibility pre-check:</i> compute {@code cur + all neg bonuses in
 *             mask} and verify that every negative item in the mask could possibly
 *             meet its own requirements (excluding its own contribution). Fails fast
 *             without a full simulation.</li>
 *         <li><i>Simulation from base:</i> starting from the player's raw allocated
 *             SP (not the greedy state) avoids the "greedy trap" — the scenario where
 *             a positive item greedily activated early blocks a negative item that
 *             must logically precede it. The simulation interleaves negative and
 *             positive item activation until no further progress is possible.</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Build result.</b> Collect valid/invalid lists from the winning masks and
 *       return.</li>
 * </ol>
 *
 * <h2>Performance</h2>
 * All scratch arrays ({@code reqs}, {@code bonus}, {@code simSP}, etc.) are
 * pre-allocated as instance fields and reused across calls, keeping per-invocation
 * heap allocation to the two result {@link java.util.ArrayList}s only.
 */
@Information(name = "Pruned Mask", version = 1, authors = "kmaxi")
public class PrunedMaskAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final int MAX_ITEMS = 64;
    private static final int SP_COUNT  = SkillPoint.values().length; // 5
    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final NegativeMaskCache MASK_CACHE = new NegativeMaskCache();

    // ── Pre-allocated scratch space — reused across run() calls (not thread-safe) ──
    private final int[][] reqs   = new int[MAX_ITEMS][SP_COUNT];
    private final int[][] bonus  = new int[MAX_ITEMS][SP_COUNT];
    private final boolean[] hasNeg = new boolean[MAX_ITEMS];
    private final boolean[] infeas = new boolean[MAX_ITEMS];
    private final int[]     posIdx  = new int[MAX_ITEMS];
    private final int[]     negIdx  = new int[MAX_ITEMS];
    private final int[]     freeIdx = new int[MAX_ITEMS];
    private final boolean[] isFree  = new boolean[MAX_ITEMS];
    private final int[] totMax   = new int[SP_COUNT];
    private final int[] base     = new int[SP_COUNT];
    private final int[] cur      = new int[SP_COUNT];
    private final int[] simSP    = new int[SP_COUNT];

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int n = equipment.size();

        // ── 1. Precompute flat primitive arrays ─────────────────────────────────────
        // All interface dispatch happens here, once — never inside the hot path.
        for (int k = 0; k < SP_COUNT; k++) {
            base[k]   = player.allocated(SKILL_POINTS[k]);
            totMax[k] = 0;
        }
        for (int i = 0; i < n; i++) {
            IEquipment item = equipment.get(i);
            System.arraycopy(item.requirements(), 0, reqs[i],  0, SP_COUNT);
            System.arraycopy(item.bonuses(),       0, bonus[i], 0, SP_COUNT);
            hasNeg[i] = item.hasNegativeBonus();
            infeas[i] = false;
            isFree[i] = false;
        }

        // ── 2a. Pre-activate free items (no requirements, no negative bonus) ─────────
        // These are unconditionally optimal: always equippable, never reduce any SP
        // dimension, never invalidate other items. Adding their bonuses to base gives
        // all subsequent phases an accurate SP floor and raises the pruning baseline.
        int freeSize = 0;
        for (int i = 0; i < n; i++) {
            if (hasNeg[i]) continue;
            boolean noReq = reqs[i][0] <= 0 && reqs[i][1] <= 0 && reqs[i][2] <= 0
                         && reqs[i][3] <= 0 && reqs[i][4] <= 0;
            if (!noReq) continue;
            base[0] += bonus[i][0]; base[1] += bonus[i][1]; base[2] += bonus[i][2];
            base[3] += bonus[i][3]; base[4] += bonus[i][4];
            isFree[i] = true;
            freeIdx[freeSize++] = i;
        }

        // ── 2b. Pre-filter permanently infeasible items ───────────────────────────────
        // totMax = sum of positive bonus components from non-free items only (free
        // bonuses are already in base). Used to bound the maximum achievable SP.
        for (int k = 0; k < SP_COUNT; k++) totMax[k] = 0;
        for (int i = 0; i < n; i++) {
            if (isFree[i]) continue;
            if (bonus[i][0] > 0) totMax[0] += bonus[i][0];
            if (bonus[i][1] > 0) totMax[1] += bonus[i][1];
            if (bonus[i][2] > 0) totMax[2] += bonus[i][2];
            if (bonus[i][3] > 0) totMax[3] += bonus[i][3];
            if (bonus[i][4] > 0) totMax[4] += bonus[i][4];
        }

        int posSize = 0, negSize = 0;
        for (int i = 0; i < n; i++) {
            if (isFree[i]) continue; // always valid, skip feasibility check
            boolean possible = true;
            for (int k = 0; k < SP_COUNT; k++) {
                if (reqs[i][k] <= 0) continue;
                int self = Math.max(bonus[i][k], 0);
                if (base[k] + totMax[k] - self < reqs[i][k]) { possible = false; break; }
            }
            if (!possible) {
                infeas[i] = true;
            } else if (hasNeg[i]) {
                negIdx[negSize++] = i;
            } else {
                posIdx[posSize++] = i;
            }
        }

        // Safety caps — sort by descending net bonus before truncating so the
        // weakest contributors are dropped, not arbitrary tail elements.
        // Bitmask uses long (64 bits); keep posSize ≤ 62 to avoid sign-bit issues.
        if (posSize > 62) {
            sortByNetBonusDesc(posIdx, posSize);
            int dropped = posSize - 62;
            for (int pi = 62; pi < posSize; pi++) infeas[posIdx[pi]] = true;
            posSize = 62;
            System.err.printf("[PrunedMask] WARNING: %d positive items exceed the 62-item bitmask cap; " +
                    "%d lowest-bonus items marked infeasible — result may not be globally optimal%n",
                    posSize + dropped, dropped);
        }
        // NegativeMaskCache._cache has 18 entries (indices 0..17); cap negSize to 17.
        if (negSize > 17) {
            sortByNetBonusDesc(negIdx, negSize);
            int dropped = negSize - 17;
            for (int ni = 17; ni < negSize; ni++) infeas[negIdx[ni]] = true;
            negSize = 17;
            System.err.printf("[PrunedMask] WARNING: %d negative items exceed the NegativeMaskCache 17-item cap; " +
                    "%d lowest-bonus items marked infeasible — result may not be globally optimal%n",
                    negSize + dropped, dropped);
        }

        // ── 3. Greedy positive pass ─────────────────────────────────────────────────
        // Positive items can only add to SP totals, so greedy convergence is optimal.
        long posActive = 0L;
        System.arraycopy(base, 0, cur, 0, SP_COUNT);
        int posWeight = 0;
        {
            boolean changed = true;
            while (changed) {
                changed = false;
                //noinspection DuplicatedCode
                for (int pi = 0; pi < posSize; pi++) {
                    if ((posActive & (1L << pi)) != 0) continue;
                    if (meets(cur, reqs[posIdx[pi]])) {
                        posActive |= (1L << pi);
                        addSP(cur, bonus[posIdx[pi]]);
                        posWeight += sumSP(bonus[posIdx[pi]]);
                        changed = true;
                    }
                }
            }
        }

        // ── 4. Bitmask enumeration of negative items ────────────────────────────────
        // Masks are pre-sorted descending by bit-count, enabling early break.
        IntArrayList masks = MASK_CACHE.get(negSize);

        int  bestCount   = freeSize + Long.bitCount(posActive);
        int  bestWeight  = posWeight;
        long bestPosMask = posActive;
        int  bestNegMask = 0;

        for (int mi = 0; mi < masks.size(); mi++) {
            int mask = masks.getInt(mi);
            if (mask == 0) continue; // positive-only baseline already initialized above

            int negCount = Integer.bitCount(mask);
            int maxTotal = freeSize + negCount + posSize; // free items always count

            // All remaining masks have ≤ this many items; can't beat bestCount.
            if (maxTotal < bestCount) break;

            // ── Quick SP feasibility pre-check (uses cur as optimistic SP state) ───
            //
            // combined = cur + sum of all neg bonuses in mask.
            // For each neg item at index i in the mask:
            //   "Can its requirements be met, even in the BEST case, excluding its own
            //    contribution (since items can't bootstrap themselves)?"
            // = combined[k] - bonus[i][k] >= reqs[i][k]
            //
            // If this fails for any item, the mask is definitively invalid.
            System.arraycopy(cur, 0, simSP, 0, SP_COUNT); // dual-use: feasibility then simulation
            for (int ni = 0; ni < negSize; ni++) {
                if ((mask & (1 << ni)) != 0) addSP(simSP, bonus[negIdx[ni]]);
            }
            boolean feasFail = false;
            for (int ni = 0; ni < negSize; ni++) {
                if ((mask & (1 << ni)) == 0) continue;
                int i = negIdx[ni];
                for (int k = 0; k < SP_COUNT; k++) {
                    if (reqs[i][k] > 0 && (simSP[k] - bonus[i][k]) < reqs[i][k]) {
                        feasFail = true; break;
                    }
                }
                if (feasFail) break;
            }
            if (feasFail) continue;

            // ── Full simulation starting from BASE ───────────────────────────────────
            //
            // Key insight: starting from base (rather than from cur/greedily-active
            // positives) allows negative items to be interleaved with positives in the
            // correct order. A greedy positive pre-activation can block negative items
            // that must logically precede certain positives ("greedy trap").
            //
            // simSP currently holds combined (cur + all neg bonuses); we recycle it
            // by copying base into it, saving one allocation.
            System.arraycopy(base, 0, simSP, 0, SP_COUNT);
            long simPosActive = 0L; // no positives pre-activated
            int  negActive    = 0;
            int  finalWeight  = 0;

            boolean changed = true;
            while (changed) {
                changed = false;

                // Try adding each neg item in the mask
                //noinspection DuplicatedCode
                for (int ni = 0; ni < negSize; ni++) {
                    int bit = 1 << ni;
                    if ((mask & bit) == 0 || (negActive & bit) != 0) continue;
                    if (!meets(simSP, reqs[negIdx[ni]])) continue;

                    // Tentatively apply this neg item's bonus
                    addSP(simSP, bonus[negIdx[ni]]);

                    // Reject if it invalidates any currently-active positive item
                    boolean invalidates = false;
                    for (int pi = 0; pi < posSize; pi++) {
                        if ((simPosActive & (1L << pi)) != 0 && !meets(simSP, reqs[posIdx[pi]])) {
                            invalidates = true;
                            break;
                        }
                    }

                    if (invalidates) {
                        subSP(simSP, bonus[negIdx[ni]]); // rollback
                        continue;
                    }

                    negActive   |= bit;
                    finalWeight += sumSP(bonus[negIdx[ni]]);
                    changed = true;
                }

                // Try unlocking additional positive items with the updated SP state
                //noinspection DuplicatedCode
                for (int pi = 0; pi < posSize; pi++) {
                    if ((simPosActive & (1L << pi)) != 0) continue;
                    if (meets(simSP, reqs[posIdx[pi]])) {
                        simPosActive |= (1L << pi);
                        addSP(simSP, bonus[posIdx[pi]]);
                        finalWeight  += sumSP(bonus[posIdx[pi]]);
                        changed = true;
                    }
                }
            }

            // All neg items prescribed by the mask must have been activated
            if (negActive != mask) continue;

            // Final validation for active POSITIVE items only.
            //
            // Negative items are NOT re-validated here: their requirements were checked
            // at equipping time (simulation line above). Re-checking at the final state
            // would be incorrect because their own negative bonuses may have reduced SP
            // below their requirements — but those items were already validly equipped.
            boolean valid = true;
            //noinspection DuplicatedCode
            for (int pi = 0; pi < posSize; pi++) {
                if ((simPosActive & (1L << pi)) != 0 && !meets(simSP, reqs[posIdx[pi]])) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            int totalCount = freeSize + Long.bitCount(simPosActive) + negCount;
            if (totalCount < bestCount) continue;
            if (totalCount == bestCount && finalWeight <= bestWeight) continue;

            bestCount   = totalCount;
            bestWeight  = finalWeight;
            bestPosMask = simPosActive;
            bestNegMask = mask;
        }

        // ── 5. Build result lists from original IEquipment references ───────────────
        List<IEquipment> valid   = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (infeas[i]) invalid.add(equipment.get(i));
        }
        for (int fi = 0; fi < freeSize; fi++) {
            valid.add(equipment.get(freeIdx[fi])); // always valid
        }
        for (int pi = 0; pi < posSize; pi++) {
            ((bestPosMask & (1L << pi)) != 0 ? valid : invalid).add(equipment.get(posIdx[pi]));
        }
        for (int ni = 0; ni < negSize; ni++) {
            ((bestNegMask & (1 << ni)) != 0 ? valid : invalid).add(equipment.get(negIdx[ni]));
        }

        player.reset();
        for (IEquipment item : valid) {
            player.modify(item.bonuses(), true);
        }
        return new Result(valid, invalid);
    }

    // ── Static helpers — operate directly on int[SP_COUNT] ──────────────────────────

    /**
     * Returns true iff for every skill k where req[k] > 0, sp[k] >= req[k].
     * Mirrors Equipment.canEquip: a requirement of 0 is always satisfied,
     * regardless of the current SP value (which may be negative due to item bonuses).
     */
    private static boolean meets(int[] sp, int[] req) {
        return (req[0] <= 0 || sp[0] >= req[0])
            && (req[1] <= 0 || sp[1] >= req[1])
            && (req[2] <= 0 || sp[2] >= req[2])
            && (req[3] <= 0 || sp[3] >= req[3])
            && (req[4] <= 0 || sp[4] >= req[4]);
    }

    /** sp[k] += delta[k] for all k. */
    private static void addSP(int[] sp, int[] delta) {
        sp[0] += delta[0]; sp[1] += delta[1]; sp[2] += delta[2];
        sp[3] += delta[3]; sp[4] += delta[4];
    }

    /** sp[k] -= delta[k] for all k. */
    private static void subSP(int[] sp, int[] delta) {
        sp[0] -= delta[0]; sp[1] -= delta[1]; sp[2] -= delta[2];
        sp[3] -= delta[3]; sp[4] -= delta[4];
    }

    /** Returns the sum of all values in sp. */
    private static int sumSP(int[] sp) {
        return sp[0] + sp[1] + sp[2] + sp[3] + sp[4];
    }

    /** Returns the net bonus (sum of all bonus dimensions) for item at index {@code itemIdx}. */
    private int netBonus(int itemIdx) {
        int total = 0;
        for (int k = 0; k < SP_COUNT; k++) total += bonus[itemIdx][k];
        return total;
    }

    /**
     * Sorts {@code idx[0..size-1]} in-place by descending net bonus via insertion sort.
     * Used only when safety caps are triggered (size ≤ ~64), so O(n²) is acceptable.
     */
    private void sortByNetBonusDesc(int[] idx, int size) {
        for (int i = 1; i < size; i++) {
            int entry = idx[i];
            int entryBonus = netBonus(entry);
            int j = i - 1;
            while (j >= 0 && netBonus(idx[j]) < entryBonus) {
                idx[j + 1] = idx[j--];
            }
            idx[j + 1] = entry;
        }
    }

}
