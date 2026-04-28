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
@Information(name = "Pruned Mask", version = 2, authors = "kmaxi & patrick")
public class PrunedMaskV2Algorithm implements IAlgorithm<WynnPlayer> {

    private static final int MAX_ITEMS = 64;
    private static final int SP_COUNT = 5;
    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final NegativeMaskCache MASK_CACHE = new NegativeMaskCache();

    // ── Pre-allocated scratch space — reused across run() calls (not thread-safe) ──
    private final int[][] reqs = new int[MAX_ITEMS][SP_COUNT];
    private final int[][] bonus = new int[MAX_ITEMS][SP_COUNT];
    private final int[] itemBonus = new int[MAX_ITEMS];
    private final boolean[] hasNeg = new boolean[MAX_ITEMS];
    private final boolean[] infeas = new boolean[MAX_ITEMS];
    private final int[] posIdx = new int[MAX_ITEMS];
    private final int[] negIdx = new int[MAX_ITEMS];
    private final int[] totMax = new int[SP_COUNT];
    private final int[] base = new int[SP_COUNT];
    private final int[] cur = new int[SP_COUNT];
    private final int[] simSP = new int[SP_COUNT];

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        List<IEquipment> valid = new ArrayList<>(64);
        List<IEquipment> invalid = new ArrayList<>(64);
        int n = equipment.size();

        // ── 1. Precompute flat primitive arrays ─────────────────────────────────────
        // All interface dispatch happens here, once — never inside the hot path.
        for (int k = 0; k < SP_COUNT; k++) {
            base[k] = player.allocated(SKILL_POINTS[k]);
            totMax[k] = 0;
        }

        int itemCount = 0;
        for (int i = 0; i < n; i++) {
            IEquipment item = equipment.get(i);
            int[] r = item.requirements();
            int[] b = item.bonuses();

            // skip valid items (no reqs + no bonus)
            boolean skip = true;
            for (int k = 0; k < r.length; ++k) {
                if (r[k] != 0 || b[k] != 0 || r[k] != b[k]) {
                    skip = false;
                    break;
                }
            }

            if (skip) {
                valid.add(item);
                continue;
            }

            System.arraycopy(r, 0, reqs[itemCount], 0, SP_COUNT);
            System.arraycopy(b, 0, bonus[itemCount], 0, SP_COUNT);
            itemBonus[itemCount] = b[0] + b[1] + b[2] + b[3] + b[4]; // pre-calc total bonus for every item
            hasNeg[itemCount] = item.hasNegativeBonus();
            infeas[itemCount] = false;
            itemCount++;
        }
        n = itemCount;

