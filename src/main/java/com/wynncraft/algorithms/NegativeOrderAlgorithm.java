package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Information(name = "Negative Order", version = 1, authors = "RedLogic")
public class NegativeOrderAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    @Override
    public Result run(WynnPlayer player) {
        SearchContext context = new SearchContext(player);
        context.search();
        return context.best.asResult(context.positive, context.negative);
    }

    private static final class SearchContext {

        private final List<IEquipment> positive = new ArrayList<>();
        private final List<IEquipment> negative = new ArrayList<>();

        private final int[] baseTotals = new int[SKILL_POINTS.length];
        private final int[] positiveWeights;
        private final int[] negativeWeights;
        private final Set<StateKey> visited = new HashSet<>();

        private BestResult best;

        private SearchContext(WynnPlayer player) {
            List<IEquipment> equipment = player.equipment();
            for (int i = 0; i < equipment.size(); i++) {
                IEquipment item = equipment.get(i);
                if (item.hasNegativeBonus()) {
                    negative.add(item);
                } else {
                    positive.add(item);
                }
            }

            if (positive.size() >= Long.SIZE || negative.size() >= Long.SIZE) {
                throw new IllegalArgumentException("NegativeOrderAlgorithm supports up to 63 positive and 63 negative items");
            }

            for (int i = 0; i < baseTotals.length; i++) {
                baseTotals[i] = player.allocated(SKILL_POINTS[i]);
            }

            positiveWeights = new int[positive.size()];
            for (int i = 0; i < positive.size(); i++) {
                positiveWeights[i] = weight(positive.get(i).bonuses());
            }

            negativeWeights = new int[negative.size()];
            for (int i = 0; i < negative.size(); i++) {
                negativeWeights[i] = weight(negative.get(i).bonuses());
            }

            best = BestResult.empty();
        }

        private void search() {
            dfs(0L, 0L, baseTotals.clone(), 0);
        }

        private void dfs(long positiveMask, long negativeMask, int[] totals, int weight) {
            Saturation saturation = saturatePositive(positiveMask, totals, weight);
            positiveMask = saturation.positiveMask();
            weight = saturation.weight();

            StateKey key = new StateKey(positiveMask, negativeMask);
            if (!visited.add(key)) {
                return;
            }

            int count = Long.bitCount(positiveMask) + Long.bitCount(negativeMask);
            if (count > best.count() || (count == best.count() && weight > best.weight())) {
                best = new BestResult(positiveMask, negativeMask, count, weight);
            }

            for (int i = 0; i < negative.size(); i++) {
                long bit = 1L << i;
                if ((negativeMask & bit) != 0L) {
                    continue;
                }

                IEquipment item = negative.get(i);
                if (!canEquip(item, totals)) {
                    continue;
                }

                int[] nextTotals = totals.clone();
                modify(nextTotals, item.bonuses(), true);
                dfs(positiveMask, negativeMask | bit, nextTotals, weight + negativeWeights[i]);
            }
        }

        private Saturation saturatePositive(long positiveMask, int[] totals, int weight) {
            while (true) {
                boolean changed = false;
                for (int i = 0; i < positive.size(); i++) {
                    long bit = 1L << i;
                    if ((positiveMask & bit) != 0L) {
                        continue;
                    }

                    IEquipment item = positive.get(i);
                    if (!canEquip(item, totals)) {
                        continue;
                    }

                    positiveMask |= bit;
                    modify(totals, item.bonuses(), true);
                    weight += positiveWeights[i];
                    changed = true;
                }

                if (!changed) {
                    return new Saturation(positiveMask, weight);
                }
            }
        }

        private boolean canEquip(IEquipment item, int[] totals) {
            int[] requirements = item.requirements();
            for (int i = 0; i < requirements.length; i++) {
                if (requirements[i] > 0 && totals[i] < requirements[i]) {
                    return false;
                }
            }
            return true;
        }

        private void modify(int[] totals, int[] skillPoints, boolean sum) {
            for (int i = 0; i < skillPoints.length; i++) {
                totals[i] += sum ? skillPoints[i] : -skillPoints[i];
            }
        }

        private int weight(int[] skillPoints) {
            int total = 0;
            for (int i = 0; i < skillPoints.length; i++) {
                total += skillPoints[i];
            }
            return total;
        }
    }

    private record Saturation(long positiveMask, int weight) {
    }

    private record StateKey(long positiveMask, long negativeMask) {
    }

    private record BestResult(long positiveMask, long negativeMask, int count, int weight) {

        private static BestResult empty() {
            return new BestResult(0L, 0L, 0, Integer.MIN_VALUE);
        }

        private Result asResult(List<IEquipment> positive, List<IEquipment> negative) {
            List<IEquipment> valid = new ArrayList<>();
            List<IEquipment> invalid = new ArrayList<>();

            for (int i = 0; i < positive.size(); i++) {
                if ((positiveMask & (1L << i)) != 0L) {
                    valid.add(positive.get(i));
                } else {
                    invalid.add(positive.get(i));
                }
            }

            for (int i = 0; i < negative.size(); i++) {
                if ((negativeMask & (1L << i)) != 0L) {
                    valid.add(negative.get(i));
                } else {
                    invalid.add(negative.get(i));
                }
            }

            return new Result(valid, invalid);
        }
    }

}
