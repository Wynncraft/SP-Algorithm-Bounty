package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * Compact, append-only player representation for {@link AxiomAlgorithm}.
 *
 * <p>The builder extracts immutable equipment data once. Built players capture
 * the current array references and sizes; later appends never overwrite their
 * prefix, so snapshots remain stable without copying the equipment data.</p>
 */
public final class AxiomPlayer extends AbstractList<IEquipment> implements IPlayer {

    static final int BIAS = 1024;
    static final long BIAS_5 = 0x0400_4004_0040_0400L;

    final Data data;
    final int itemCount;
    final int candidateCount;
    final long freeBonusesPacked;
    final long allBonusesPacked;
    final long allocatedPacked;
    final long basePacked;
    final boolean packedSafe;

    private int bonus0;
    private int bonus1;
    private int bonus2;
    private int bonus3;
    private int bonus4;
    private int weight;

    private AxiomPlayer(
        Data data,
        int itemCount,
        int candidateCount,
        long freeBonusesPacked,
        long allBonusesPacked,
        int allocated0,
        int allocated1,
        int allocated2,
        int allocated3,
        int allocated4,
        boolean packedSafe
    ) {
        this.data = data;
        this.itemCount = itemCount;
        this.candidateCount = candidateCount;
        this.freeBonusesPacked = freeBonusesPacked;
        this.allBonusesPacked = allBonusesPacked;
        this.allocatedPacked = pack5(allocated0, allocated1, allocated2, allocated3, allocated4);
        this.basePacked = allocatedPacked + freeBonusesPacked - BIAS_5;
        this.packedSafe = packedSafe;
    }

    static long pack5(int a, int b, int c, int d, int e) {
        return (long) (a + BIAS)
            | ((long) (b + BIAS) << 12)
            | ((long) (c + BIAS) << 24)
            | ((long) (d + BIAS) << 36)
            | ((long) (e + BIAS) << 48);
    }

    void applyPackedTotal(long packedTotal) {
        bonus0 = lane(packedTotal, 0) - allocatedValue(0);
        bonus1 = lane(packedTotal, 12) - allocatedValue(12);
        bonus2 = lane(packedTotal, 24) - allocatedValue(24);
        bonus3 = lane(packedTotal, 36) - allocatedValue(36);
        bonus4 = lane(packedTotal, 48) - allocatedValue(48);
        weight = bonus0 + bonus1 + bonus2 + bonus3 + bonus4;
    }

    void applyAllBonuses() {
        if (!packedSafe) {
            int b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0;
            for (int i = 0; i < itemCount; i++) {
                int[] bonus = data.items[i].bonuses();
                b0 += bonus[0]; b1 += bonus[1]; b2 += bonus[2]; b3 += bonus[3]; b4 += bonus[4];
            }
            applyBonuses(b0, b1, b2, b3, b4);
            return;
        }
        bonus0 = lane(allBonusesPacked, 0);
        bonus1 = lane(allBonusesPacked, 12);
        bonus2 = lane(allBonusesPacked, 24);
        bonus3 = lane(allBonusesPacked, 36);
        bonus4 = lane(allBonusesPacked, 48);
        weight = bonus0 + bonus1 + bonus2 + bonus3 + bonus4;
    }

    void applyBonuses(int b0, int b1, int b2, int b3, int b4) {
        bonus0 = b0;
        bonus1 = b1;
        bonus2 = b2;
        bonus3 = b3;
        bonus4 = b4;
        weight = b0 + b1 + b2 + b3 + b4;
    }

    int freeBonus(int shift) {
        return lane(freeBonusesPacked, shift);
    }

    int allocatedValue(int shift) {
        return lane(allocatedPacked, shift);
    }

    private static int lane(long packed, int shift) {
        return (int) ((packed >>> shift) & 0xFFFL) - BIAS;
    }

    @Override
    public IEquipment get(int index) {
        if (index < 0 || index >= itemCount) {
            throw new IndexOutOfBoundsException(index);
        }
        return data.items[index];
    }

    @Override
    public int size() {
        return itemCount;
    }

