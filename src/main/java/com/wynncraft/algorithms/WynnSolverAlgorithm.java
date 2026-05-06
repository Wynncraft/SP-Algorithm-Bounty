package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of WynnSolver's cascade activation algorithm from
 * Alex-Guha/wynnbuilder-beta.github.io/js/game/skillpoints.js
 *
 * The JS algorithm computes minimum assigned SP to equip all items using
 * backtracking over activation orderings. This port adapts it to the checker
 * interface: given fixed assigned SP, find the maximum set of items equippable
 * under cascade activation mechanics.
 *
 * Phase 1: Activate free items (no requirements, non-negative bonuses).
 * These are always safe to activate first since they only increase SP.
 * Phase 2: Backtracking search over activation orderings for remaining items
 * (those with requirements or negative bonuses). At each step:
 * - Try activating an unactivated item if canEquip succeeds
 * - After applying its bonuses, verify all previously activated items
 * remain valid (isValid check — the cascade sustainability constraint)
 * - Track the best (most items activated, then highest total bonus weight)
 */
@Information(name = "WynnSolver", version = 1, authors = {"Alex-Guha"})
public class WynnSolverAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        IEquipment[] items = equipment.toArray(new IEquipment[0]);
        int[] assignedSP = new int[SKILL_POINTS.length];
        for (int i = 0; i < SKILL_POINTS.length; i++) {
            assignedSP[i] = player.allocated(SKILL_POINTS[i]);
        }

        boolean[] keep = check(items, assignedSP);

        List<IEquipment> valid = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            if (keep[i]) {
                valid.add(items[i]);
                player.modify(items[i].bonuses(), true);
            } else {
                invalid.add(items[i]);
            }
        }
        return new Result(valid, invalid);
    }

    private boolean[] check(IEquipment[] items, int[] assignedSkillpoints) {
        int n = items.length;
        int[] sp = assignedSkillpoints.clone();
        boolean[] activated = new boolean[n];

        // Phase 1: Activate free items (no requirements, all bonuses >= 0).
        // These only increase SP and have no requirements, so activating them
        // first is always optimal — mirrors the JS algorithm's free_bonus pool.
        for (int i = 0; i < n; i++) {
            if (!hasRequirements(items[i]) && !hasNegativeBonuses(items[i])) {
                activated[i] = true;
                applyBonuses(sp, items[i].bonuses());
            }
        }

        int baseCount = 0;
        int baseWeight = 0;
        for (int i = 0; i < n; i++) {
            if (activated[i]) {
                baseCount++;
                baseWeight += bonusWeight(items[i]);
            }
        }

        // Collect remaining items that need ordering consideration
        int k = 0;
        int[] rem = new int[n - baseCount];
        for (int i = 0; i < n; i++) {
            if (!activated[i])
                rem[k++] = i;
        }

        if (k == 0)
            return activated;

        // Phase 2: Backtracking search — mirrors the JS algorithm's _bt() function
        // but adapted for fixed SP instead of minimizing SP.
        // best[0] = count, best[1] = weight
        int[] best = { baseCount, baseWeight };
        boolean[] bestResult = activated.clone();

        bt(items, rem, k, sp, activated, baseCount, baseWeight, best, bestResult);

        return bestResult;
    }

    private static void bt(IEquipment[] items, int[] rem, int k, int[] sp,
            boolean[] activated, int count, int weight,
            int[] best, boolean[] bestResult) {
        // Update best if this state is better
        if (count > best[0] || (count == best[0] && weight > best[1])) {
            best[0] = count;
            best[1] = weight;
            System.arraycopy(activated, 0, bestResult, 0, activated.length);
        }
        // Early exit if all items equipped
        if (best[0] == items.length)
            return;

        // Upper bound pruning: even if all remaining unactivated items equip,
        // can we beat the current best count?
        int remaining = 0;
        for (int j = 0; j < k; j++) {
            if (!activated[rem[j]])
                remaining++;
        }
        if (count + remaining <= best[0])
            return;

        for (int idx = 0; idx < k; idx++) {
            int i = rem[idx];
            if (activated[i])
                continue;
            int[] reqs = items[i].requirements();
            if (!meetsReqs(sp, reqs))
                continue;

            // Save SP state and apply this item's bonuses
            int[] savedSP = sp.clone();
            int[] bonuses = items[i].bonuses();
            applyBonuses(sp, bonuses);

            // Cascade validity check: every previously activated item must
            // still meet its requirements after subtracting its own bonus
            // from the current SP state. This is the exclude-self cascade
            // rule — bootstrapping (an item's own bonus crossing the req
            // threshold) is not allowed.
            boolean valid = true;
            for (int j = 0; j < items.length; j++) {
                if (!activated[j])
                    continue;
                if (!isValidExcludingSelf(sp, items[j].requirements(), items[j].bonuses())) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                activated[i] = true;
                int w = bonusWeight(items[i]);
                bt(items, rem, k, sp, activated, count + 1, weight + w, best, bestResult);
                activated[i] = false;
                if (best[0] == items.length) {
                    System.arraycopy(savedSP, 0, sp, 0, sp.length);
                    return;
                }
            }

            // Restore SP state
            System.arraycopy(savedSP, 0, sp, 0, sp.length);
        }
    }

    private static boolean meetsReqs(int[] sp, int[] reqs) {
        for (int s = 0; s < 5; s++) {
            if (reqs[s] > 0 && sp[s] < reqs[s]) return false;
        }
        return true;
    }

    private static boolean isValidExcludingSelf(int[] sp, int[] reqs, int[] bonuses) {
        for (int s = 0; s < 5; s++) {
            if (reqs[s] > 0 && reqs[s] + bonuses[s] > sp[s]) return false;
        }
        return true;
    }

    private static void applyBonuses(int[] sp, int[] bonuses) {
        for (int s = 0; s < 5; s++) {
            sp[s] += bonuses[s];
        }
    }

    private static boolean hasRequirements(IEquipment item) {
        int[] requirements = item.requirements();
        for (int r : requirements) {
            if (r > 0)
                return true;
        }
        return false;
    }

    private static boolean hasNegativeBonuses(IEquipment item) {
        int[] bonuses = item.bonuses();
        for (int b : bonuses) {
            if (b < 0)
                return true;
        }
        return false;
    }

    private static int bonusWeight(IEquipment item) {
        int sum = 0;
        int[] bonuses = item.bonuses();
        for (int b : bonuses)
            sum += b;
        return sum;
    }
}
