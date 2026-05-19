package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.fred.Utils.IntStack;
import com.wynncraft.core.fred.Utils.Vec5;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.Arrays;
import java.util.List;

/**
 * V2 allocates the item arrays in advance. It makes debugging a bit harder though.
 */
@Information(name = "Fred Algo", version = 2, authors = "Frederik")
public class FredV2Algorithm implements IAlgorithm<WynnPlayer> {
    private static final int N_ITEMS_MAX = 32;

    private final Vec5[] itemReqs;
    private final Vec5[] itemBonuses;
    private final boolean[] negItems;
    private final boolean[] result;
    private final int[] itemIdxs;

    public FredV2Algorithm() {
        itemReqs = new Vec5[N_ITEMS_MAX];
        itemBonuses = new Vec5[N_ITEMS_MAX];
        negItems = new boolean[N_ITEMS_MAX];

        for (int i = 0; i < N_ITEMS_MAX; i++) {
            itemReqs[i] = new Vec5();
            itemBonuses[i] = new Vec5();
        }

        result = new boolean[N_ITEMS_MAX];
        itemIdxs = new int[N_ITEMS_MAX];
    }

    // Get the bonus SP of a specified combo.
    public static void getComboSP(int combo, Vec5 baseSP, Vec5[] itemBonuses, Vec5 out) {
        out.setFrom(baseSP);
        while (combo != 0) {
            int i = Integer.numberOfTrailingZeros(combo);
            out.add(itemBonuses[i]);
            combo &= combo - 1;
        }
    }

    private static boolean isZero(int[] arr) {
        return arr[0] == 0 && arr[1] == 0 && arr[2] == 0 && arr[3] == 0 && arr[4] == 0;
    }

    public Result run(WynnPlayer player) {
        Arrays.fill(result, false);
        Arrays.fill(negItems, false);

        // Preprocess items
        List<IEquipment> items = player.equipment();
        final int N_ITEMS = items.size();
        Vec5 assignedSP = new Vec5(
                player.allocated(SkillPoint.STRENGTH),
                player.allocated(SkillPoint.DEXTERITY),
                player.allocated(SkillPoint.INTELLIGENCE),
                player.allocated(SkillPoint.DEFENCE),
                player.allocated(SkillPoint.AGILITY)
        );
        Vec5 baseSP = new Vec5(assignedSP);
        Vec5 minSP = new Vec5(assignedSP);

        for (int i = 0; i < N_ITEMS; i++) {
            int[] req = items.get(i).requirements();
            int[] bonus = items.get(i).bonuses();

            if (isZero(bonus) && isZero(req)) {
                result[i] = true;   // Filter out tomes and similar items
            } else {
                itemReqs[i].setFrom(req);
                itemBonuses[i].setFrom(bonus);
                if (itemBonuses[i].hasNeg()) {
                    negItems[i] = true;
                    minSP.addNegatives(itemBonuses[i]);
                }
            }
        }

        // Greedy add all items that are guaranteed to be equippable
        boolean added = true;
        while (added) {
            added = false;
            for (int i = 0; i < N_ITEMS; i++) {
                if (result[i] || negItems[i])
                    continue;
                if (Vec5.meetsReqs(minSP, itemReqs[i])) {
                    baseSP.add(itemBonuses[i]);
                    minSP.add(itemBonuses[i]);
                    added = true;
                    result[i] = true;
                }
            }
        }

        // Clean up the item arrays for better efficiency
        int itemCtr = 0;
        for (int i = 0; i < N_ITEMS; i++) {
            if (result[i])
                continue;

            itemIdxs[itemCtr] = i;
            itemReqs[itemCtr].setFrom(itemReqs[i]);
            itemBonuses[itemCtr].setFrom(itemBonuses[i]);
            negItems[itemCtr] = negItems[i];
            itemCtr++;
        }

        /*
         * Each combination of items is represented using an integer as a bitmask, where each bit corresponds to one item.
         * For example for N_UNDETERMINED = 4 the combo number 15 (binary 1111) would be the combo with all four items.
         * This part runs a dfs to find the best valid combination.
         */
        final int N_UNDETERMINED = itemCtr;
        final int N_COMBOS = 1 << N_UNDETERMINED;

        int bestCombo = 0;
        int bestSPTotal = baseSP.total();
        int bestItemCount = 0;

        boolean[] checkedCombos = new boolean[N_COMBOS];
        checkedCombos[0] = true;

        IntStack stack = new IntStack(32);
        stack.push(0);

        Vec5 comboSP = new Vec5();
        Vec5 newComboSP = new Vec5();
        Vec5 newComboSP2 = new Vec5();

        while (!stack.isEmpty()) {
            int combo = stack.pop();
            getComboSP(combo, baseSP, itemBonuses, comboSP);

            if (combo == N_COMBOS - 1)
                break;  // The whole build is valid

            // Add 1 item
            for (int itemIdx = 0; itemIdx < N_UNDETERMINED; itemIdx++) {
                int itemBit = 1 << itemIdx;
                if ((combo & itemBit) != 0)
                    continue;
                int newCombo = combo | itemBit;

                if (checkedCombos[newCombo])
                    continue;   // Already checked combo
                checkedCombos[newCombo] = true;

                if (!Vec5.meetsReqs(comboSP, itemReqs[itemIdx]))
                    continue;

                newComboSP.setFrom(comboSP);
                newComboSP.add(itemBonuses[itemIdx]);

                // Recheck all items if the added item has negative bonuses
                if (negItems[itemIdx]) {
                    boolean valid = true;
                    for (int itemIdx2 = 0; itemIdx2 < N_UNDETERMINED; itemIdx2++) {
                        int itemBit2 = 1 << itemIdx2;
                        if ((combo & itemBit2) == 0)
                            continue;

                        newComboSP2.setFrom(newComboSP);
                        newComboSP2.sub(itemBonuses[itemIdx2]);
                        if (!Vec5.meetsReqs(newComboSP2, itemReqs[itemIdx2])) {
                            valid = false;
                            break;
                        }
                    }
                    if (!valid)
                        continue;
                }

                int spTot = newComboSP.total();
                int itemCount = Integer.bitCount(newCombo);
                if (itemCount > bestItemCount || ((itemCount == bestItemCount && spTot > bestSPTotal))) {
                    bestCombo = newCombo;
                    bestSPTotal = spTot;
                    bestItemCount = itemCount;
                }
                stack.push(newCombo);
            }
        }

        // Process result
        for (int itemIdx = 0; itemIdx < N_UNDETERMINED; itemIdx++) {
            if ((bestCombo & (1 << itemIdx)) != 0) {
                result[itemIdxs[itemIdx]] = true;
                baseSP.add(itemBonuses[itemIdx]);
            }
        }
        baseSP.sub(assignedSP);
        player.modify(baseSP.asArray(), true);

        int totalValid = bestItemCount + (N_ITEMS - N_UNDETERMINED);
        IEquipment[] validItems = new IEquipment[totalValid];
        IEquipment[] invalidItems = new IEquipment[N_ITEMS - totalValid];
        int validCtr = 0;
        int invalidCtr = 0;
        for (int i = 0; i < N_ITEMS; i++) {
            if (result[i])
                validItems[validCtr++] = items.get(i);
            else
                invalidItems[invalidCtr++] = items.get(i);
        }
        return new Result(Arrays.asList(validItems), Arrays.asList(invalidItems));
    }
}