    @Override
    public List<IEquipment> equipment() {
        return this;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public int total(SkillPoint skill) {
        return switch (skill) {
            case STRENGTH -> allocatedValue(0) + bonus0;
            case DEXTERITY -> allocatedValue(12) + bonus1;
            case INTELLIGENCE -> allocatedValue(24) + bonus2;
            case DEFENCE -> allocatedValue(36) + bonus3;
            case AGILITY -> allocatedValue(48) + bonus4;
        };
    }

    @Override
    public int allocated(SkillPoint skill) {
        return switch (skill) {
            case STRENGTH -> allocatedValue(0);
            case DEXTERITY -> allocatedValue(12);
            case INTELLIGENCE -> allocatedValue(24);
            case DEFENCE -> allocatedValue(36);
            case AGILITY -> allocatedValue(48);
        };
    }

    @Override
    public void modify(int[] skillPoints, boolean sum) {
        int sign = sum ? 1 : -1;
        bonus0 += sign * skillPoints[0];
        bonus1 += sign * skillPoints[1];
        bonus2 += sign * skillPoints[2];
        bonus3 += sign * skillPoints[3];
        bonus4 += sign * skillPoints[4];
        weight = bonus0 + bonus1 + bonus2 + bonus3 + bonus4;
    }

    @Override
    public void reset() {
        bonus0 = 0;
        bonus1 = 0;
        bonus2 = 0;
        bonus3 = 0;
        bonus4 = 0;
        weight = 0;
    }

    public static final class Builder implements IPlayerBuilder<AxiomPlayer> {

        private static final int INITIAL_CAPACITY = 24;
        private static final int MIN_PACKED = -BIAS;
        private static final int MAX_PACKED = BIAS - 1;

        private final Data data = new Data(INITIAL_CAPACITY);

        private int itemCount;
        private int candidateCount;

        private int free0;
        private int free1;
        private int free2;
        private int free3;
        private int free4;

        private int total0;
        private int total1;
        private int total2;
        private int total3;
        private int total4;

        private int negative0;
        private int negative1;
        private int negative2;
        private int negative3;
        private int negative4;
        private int positive0;
        private int positive1;
        private int positive2;
        private int positive3;
        private int positive4;

        private int allocated0;
        private int allocated1;
        private int allocated2;
        private int allocated3;
        private int allocated4;
        private boolean valuesPackable = true;

        @Override
        public IPlayerBuilder<AxiomPlayer> equipment(IEquipment... additions) {
            for (IEquipment item : additions) {
                if (itemCount == 63) {
                    throw new IllegalArgumentException("Axiom supports at most 63 equipment items");
                }
                ensureItemCapacity(itemCount + 1);
                data.items[itemCount] = item;

                int[] req = item.requirements();
                int[] bon = item.bonuses();
                int r0 = req[0], r1 = req[1], r2 = req[2], r3 = req[3], r4 = req[4];
                int b0 = bon[0], b1 = bon[1], b2 = bon[2], b3 = bon[3], b4 = bon[4];
                boolean hasRequirements = r0 > 0 || r1 > 0 || r2 > 0 || r3 > 0 || r4 > 0;
                boolean hasNegative = b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0 || b4 < 0;

                if (!hasRequirements && !hasNegative) {
                    data.freeItems |= 1L << itemCount;
                    free0 += b0;
                    free1 += b1;
                    free2 += b2;
                    free3 += b3;
                    free4 += b4;
                } else {
                    ensureCandidateCapacity(candidateCount + 1);
                    data.requirements[candidateCount] = packThreshold(r0, r1, r2, r3, r4);
                    data.bonuses[candidateCount] = pack5(b0, b1, b2, b3, b4);
                    data.needs[candidateCount] = packNeed(r0, r1, r2, r3, r4, b0, b1, b2, b3, b4);
                    int weight = b0 + b1 + b2 + b3 + b4;
                    if (weight < Short.MIN_VALUE || weight > Short.MAX_VALUE) {
                        valuesPackable = false;
                    }
                    data.weights[candidateCount] = (short) weight;
                    if (hasNegative) {
                        data.negativeCandidates |= 1L << candidateCount;
                    }
                    candidateCount++;

                    total0 += b0;
                    total1 += b1;
                    total2 += b2;
                    total3 += b3;
                    total4 += b4;
                    negative0 += Math.min(0, b0);
                    negative1 += Math.min(0, b1);
                    negative2 += Math.min(0, b2);
                    negative3 += Math.min(0, b3);
                    negative4 += Math.min(0, b4);
                    positive0 += Math.max(0, b0);
                    positive1 += Math.max(0, b1);
                    positive2 += Math.max(0, b2);
                    positive3 += Math.max(0, b3);
                    positive4 += Math.max(0, b4);

                    valuesPackable &= inRange(b0) && inRange(b1) && inRange(b2) && inRange(b3) && inRange(b4);
                    valuesPackable &= requirementPackable(r0, b0) && requirementPackable(r1, b1)
                        && requirementPackable(r2, b2) && requirementPackable(r3, b3)
                        && requirementPackable(r4, b4);
                }
                itemCount++;
            }
            return this;
        }

        @Override
        public IPlayerBuilder<AxiomPlayer> allocate(SkillPoint point, int amount) {
            switch (point) {
                case STRENGTH -> allocated0 = amount;
                case DEXTERITY -> allocated1 = amount;
                case INTELLIGENCE -> allocated2 = amount;
                case DEFENCE -> allocated3 = amount;
                case AGILITY -> allocated4 = amount;
            }
            return this;
        }

        @Override
        public AxiomPlayer build() {
            boolean safe = valuesPackable
                && stateRangeSafe(allocated0 + free0, negative0, positive0)
                && stateRangeSafe(allocated1 + free1, negative1, positive1)
                && stateRangeSafe(allocated2 + free2, negative2, positive2)
                && stateRangeSafe(allocated3 + free3, negative3, positive3)
                && stateRangeSafe(allocated4 + free4, negative4, positive4);
            return new AxiomPlayer(
                data,
                itemCount,
                candidateCount,
                pack5(free0, free1, free2, free3, free4),
                pack5(free0 + total0, free1 + total1, free2 + total2, free3 + total3, free4 + total4),
                allocated0,
                allocated1,
                allocated2,
                allocated3,
                allocated4,
                safe
            );
        }

        private void ensureItemCapacity(int required) {
            if (required <= data.items.length) {
                return;
            }
            data.items = Arrays.copyOf(data.items, Math.max(required, data.items.length << 1));
        }

        private void ensureCandidateCapacity(int required) {
            if (required <= data.requirements.length) {
                return;
            }
            int capacity = Math.max(required, data.requirements.length << 1);
            data.requirements = Arrays.copyOf(data.requirements, capacity);
            data.bonuses = Arrays.copyOf(data.bonuses, capacity);
            data.needs = Arrays.copyOf(data.needs, capacity);
            data.weights = Arrays.copyOf(data.weights, capacity);
        }

        private static boolean inRange(int value) {
            return value >= MIN_PACKED && value <= MAX_PACKED;
        }

        private static boolean requirementPackable(int requirement, int bonus) {
            return requirement <= 0
                || (inRange(requirement) && inRange(requirement + bonus));
        }

        private static boolean stateRangeSafe(int base, int negative, int positive) {
            return inRange(base + negative) && inRange(base + positive);
        }

        private static long packThreshold(int r0, int r1, int r2, int r3, int r4) {
            return laneRequirement(r0)
                | (laneRequirement(r1) << 12)
                | (laneRequirement(r2) << 24)
                | (laneRequirement(r3) << 36)
                | (laneRequirement(r4) << 48);
        }

        private static long packNeed(
            int r0, int r1, int r2, int r3, int r4,
            int b0, int b1, int b2, int b3, int b4
        ) {
            return laneNeed(r0, b0)
                | (laneNeed(r1, b1) << 12)
                | (laneNeed(r2, b2) << 24)
                | (laneNeed(r3, b3) << 36)
                | (laneNeed(r4, b4) << 48);
        }

        private static long laneRequirement(int requirement) {
            return requirement > 0 ? requirement + BIAS : 0L;
        }

        private static long laneNeed(int requirement, int bonus) {
            return requirement > 0 ? requirement + bonus + BIAS : 0L;
        }
    }

    static final class Data {
        IEquipment[] items;
        long[] requirements;
        long[] bonuses;
        long[] needs;
        short[] weights;
        long negativeCandidates;
        long freeItems;

        Data(int capacity) {
            items = new IEquipment[capacity];
            requirements = new long[capacity];
            bonuses = new long[capacity];
            needs = new long[capacity];
            weights = new short[capacity];
        }
    }
}
