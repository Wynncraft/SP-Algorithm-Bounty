package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

public final class HelixPlayer implements IPlayer {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    final IEquipment[] items;
    final int itemCount;
    final int[] allocated;
    final int[] req0;
    final int[] req1;
    final int[] req2;
    final int[] req3;
    final int[] req4;
    final int[] bonus0;
    final int[] bonus1;
    final int[] bonus2;
    final int[] bonus3;
    final int[] bonus4;
    final int[] itemWeight;
    final int[] bonus = new int[SKILL_POINTS.length];
    private final List<IEquipment> equipmentView;
    int weight;

    private HelixPlayer(
            IEquipment[] items,
            int itemCount,
            int[] allocated,
            int[] req0,
            int[] req1,
            int[] req2,
            int[] req3,
            int[] req4,
            int[] bonus0,
            int[] bonus1,
            int[] bonus2,
            int[] bonus3,
            int[] bonus4,
            int[] itemWeight
    ) {
        this.items = items;
        this.itemCount = itemCount;
        this.allocated = allocated;
        this.req0 = req0;
        this.req1 = req1;
        this.req2 = req2;
        this.req3 = req3;
        this.req4 = req4;
        this.bonus0 = bonus0;
        this.bonus1 = bonus1;
        this.bonus2 = bonus2;
        this.bonus3 = bonus3;
        this.bonus4 = bonus4;
        this.itemWeight = itemWeight;
        this.equipmentView = new EquipmentList(items, itemCount);
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

    public static final class Builder implements IPlayerBuilder<HelixPlayer> {
        private IEquipment[] items = new IEquipment[16];
        private int[] req0 = new int[16];
        private int[] req1 = new int[16];
        private int[] req2 = new int[16];
        private int[] req3 = new int[16];
        private int[] req4 = new int[16];
        private int[] bonus0 = new int[16];
        private int[] bonus1 = new int[16];
        private int[] bonus2 = new int[16];
        private int[] bonus3 = new int[16];
        private int[] bonus4 = new int[16];
        private int[] itemWeight = new int[16];
        private int itemCount;
        private final int[] allocated = new int[SKILL_POINTS.length];

        @Override
        public IPlayerBuilder<HelixPlayer> equipment(IEquipment... equipment) {
            ensureCapacity(itemCount + equipment.length);
            for (IEquipment item : equipment) {
                int[] req = item.requirements();
                int[] bonus = item.bonuses();
                int idx = itemCount++;
                items[idx] = item;
                req0[idx] = req[STR];
                req1[idx] = req[DEX];
                req2[idx] = req[INT];
                req3[idx] = req[DEF];
                req4[idx] = req[AGI];
                bonus0[idx] = bonus[STR];
                bonus1[idx] = bonus[DEX];
                bonus2[idx] = bonus[INT];
                bonus3[idx] = bonus[DEF];
                bonus4[idx] = bonus[AGI];
                itemWeight[idx] = bonus[STR] + bonus[DEX] + bonus[INT] + bonus[DEF] + bonus[AGI];
            }
            return this;
        }

        @Override
        public IPlayerBuilder<HelixPlayer> allocate(SkillPoint point, int amount) {
            allocated[point.ordinal()] = amount;
            return this;
        }

        @Override
        public HelixPlayer build() {
            return new HelixPlayer(
                    Arrays.copyOf(items, itemCount),
                    itemCount,
                    allocated.clone(),
                    Arrays.copyOf(req0, itemCount),
                    Arrays.copyOf(req1, itemCount),
                    Arrays.copyOf(req2, itemCount),
                    Arrays.copyOf(req3, itemCount),
                    Arrays.copyOf(req4, itemCount),
                    Arrays.copyOf(bonus0, itemCount),
                    Arrays.copyOf(bonus1, itemCount),
                    Arrays.copyOf(bonus2, itemCount),
                    Arrays.copyOf(bonus3, itemCount),
                    Arrays.copyOf(bonus4, itemCount),
                    Arrays.copyOf(itemWeight, itemCount));
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
            req0 = Arrays.copyOf(req0, next);
            req1 = Arrays.copyOf(req1, next);
            req2 = Arrays.copyOf(req2, next);
            req3 = Arrays.copyOf(req3, next);
            req4 = Arrays.copyOf(req4, next);
            bonus0 = Arrays.copyOf(bonus0, next);
            bonus1 = Arrays.copyOf(bonus1, next);
            bonus2 = Arrays.copyOf(bonus2, next);
            bonus3 = Arrays.copyOf(bonus3, next);
            bonus4 = Arrays.copyOf(bonus4, next);
            itemWeight = Arrays.copyOf(itemWeight, next);
        }
    }

    private static final class EquipmentList extends AbstractList<IEquipment> {
        private final IEquipment[] items;
        private final int size;

        private EquipmentList(IEquipment[] items, int size) {
            this.items = items;
            this.size = size;
        }

        @Override
        public IEquipment get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(index);
            }
            return items[index];
        }

        @Override
        public int size() {
            return size;
        }
    }
}
