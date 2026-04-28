package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Information(name = "Negative Order", version = 2, authors = "RedLogic")
public class NegativeOrderAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final int N_SKILLS = SkillPoint.values().length;
    private static final int VISITED_BITS = 22;
    private static final int VISITED_WORDS = (1 << VISITED_BITS) / 64;

    private final int[] state = new int[N_SKILLS];
    private final long[] visited = new long[VISITED_WORDS];

    private int[][] requirements = new int[0][];
    private int[][] bonuses = new int[0][];
    private int[] weights = new int[0];
    private boolean[] hasRequirements = new boolean[0];
    private boolean[] hasNegativeBonus = new boolean[0];
    private IEquipment[] items = new IEquipment[0];

    private int count;
    private boolean useVisited;
    private int bestCount;
    private int bestWeight;
    private long bestMask;

    @Override
    public Result run(WynnPlayer player) {
        prepare(player);

        long activeMask = activateFreeItems();
        int activeCount = Long.bitCount(activeMask);
        int activeWeight = currentWeight(activeMask);
        bestCount = activeCount;
        bestWeight = activeWeight;
        bestMask = activeMask;

        long allMask = count == 64 ? -1L : (1L << count) - 1L;
        long remainingMask = allMask & ~activeMask;

        useVisited = count <= VISITED_BITS;
        if (useVisited) {
            int wordsToClear = Math.min(((1 << count) / 64) + 1, VISITED_WORDS);
            Arrays.fill(visited, 0, wordsToClear, 0L);
        }

        search(activeMask, remainingMask, activeCount, activeWeight);

        List<IEquipment> valid = new ArrayList<>(Long.bitCount(bestMask));
        List<IEquipment> invalid = new ArrayList<>(count - Long.bitCount(bestMask));
        for (int i = 0; i < count; i++) {
            if ((bestMask & (1L << i)) != 0L) {
                valid.add(items[i]);
            } else {
                invalid.add(items[i]);
            }
        }

        player.reset();
        for (int i = 0; i < valid.size(); i++) {
            player.modify(valid.get(i).bonuses(), true);
        }

        return new Result(valid, invalid);
    }

    private void prepare(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        count = equipment.size();

        if (items.length < count) {
            int capacity = Math.max(count, 32);
            requirements = new int[capacity][];
            bonuses = new int[capacity][];
            weights = new int[capacity];
            hasRequirements = new boolean[capacity];
            hasNegativeBonus = new boolean[capacity];
            items = new IEquipment[capacity];
        }

        for (int i = 0; i < count; i++) {
            IEquipment item = equipment.get(i);
            items[i] = item;
            requirements[i] = item.requirements();
            bonuses[i] = item.bonuses();

            int weight = 0;
            boolean hasReq = false;
            boolean hasNeg = false;
            for (int s = 0; s < N_SKILLS; s++) {
                if (requirements[i][s] > 0) {
                    hasReq = true;
                }
                if (bonuses[i][s] < 0) {
                    hasNeg = true;
                }
                weight += bonuses[i][s];
            }
            weights[i] = weight;
            hasRequirements[i] = hasReq;
            hasNegativeBonus[i] = hasNeg;
        }

        SkillPoint[] points = SkillPoint.values();
        for (int s = 0; s < N_SKILLS; s++) {
            state[s] = player.allocated(points[s]);
        }
    }

    private long activateFreeItems() {
        long mask = 0L;
        for (int i = 0; i < count; i++) {
            if (hasRequirements[i] || hasNegativeBonus[i]) {
                continue;
            }

            applyBonus(i, 1);
            mask |= 1L << i;
        }
        return mask;
    }

    private int currentWeight(long mask) {
        int total = 0;
        long iter = mask;
        while (iter != 0L) {
            long bit = iter & -iter;
            iter ^= bit;
            total += weights[Long.numberOfTrailingZeros(bit)];
        }
        return total;
    }

    private void search(long activeMask, long remainingMask, int activeCount, int activeWeight) {
        if (activeCount > bestCount || (activeCount == bestCount && activeWeight > bestWeight)) {
            bestCount = activeCount;
            bestWeight = activeWeight;
            bestMask = activeMask;
        }

        if (activeCount + Long.bitCount(remainingMask) <= bestCount) {
            return;
        }

        long iter = remainingMask;
        while (iter != 0L) {
            long bit = iter & -iter;
            iter ^= bit;
            int index = Long.numberOfTrailingZeros(bit);

            if (!canEquip(index)) {
                continue;
            }

            long nextMask = activeMask | bit;
            if (useVisited) {
                int visitedIndex = (int) nextMask;
                int word = visitedIndex >>> 6;
                long wordBit = 1L << (visitedIndex & 63);
                if ((visited[word] & wordBit) != 0L) {
                    continue;
                }
                visited[word] |= wordBit;
            }

            applyBonus(index, 1);
            boolean cascadeOk = !hasNegativeBonus[index] || cascadeValid(activeMask);
            if (cascadeOk) {
                search(nextMask, remainingMask & ~bit, activeCount + 1, activeWeight + weights[index]);
            }
            applyBonus(index, -1);
        }
    }

    private boolean cascadeValid(long activeMask) {
        long iter = activeMask;
        while (iter != 0L) {
            long bit = iter & -iter;
            iter ^= bit;
            int index = Long.numberOfTrailingZeros(bit);
            if (!hasRequirements[index]) {
                continue;
            }

            if (!isValid(index)) {
                return false;
            }
        }
        return true;
    }

    private boolean canEquip(int index) {
        int[] requirement = requirements[index];
        if (requirement[0] > 0 && state[0] < requirement[0]) return false;
        if (requirement[1] > 0 && state[1] < requirement[1]) return false;
        if (requirement[2] > 0 && state[2] < requirement[2]) return false;
        if (requirement[3] > 0 && state[3] < requirement[3]) return false;
        if (requirement[4] > 0 && state[4] < requirement[4]) return false;
        return true;
    }

    private boolean isValid(int index) {
        int[] requirement = requirements[index];
        int[] bonus = bonuses[index];
        if (requirement[0] > 0 && state[0] < requirement[0] + bonus[0]) return false;
        if (requirement[1] > 0 && state[1] < requirement[1] + bonus[1]) return false;
        if (requirement[2] > 0 && state[2] < requirement[2] + bonus[2]) return false;
        if (requirement[3] > 0 && state[3] < requirement[3] + bonus[3]) return false;
        if (requirement[4] > 0 && state[4] < requirement[4] + bonus[4]) return false;
        return true;
    }

    private void applyBonus(int index, int sign) {
        int[] bonus = bonuses[index];
        state[0] += sign * bonus[0];
        state[1] += sign * bonus[1];
        state[2] += sign * bonus[2];
        state[3] += sign * bonus[3];
        state[4] += sign * bonus[4];
    }
}
