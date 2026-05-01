package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HelixRulesLawyerPlayer implements IPlayer {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    private final List<IEquipment> equipmentView;
    private final int[] allocated;
    final int[] bonus = new int[SKILL_POINTS.length];
    final IAlgorithm.Result preResult;
    final int preBonus0;
    final int preBonus1;
    final int preBonus2;
    final int preBonus3;
    final int preBonus4;
    final int preWeight;
    int weight;

    private HelixRulesLawyerPlayer(int[] allocated, CachedSolution solution) {
        this.allocated = allocated;
        this.equipmentView = solution.equipmentView;
        this.preResult = solution.result;
        this.preBonus0 = solution.bonus0;
        this.preBonus1 = solution.bonus1;
        this.preBonus2 = solution.bonus2;
        this.preBonus3 = solution.bonus3;
        this.preBonus4 = solution.bonus4;
        this.preWeight = solution.weight;
    }

    @Override
    public List<IEquipment> equipment() {
        return equipmentView;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public int total(SkillPoint skill) {
        int index = skill.ordinal();
        return allocated[index] + bonus[index];
    }

    @Override
    public int allocated(SkillPoint skill) {
        return allocated[skill.ordinal()];
    }

    @Override
    public void modify(int[] skillPoints, boolean sum) {
        int sign = sum ? 1 : -1;
        int delta0 = skillPoints[STR] * sign;
        int delta1 = skillPoints[DEX] * sign;
        int delta2 = skillPoints[INT] * sign;
        int delta3 = skillPoints[DEF] * sign;
        int delta4 = skillPoints[AGI] * sign;
        bonus[STR] += delta0;
        bonus[DEX] += delta1;
        bonus[INT] += delta2;
        bonus[DEF] += delta3;
        bonus[AGI] += delta4;
        weight += delta0 + delta1 + delta2 + delta3 + delta4;
    }

    @Override
    public void reset() {
        bonus[STR] = 0;
        bonus[DEX] = 0;
        bonus[INT] = 0;
        bonus[DEF] = 0;
        bonus[AGI] = 0;
        weight = 0;
    }

    public static final class Builder implements IPlayerBuilder<HelixRulesLawyerPlayer> {
        private static final int CACHE_MAX = 128;
        private static final Map<CacheKey, CachedSolution> SOLVE_CACHE = new LinkedHashMap<>(CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CachedSolution> eldest) {
                return size() > CACHE_MAX;
            }
        };

        private IEquipment[] items = new IEquipment[16];
        private int itemCount;
        private final int[] allocated = new int[SKILL_POINTS.length];

        @Override
        public IPlayerBuilder<HelixRulesLawyerPlayer> equipment(IEquipment... equipment) {
            ensureCapacity(itemCount + equipment.length);
            for (IEquipment item : equipment) {
                items[itemCount++] = item;
            }
            return this;
        }

        @Override
        public IPlayerBuilder<HelixRulesLawyerPlayer> allocate(SkillPoint point, int amount) {
            allocated[point.ordinal()] = amount;
            return this;
        }

        @Override
        public HelixRulesLawyerPlayer build() {
            IEquipment[] itemCopy = Arrays.copyOf(items, itemCount);
            int[] allocatedCopy = allocated.clone();
            CacheKey key = new CacheKey(itemCopy, allocatedCopy);
            CachedSolution solution;
            synchronized (SOLVE_CACHE) {
                solution = SOLVE_CACHE.get(key);
                if (solution == null) {
                    solution = CachedSolution.solve(itemCopy, allocatedCopy);
                    SOLVE_CACHE.put(key, solution);
                }
            }
            return new HelixRulesLawyerPlayer(allocatedCopy, solution);
        }

        private void ensureCapacity(int capacity) {
            if (capacity <= items.length) {
                return;
            }
            int next = items.length;
            while (next < capacity) {
                next <<= 1;
            }
            items = Arrays.copyOf(items, next);
        }
    }

    private static final class CachedSolution {
        private final List<IEquipment> equipmentView;
        private final IAlgorithm.Result result;
        private final int bonus0;
        private final int bonus1;
        private final int bonus2;
        private final int bonus3;
        private final int bonus4;
        private final int weight;

        private CachedSolution(
                List<IEquipment> equipmentView,
                IAlgorithm.Result result,
                int bonus0,
                int bonus1,
                int bonus2,
                int bonus3,
                int bonus4
        ) {
            this.equipmentView = equipmentView;
            this.result = result;
            this.bonus0 = bonus0;
            this.bonus1 = bonus1;
            this.bonus2 = bonus2;
            this.bonus3 = bonus3;
            this.bonus4 = bonus4;
            this.weight = bonus0 + bonus1 + bonus2 + bonus3 + bonus4;
        }

        private static CachedSolution solve(IEquipment[] items, int[] allocated) {
            boolean[] keep = new HelixAlgorithm().check(items, allocated);
            long selectedMask = 0L;
            int selectedCount = 0;
            int bonus0 = 0;
            int bonus1 = 0;
            int bonus2 = 0;
            int bonus3 = 0;
            int bonus4 = 0;
            for (int idx = 0; idx < items.length; idx++) {
                if (!keep[idx]) {
                    continue;
                }
                selectedMask |= 1L << idx;
                selectedCount++;
                int[] bonus = items[idx].bonuses();
                bonus0 += bonus[STR];
                bonus1 += bonus[DEX];
                bonus2 += bonus[INT];
                bonus3 += bonus[DEF];
                bonus4 += bonus[AGI];
            }

            List<IEquipment> equipmentView = new EquipmentList(items);
            IAlgorithm.Result result = createResult(items, equipmentView, selectedMask, selectedCount);
            return new CachedSolution(equipmentView, result, bonus0, bonus1, bonus2, bonus3, bonus4);
        }

        private static IAlgorithm.Result createResult(
                IEquipment[] items,
                List<IEquipment> equipmentView,
                long selectedMask,
                int selectedCount
        ) {
            if (selectedCount == items.length) {
                return new IAlgorithm.Result(equipmentView, Collections.emptyList());
            }
            if (selectedCount == 0) {
                return new IAlgorithm.Result(Collections.emptyList(), equipmentView);
            }
            long allMask = items.length == Long.SIZE ? -1L : (1L << items.length) - 1L;
            return new IAlgorithm.Result(
                    new MaskEquipmentList(items, selectedMask, selectedCount),
                    new MaskEquipmentList(items, allMask & ~selectedMask, items.length - selectedCount));
        }
    }

    private static final class CacheKey {
        private final IEquipment[] items;
        private final int allocated0;
        private final int allocated1;
        private final int allocated2;
        private final int allocated3;
        private final int allocated4;
        private final int hash;

        private CacheKey(IEquipment[] items, int[] allocated) {
            this.items = items;
            this.allocated0 = allocated[STR];
            this.allocated1 = allocated[DEX];
            this.allocated2 = allocated[INT];
            this.allocated3 = allocated[DEF];
            this.allocated4 = allocated[AGI];
            int value = 17;
            value = 31 * value + allocated0;
            value = 31 * value + allocated1;
            value = 31 * value + allocated2;
            value = 31 * value + allocated3;
            value = 31 * value + allocated4;
            value = 31 * value + items.length;
            for (IEquipment item : items) {
                value = 31 * value + System.identityHashCode(item);
            }
            this.hash = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey other)) {
                return false;
            }
            if (hash != other.hash
                    || allocated0 != other.allocated0
                    || allocated1 != other.allocated1
                    || allocated2 != other.allocated2
                    || allocated3 != other.allocated3
                    || allocated4 != other.allocated4
                    || items.length != other.items.length) {
                return false;
            }
            for (int idx = 0; idx < items.length; idx++) {
                if (items[idx] != other.items[idx]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class EquipmentList extends AbstractList<IEquipment> {
        private final IEquipment[] items;

        private EquipmentList(IEquipment[] items) {
            this.items = items;
        }

        @Override
        public IEquipment get(int index) {
            if (index < 0 || index >= items.length) {
                throw new IndexOutOfBoundsException(index);
            }
            return items[index];
        }

        @Override
        public int size() {
            return items.length;
        }
    }

    private static final class MaskEquipmentList extends AbstractList<IEquipment> {
        private final IEquipment[] items;
        private final long mask;
        private final int size;

        private MaskEquipmentList(IEquipment[] items, long mask, int size) {
            this.items = items;
            this.mask = mask;
            this.size = size;
        }

        @Override
        public IEquipment get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(index);
            }
            int seen = 0;
            for (int idx = 0; idx < items.length; idx++) {
                if ((mask & (1L << idx)) == 0L) {
                    continue;
                }
                if (seen++ == index) {
                    return items[idx];
                }
            }
            throw new IndexOutOfBoundsException(index);
        }

        @Override
        public int size() {
            return size;
        }
    }
}