        // ── 2. Pre-filter permanently infeasible items ──────────────────────
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < SP_COUNT; k++) {
                if (bonus[i][k] > 0) {
                    totMax[k] += bonus[i][k];
                }
            }
        }

        int posSize = 0, negSize = 0;
        for (int i = 0; i < n; i++) {
            boolean possible = true;
            int[] r = reqs[i];
            int[] b = bonus[i];

            if ((r[0] > 0 && base[0] + totMax[0] - Math.max(b[0], 0) < r[0]) ||
                    (r[1] > 0 && base[1] + totMax[1] - Math.max(b[1], 0) < r[1]) ||
                    (r[2] > 0 && base[2] + totMax[2] - Math.max(b[2], 0) < r[2]) ||
                    (r[3] > 0 && base[3] + totMax[3] - Math.max(b[3], 0) < r[3]) ||
                    (r[4] > 0 && base[4] + totMax[4] - Math.max(b[4], 0) < r[4])) {
                possible = false;
            }

            if (!possible) {
                infeas[i] = true;
            } else if (hasNeg[i]) {
                negIdx[negSize++] = i;
            } else {
                posIdx[posSize++] = i;
            }
        }

        // Safety caps
        //if (posSize > 30) {
        //    sortByNetBonusDesc(posIdx, posSize);
        //    posSize = 30;
        //}

        // NegativeMaskCache._cache has 18 entries (indices 0..17); cap negSize to 17.
        //if (negSize > 17) {
        //    sortByNetBonusDesc(negIdx, negSize);
        //    negSize = 17;
        //}

        // ── 3. Greedy positive pass ─────────────────────────────────────────
        int posActive = 0;
        System.arraycopy(base, 0, cur, 0, SP_COUNT);
        int posWeight = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int pi = 0; pi < posSize; pi++) {
                if ((posActive & (1 << pi)) != 0) continue;
                if (meets(cur, reqs[posIdx[pi]])) {
                    posActive |= (1 << pi);
                    addSP(cur, bonus[posIdx[pi]]);
                    posWeight += itemBonus[posIdx[pi]];
                    changed = true;
                }
            }
        }

        // ── 4. Bitmask enumeration ──────────────────────────────────────────
        IntArrayList masks = MASK_CACHE.get(negSize);
        int bestCount = Integer.bitCount(posActive);
        int bestWeight = posWeight;
        int bestPosMask = posActive;
        int bestNegMask = 0;

        for (int mi = 0; mi < masks.size(); mi++) {
            int mask = masks.getInt(mi);
            if (mask == 0) continue;

            int negCount = Integer.bitCount(mask);
            if (negCount + posSize < bestCount) break;

            // Feasibility check (Dual-use simSP)
            System.arraycopy(cur, 0, simSP, 0, SP_COUNT);
            for (int ni = 0; ni < negSize; ni++) {
                if ((mask & (1 << ni)) != 0) addSP(simSP, bonus[negIdx[ni]]);
            }

            boolean feasFail = false;
            for (int ni = 0; ni < negSize; ni++) {
                if ((mask & (1 << ni)) == 0) continue;
                int i = negIdx[ni];
                int[] r = reqs[i];
                int[] b = bonus[i];
                if ((r[0] > 0 && simSP[0] - b[0] < r[0]) ||
                        (r[1] > 0 && simSP[1] - b[1] < r[1]) ||
                        (r[2] > 0 && simSP[2] - b[2] < r[2]) ||
                        (r[3] > 0 && simSP[3] - b[3] < r[3]) ||
                        (r[4] > 0 && simSP[4] - b[4] < r[4])) {
                    feasFail = true;
                    break;
                }
            }
            if (feasFail) continue;

            // Full simulation
            System.arraycopy(base, 0, simSP, 0, SP_COUNT);
            int simPosActive = 0; // no positives pre-activated
            int negActive = 0;
            int finalWeight = 0;

            changed = true;
            while (changed) {
                changed = false;

                // Try adding each neg item in the mask
                for (int ni = 0; ni < negSize; ni++) {
                    int bit = 1 << ni;
                    if ((mask & bit) == 0 || (negActive & bit) != 0) continue;
                    if (!meets(simSP, reqs[negIdx[ni]])) continue;

                    // Tentatively apply this neg item's bonus
                    addSP(simSP, bonus[negIdx[ni]]);

                    // Reject if it invalidates any currently-active positive item
                    boolean invalidates = false;
                    for (int pi = 0; pi < posSize; pi++) {
                        if ((simPosActive & (1 << pi)) != 0 && !meets(simSP, reqs[posIdx[pi]])) {
                            invalidates = true;
                            break;
                        }
                    }

                    if (invalidates) {
                        subSP(simSP, bonus[negIdx[ni]]);
                        continue;
                    }

                    negActive |= bit;
                    finalWeight += itemBonus[negIdx[ni]];
                    changed = true;
                }

                // Try unlocking additional positive items with the updated SP state
                for (int pi = 0; pi < posSize; pi++) {
                    if ((simPosActive & (1 << pi)) != 0) continue;
                    if (meets(simSP, reqs[posIdx[pi]])) {
                        simPosActive |= (1 << pi);
                        addSP(simSP, bonus[posIdx[pi]]);
                        finalWeight += itemBonus[posIdx[pi]];
                        changed = true;
                    }
                }
            }

            // All neg items prescribed by the mask must have been activated
            if (negActive != mask) {
                continue;
            }

            int totalCount = Integer.bitCount(simPosActive) + negCount;
            if (totalCount > bestCount || (totalCount == bestCount && finalWeight > bestWeight)) {
                bestCount = totalCount;
                bestWeight = finalWeight;
                bestPosMask = simPosActive;
                bestNegMask = mask;
            }
        }

        // ── 5. Build Result ────────────────────────────────────────────────-
        for (int i = 0; i < n; i++) {
            if (infeas[i]) {
                invalid.add(equipment.get(i));
            }
        }

        for (int pi = 0; pi < posSize; pi++) {
            ((bestPosMask & (1 << pi)) != 0 ? valid : invalid).add(equipment.get(posIdx[pi]));
        }
        for (int ni = 0; ni < negSize; ni++) {
            ((bestNegMask & (1 << ni)) != 0 ? valid : invalid).add(equipment.get(negIdx[ni]));
        }

        player.reset();
        for (int i = 0; i < valid.size(); i++) {
            player.modify(valid.get(i).bonuses(), true);
        }

        return new Result(valid, invalid);
    }

    /**
     * Returns true iff for every skill k where req[k] > 0, sp[k] >= req[k].
     * Mirrors Equipment.canEquip: a requirement of 0 is always satisfied,
     * regardless of the current SP value (which may be negative due to item bonuses).
     */
    private static boolean meets(int[] sp, int[] req) {
        if (req[0] > 0 && sp[0] < req[0]) return false;
        if (req[1] > 0 && sp[1] < req[1]) return false;
        if (req[2] > 0 && sp[2] < req[2]) return false;
        if (req[3] > 0 && sp[3] < req[3]) return false;
        if (req[4] > 0 && sp[4] < req[4]) return false;
        return true;
    }

    private static void addSP(int[] sp, int[] delta) {
        sp[0] += delta[0];
        sp[1] += delta[1];
        sp[2] += delta[2];
        sp[3] += delta[3];
        sp[4] += delta[4];
    }

    /** sp[k] -= delta[k] for all k. */
    private static void subSP(int[] sp, int[] delta) {
        for (int k = 0; k < SP_COUNT; k++) sp[k] -= delta[k];
    }

}
