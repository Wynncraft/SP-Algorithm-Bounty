package com.wynncraft.algorithms;

import com.wynncraft.core.NegativeMaskCache;
import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;
import speiger.src.collections.ints.lists.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

@Information(name = "Wynn Dedupe Branch", version = 1, authors = "azael")
public class WynnDeduplicatedBranchAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final NegativeMaskCache MASK_CACHE = new NegativeMaskCache();
    private static final WynnFrumaAlgorithm FALLBACK = new WynnFrumaAlgorithm();

    private static final class Group {
        final IEquipment template;
        final List<IEquipment> items = new ArrayList<>();
        final int[] scaledBonus;
        int scaledWeight;

        Group(IEquipment item) {
            this.template = item;
            this.scaledBonus = item.bonuses().clone();
            this.scaledWeight = weight(item.bonuses());
            this.items.add(item);
        }

        void add(IEquipment item) {
            items.add(item);

            int[] bonus = item.bonuses();
            for (int i = 0; i < bonus.length; i++) {
                scaledBonus[i] += bonus[i];
            }
            scaledWeight += weight(bonus);
        }

        int count() {
            return items.size();
        }
    }

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> positives = new ArrayList<>();
        List<IEquipment> negatives = new ArrayList<>();
        List<IEquipment> equipment = player.equipment();
        for (int i = 0; i < equipment.size(); i++) {
            IEquipment item = equipment.get(i);
            if (item.hasNegativeBonus()) {
                negatives.add(item);
                continue;
            }
            positives.add(item);
        }

        if (negatives.isEmpty()) {
            return runPositiveOnly(player, positives);
        }

        if (negatives.size() >= 18) {
            return FALLBACK.run(player);
        }

        List<Group> positiveGroups = deduplicate(positives);
        int[] allocated = allocated(player);
        int positiveItemCount = countItems(positiveGroups);
        int negativeCount = negatives.size();
        int[] baseTotals = allocated.clone();
        boolean[] basePositiveActive = new boolean[positiveGroups.size()];
        int baseCount = 0;
        int baseWeight = 0;

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < positiveGroups.size(); i++) {
                if (basePositiveActive[i]) {
                    continue;
                }

                Group group = positiveGroups.get(i);
                if (!canEquip(group.template.requirements(), baseTotals)) {
                    continue;
                }

                basePositiveActive[i] = true;
                baseCount += group.count();
                baseWeight += group.scaledWeight;
                modify(baseTotals, group.scaledBonus, true);
                changed = true;
            }
        }

        int bestCount = -1;
        int bestWeight = Integer.MIN_VALUE;
        int bestMask = 0;
        boolean[] bestPositiveActive = new boolean[positiveGroups.size()];
        boolean[] bestNegativeActive = new boolean[0];

        IntArrayList masks = MASK_CACHE.get(negativeCount);
        for (int mask : masks) {
            int selectedNegativeCount = Integer.bitCount(mask);
            int maxPossibleCount = positiveItemCount + selectedNegativeCount;
            if (bestCount > maxPossibleCount) {
                break;
            }

            int[] totals = baseTotals.clone();
            boolean[] positiveActive = Arrays.copyOf(basePositiveActive, basePositiveActive.length);
            boolean[] negativeActive = new boolean[negativeCount];
            int count = baseCount;
            int weight = baseWeight;

            changed = true;
            while (changed) {
                changed = false;

                for (int i = 0; i < negativeCount; i++) {
                    if ((mask & (1 << i)) == 0 || negativeActive[i]) {
                        continue;
                    }

                    IEquipment item = negatives.get(i);
                    if (!canEquip(item.requirements(), totals)) {
                        continue;
                    }

                    negativeActive[i] = true;
                    count++;
                    weight += weight(item.bonuses());
                    modify(totals, item.bonuses(), true);
                    changed = true;
                }

                for (int i = 0; i < positiveGroups.size(); i++) {
                    if (positiveActive[i]) {
                        continue;
                    }

                    Group group = positiveGroups.get(i);
                    if (!canEquip(group.template.requirements(), totals)) {
                        continue;
                    }

                    positiveActive[i] = true;
                    count += group.count();
                    weight += group.scaledWeight;
                    modify(totals, group.scaledBonus, true);
                    changed = true;
                }
            }

            if (!allSelectedNegativesActive(mask, negativeActive)) {
                continue;
            }

            if (count > bestCount || (count == bestCount && weight > bestWeight)) {
                bestCount = count;
                bestWeight = weight;
                bestMask = mask;
                bestPositiveActive = Arrays.copyOf(positiveActive, positiveActive.length);
                bestNegativeActive = Arrays.copyOf(negativeActive, negativeActive.length);
            }
        }

        return buildResult(player, positiveGroups, negatives, bestMask, bestPositiveActive, bestNegativeActive);
    }

    private Result runPositiveOnly(WynnPlayer player, List<IEquipment> positives) {
        int[] totals = allocated(player);
        int[] finalBonus = new int[SKILL_POINTS.length];
        boolean[] active = new boolean[positives.size()];
        List<IEquipment> valid = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();

        for (int i = 0; i < positives.size(); i++) {
            IEquipment item = positives.get(i);
            if (!isTrivial(item)) {
                continue;
            }

            active[i] = true;
            valid.add(item);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < positives.size(); i++) {
                if (active[i]) {
                    continue;
                }

                IEquipment item = positives.get(i);
                if (!canEquip(item.requirements(), totals)) {
                    continue;
                }

                active[i] = true;
                valid.add(item);
                modify(totals, item.bonuses(), true);
                modify(finalBonus, item.bonuses(), true);
                changed = true;
            }
        }

        for (int i = 0; i < positives.size(); i++) {
            if (active[i]) {
                continue;
            }

            invalid.add(positives.get(i));
        }

        player.reset();
        player.modify(finalBonus, true);
        return new Result(valid, invalid);
    }

    private Result buildResult(
        WynnPlayer player,
        List<Group> positiveGroups,
        List<IEquipment> negatives,
        int bestMask,
        boolean[] bestPositiveActive,
        boolean[] bestNegativeActive
    ) {
        player.reset();

        List<IEquipment> valid = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();

        for (int i = 0; i < positiveGroups.size(); i++) {
            Group group = positiveGroups.get(i);
            if (!bestPositiveActive[i]) {
                invalid.addAll(group.items);
                continue;
            }

            valid.addAll(group.items);
            player.modify(group.scaledBonus, true);
        }

        for (int i = 0; i < negatives.size(); i++) {
            if ((bestMask & (1 << i)) == 0 || !bestNegativeActive[i]) {
                invalid.add(negatives.get(i));
                continue;
            }

            valid.add(negatives.get(i));
            player.modify(negatives.get(i).bonuses(), true);
        }

        return new Result(valid, invalid);
    }

    private List<Group> deduplicate(List<IEquipment> positives) {
        IdentityHashMap<IEquipment, Group> groups = new IdentityHashMap<>();
        List<Group> ordered = new ArrayList<>();
        for (int i = 0; i < positives.size(); i++) {
            IEquipment item = positives.get(i);
            Group group = groups.get(item);
            if (group == null) {
                group = new Group(item);
                groups.put(item, group);
                ordered.add(group);
                continue;
            }

            group.add(item);
        }
        return ordered;
    }

    private int[] allocated(WynnPlayer player) {
        int[] result = new int[SKILL_POINTS.length];
        for (int i = 0; i < SKILL_POINTS.length; i++) {
            result[i] = player.allocated(SKILL_POINTS[i]);
        }
        return result;
    }

    private boolean isTrivial(IEquipment item) {
        int[] requirements = item.requirements();
        for (int i = 0; i < requirements.length; i++) {
            if (requirements[i] > 0) {
                return false;
            }
        }

        int[] bonuses = item.bonuses();
        for (int i = 0; i < bonuses.length; i++) {
            if (bonuses[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canEquip(int[] requirements, int[] totals) {
        for (int i = 0; i < requirements.length; i++) {
            int requirement = requirements[i];
            if (requirement > 0 && requirement > totals[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean allSelectedNegativesActive(int mask, boolean[] active) {
        for (int i = 0; i < active.length; i++) {
            if ((mask & (1 << i)) != 0 && !active[i]) {
                return false;
            }
        }
        return true;
    }

    private int countItems(List<Group> groups) {
        int sum = 0;
        for (int i = 0; i < groups.size(); i++) {
            sum += groups.get(i).count();
        }
        return sum;
    }

    private void modify(int[] target, int[] delta, boolean sum) {
        for (int i = 0; i < delta.length; i++) {
            target[i] += sum ? delta[i] : -delta[i];
        }
    }

    private static int weight(int[] bonus) {
        int total = 0;
        for (int i = 0; i < bonus.length; i++) {
            total += bonus[i];
        }
        return total;
    }
}
