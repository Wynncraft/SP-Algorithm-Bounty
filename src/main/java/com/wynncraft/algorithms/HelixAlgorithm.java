package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

@Information(name = "Helix", version = 1, authors = {"blousy"})
public final class HelixAlgorithm implements IAlgorithm<HelixPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    private static final int BIAS = 1024;
    private static final long BIAS5 = 0x0400_4004_0040_0400L;
    private static final long GUARD = 0x0800_8008_0080_0800L;

    private static final int MAX_STACK_ITEMS = 64;
    private static final int HOT_MASKS = 1 << 12;

    private IEquipment[] items = new IEquipment[0];
    private int itemCount;

    private final int[] remaining = new int[MAX_STACK_ITEMS];
    private int[] itemWeight = new int[MAX_STACK_ITEMS];
    private int[] req0 = new int[MAX_STACK_ITEMS];
    private int[] req1 = new int[MAX_STACK_ITEMS];
    private int[] req2 = new int[MAX_STACK_ITEMS];
    private int[] req3 = new int[MAX_STACK_ITEMS];
    private int[] req4 = new int[MAX_STACK_ITEMS];
    private int[] bonus0 = new int[MAX_STACK_ITEMS];
    private int[] bonus1 = new int[MAX_STACK_ITEMS];
    private int[] bonus2 = new int[MAX_STACK_ITEMS];
    private int[] bonus3 = new int[MAX_STACK_ITEMS];
    private int[] bonus4 = new int[MAX_STACK_ITEMS];
    private final long[] packedReq = new long[MAX_STACK_ITEMS];
    private final long[] packedNeed = new long[MAX_STACK_ITEMS];
    private final long[] packedBonus = new long[MAX_STACK_ITEMS];

    private final long[] hotSkillNeed = new long[HOT_MASKS * 2];
    private final int[] hotWeight = new int[HOT_MASKS];
    private final long[] hotReach = new long[(HOT_MASKS + 63) >>> 6];

    @Override
    public Result run(HelixPlayer player) {
        prepare(player);
        long selectedMask = checkMask(
                player.allocated[STR],
                player.allocated[DEX],
                player.allocated[INT],
                player.allocated[DEF],
                player.allocated[AGI]);
        applyPlayerBonuses(player, selectedMask);
        return createResult(selectedMask);
    }

    public boolean[] check(IEquipment[] items, int[] assigned) {
        prepare(items, items.length);
        long selectedMask = checkMask(assigned[STR], assigned[DEX], assigned[INT], assigned[DEF], assigned[AGI]);
        boolean[] result = new boolean[items.length];
        for (int idx = 0; idx < items.length; idx++) {
            result[idx] = (selectedMask & (1L << idx)) != 0L;
        }
        return result;
    }

    private void prepare(IEquipment[] sourceItems, int sourceItemCount) {
        if (sourceItemCount >= MAX_STACK_ITEMS) {
            throw new IllegalArgumentException("Helix supports at most " + (MAX_STACK_ITEMS - 1) + " items");
        }
        items = sourceItems;
        itemCount = sourceItemCount;
        for (int idx = 0; idx < sourceItemCount; idx++) {
            int[] req = sourceItems[idx].requirements();
            int[] bonus = sourceItems[idx].bonuses();
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
    }

    private void prepare(HelixPlayer player) {
        if (player.itemCount >= MAX_STACK_ITEMS) {
            throw new IllegalArgumentException("Helix supports at most " + (MAX_STACK_ITEMS - 1) + " items");
        }
        items = player.items;
        itemCount = player.itemCount;
        req0 = player.req0;
        req1 = player.req1;
        req2 = player.req2;
        req3 = player.req3;
        req4 = player.req4;
        bonus0 = player.bonus0;
        bonus1 = player.bonus1;
        bonus2 = player.bonus2;
        bonus3 = player.bonus3;
        bonus4 = player.bonus4;
        itemWeight = player.itemWeight;
    }

    private long checkMask(int skill0, int skill1, int skill2, int skill3, int skill4) {
        long resultMask = 0L;
        int remainingCount = 0;
        long negMask = 0L;
        boolean anyNegative = false;

        for (int idx = 0; idx < itemCount; idx++) {
            if ((req0[idx] | req1[idx] | req2[idx] | req3[idx] | req4[idx]) == 0
                    && (bonus0[idx] | bonus1[idx] | bonus2[idx] | bonus3[idx] | bonus4[idx]) >= 0) {
                resultMask |= 1L << idx;
                skill0 += bonus0[idx];
                skill1 += bonus1[idx];
                skill2 += bonus2[idx];
                skill3 += bonus3[idx];
                skill4 += bonus4[idx];
                continue;
            }

            int remIdx = remainingCount++;
            remaining[remIdx] = idx;
            if ((bonus0[idx] | bonus1[idx] | bonus2[idx] | bonus3[idx] | bonus4[idx]) < 0) {
                negMask |= 1L << remIdx;
                anyNegative = true;
            }
        }

        long remainingMask = switch (remainingCount) {
            case 0 -> 0L;
            case 1 -> canEquipItem(remaining[0], skill0, skill1, skill2, skill3, skill4) ? 1L : 0L;
            case 2 -> solveTwo(skill0, skill1, skill2, skill3, skill4);
            case 3 -> smallExact(remainingCount, skill0, skill1, skill2, skill3, skill4);
            default -> {
                long greedyMask = greedyClose(remainingCount, skill0, skill1, skill2, skill3, skill4, negMask);
                if (Long.bitCount(greedyMask) == remainingCount || !anyNegative) {
                    yield greedyMask;
                }
                yield packedBfs(remainingCount, skill0, skill1, skill2, skill3, skill4, negMask);
            }
        };

        return resultMask | remToItemMask(remainingMask, remainingCount);
    }

    private long solveTwo(int s0, int s1, int s2, int s3, int s4) {
        int idxA = remaining[0];
        int idxB = remaining[1];
        boolean canA = canEquipItem(idxA, s0, s1, s2, s3, s4);
        boolean canB = canEquipItem(idxB, s0, s1, s2, s3, s4);

        if (canA) {
            int as0 = s0 + bonus0[idxA];
            int as1 = s1 + bonus1[idxA];
            int as2 = s2 + bonus2[idxA];
            int as3 = s3 + bonus3[idxA];
            int as4 = s4 + bonus4[idxA];
            if (canEquipItem(idxB, as0, as1, as2, as3, as4)) {
                int fs0 = as0 + bonus0[idxB];
                int fs1 = as1 + bonus1[idxB];
                int fs2 = as2 + bonus2[idxB];
                int fs3 = as3 + bonus3[idxB];
                int fs4 = as4 + bonus4[idxB];
                if (meetsCascadeItem(idxA, fs0, fs1, fs2, fs3, fs4)
                        && meetsCascadeItem(idxB, fs0, fs1, fs2, fs3, fs4)) {
                    return 0b11L;
                }
            }
        }

        if (canB) {
            int bs0 = s0 + bonus0[idxB];
            int bs1 = s1 + bonus1[idxB];
            int bs2 = s2 + bonus2[idxB];
            int bs3 = s3 + bonus3[idxB];
            int bs4 = s4 + bonus4[idxB];
            if (canEquipItem(idxA, bs0, bs1, bs2, bs3, bs4)) {
                int fs0 = bs0 + bonus0[idxA];
                int fs1 = bs1 + bonus1[idxA];
                int fs2 = bs2 + bonus2[idxA];
                int fs3 = bs3 + bonus3[idxA];
                int fs4 = bs4 + bonus4[idxA];
                if (meetsCascadeItem(idxA, fs0, fs1, fs2, fs3, fs4)
                        && meetsCascadeItem(idxB, fs0, fs1, fs2, fs3, fs4)) {
                    return 0b11L;
                }
            }
        }

        if (canA && canB) {
            return itemWeight[idxA] >= itemWeight[idxB] ? 1L : 2L;
        } else if (canA) {
            return 1L;
        } else if (canB) {
            return 2L;
        }
        return 0L;
    }

    private long smallExact(int remainingCount, int s0, int s1, int s2, int s3, int s4) {
        long bestMask = 0L;
        int bestCount = 0;
        int bestWeight = Integer.MIN_VALUE;
        long limit = 1L << remainingCount;

        for (long mask = 1L; mask < limit; mask++) {
            if (!smallMaskValid(mask, s0, s1, s2, s3, s4)) {
                continue;
            }
            int count = Long.bitCount(mask);
            int weight = remMaskWeight(mask);
            if (count > bestCount || (count == bestCount && weight > bestWeight)) {
                bestCount = count;
                bestWeight = weight;
                bestMask = mask;
            }
        }
        return bestMask;
    }

    private boolean smallMaskValid(long mask, int s0, int s1, int s2, int s3, int s4) {
        int a = -1;
        int b = -1;
        int c = -1;
        int len = 0;
        for (long bits = mask; bits != 0L; bits &= bits - 1L) {
            int next = Long.numberOfTrailingZeros(bits);
            if (len == 0) {
                a = next;
            } else if (len == 1) {
                b = next;
            } else {
                c = next;
            }
            len++;
        }

        return switch (len) {
            case 1 -> orderValid(s0, s1, s2, s3, s4, a, -1, -1);
            case 2 -> orderValid(s0, s1, s2, s3, s4, a, b, -1)
                    || orderValid(s0, s1, s2, s3, s4, b, a, -1);
            case 3 -> orderValid(s0, s1, s2, s3, s4, a, b, c)
                    || orderValid(s0, s1, s2, s3, s4, a, c, b)
                    || orderValid(s0, s1, s2, s3, s4, b, a, c)
                    || orderValid(s0, s1, s2, s3, s4, b, c, a)
                    || orderValid(s0, s1, s2, s3, s4, c, a, b)
                    || orderValid(s0, s1, s2, s3, s4, c, b, a);
            default -> true;
        };
    }

    private boolean orderValid(int s0, int s1, int s2, int s3, int s4, int a, int b, int c) {
        long activeMask = 0L;
        int item = a;
        for (int pos = 0; pos < 3 && item >= 0; pos++) {
            int itemIdx = remaining[item];
            if (!canEquipItem(itemIdx, s0, s1, s2, s3, s4)) {
                return false;
            }
            s0 += bonus0[itemIdx];
            s1 += bonus1[itemIdx];
            s2 += bonus2[itemIdx];
            s3 += bonus3[itemIdx];
            s4 += bonus4[itemIdx];
            activeMask |= 1L << item;

            for (long bits = activeMask; bits != 0L; bits &= bits - 1L) {
                int activeIdx = remaining[Long.numberOfTrailingZeros(bits)];
                if (!meetsCascadeItem(activeIdx, s0, s1, s2, s3, s4)) {
                    return false;
                }
            }

            item = pos == 0 ? b : c;
        }
        return true;
    }

    private long greedyClose(int remainingCount, int skill0, int skill1, int skill2, int skill3, int skill4, long negMask) {
        long activeMask = 0L;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int remIdx = 0; remIdx < remainingCount; remIdx++) {
                long bit = 1L << remIdx;
                if ((activeMask & bit) != 0L) {
                    continue;
                }

                int itemIdx = remaining[remIdx];
                if (!canEquipItem(itemIdx, skill0, skill1, skill2, skill3, skill4)) {
                    continue;
                }

                if ((negMask & bit) != 0L) {
                    int next0 = skill0 + bonus0[itemIdx];
                    int next1 = skill1 + bonus1[itemIdx];
                    int next2 = skill2 + bonus2[itemIdx];
                    int next3 = skill3 + bonus3[itemIdx];
                    int next4 = skill4 + bonus4[itemIdx];
                    boolean valid = true;
                    for (long bits = activeMask; bits != 0L; bits &= bits - 1L) {
                        int activeIdx = remaining[Long.numberOfTrailingZeros(bits)];
                        if (!meetsCascadeItem(activeIdx, next0, next1, next2, next3, next4)) {
                            valid = false;
                            break;
                        }
                    }
                    if (!valid) {
                        continue;
                    }
                }

                activeMask |= bit;
                skill0 += bonus0[itemIdx];
                skill1 += bonus1[itemIdx];
                skill2 += bonus2[itemIdx];
                skill3 += bonus3[itemIdx];
                skill4 += bonus4[itemIdx];
                changed = true;
            }
        }

        return activeMask;
    }

    private long packedBfs(int remainingCount, int skill0, int skill1, int skill2, int skill3, int skill4, long negMask) {
        int totalMasks = 1 << remainingCount;
        int fullMask = totalMasks - 1;

        long globalMaxReq = 0L;
        for (int remIdx = 0; remIdx < remainingCount; remIdx++) {
            int itemIdx = remaining[remIdx];
            packedReq[remIdx] = packReq(itemIdx);
            packedNeed[remIdx] = packNeed(itemIdx);
            packedBonus[remIdx] = pack5(bonus0[itemIdx], bonus1[itemIdx], bonus2[itemIdx], bonus3[itemIdx], bonus4[itemIdx]);
            globalMaxReq = max5(globalMaxReq, packedReq[remIdx]);
        }

        long[] skillNeed = totalMasks <= HOT_MASKS ? hotSkillNeed : new long[totalMasks * 2];
        int[] weight = totalMasks <= HOT_MASKS ? hotWeight : new int[totalMasks];
        long[] reach = totalMasks <= HOT_MASKS ? hotReach : new long[(totalMasks + 63) >>> 6];
        int words = (totalMasks + 63) >>> 6;

        for (int w = 0; w < words; w++) {
            reach[w] = 0L;
        }
        skillNeed[0] = pack5(skill0, skill1, skill2, skill3, skill4);
        skillNeed[1] = 0L;
        weight[0] = 0;
        reach[0] = 1L;

        int bestMask = 0;
        int bestCount = 0;
        int bestWeight = 0;

        for (int wordIdx = 0; wordIdx < words; wordIdx++) {
            int baseMask = wordIdx << 6;
            long processed = 0L;

            while (true) {
                long word = reach[wordIdx] & ~processed;
                if (word == 0L) {
                    break;
                }

                int bitIdx = Long.numberOfTrailingZeros(word);
                processed |= 1L << bitIdx;
                int mask = baseMask + bitIdx;
                if (mask >= totalMasks) {
                    break;
                }

                int count = Integer.bitCount(mask);
                int maskWeight = weight[mask];
                if (count > bestCount || (count == bestCount && maskWeight > bestWeight)) {
                    bestCount = count;
                    bestWeight = maskWeight;
                    bestMask = mask;
                }
                if (bestCount == remainingCount) {
                    break;
                }

                long currentSkills = skillNeed[mask << 1];
                long currentNeed = skillNeed[(mask << 1) + 1];
                boolean allRequirementsMet = ge5(currentSkills, globalMaxReq);

                for (int absent = fullMask & ~mask; absent != 0; absent &= absent - 1) {
                    int remIdx = Integer.numberOfTrailingZeros(absent);
                    int nextMask = mask | (1 << remIdx);
                    int nextWord = nextMask >>> 6;
                    long nextBit = 1L << (nextMask & 63);
                    if ((reach[nextWord] & nextBit) != 0L) {
                        continue;
                    }
                    if (!allRequirementsMet && !ge5(currentSkills, packedReq[remIdx])) {
                        continue;
                    }

                    long nextSkills = currentSkills + packedBonus[remIdx] - BIAS5;
                    if ((negMask & (1L << remIdx)) != 0L && !ge5(nextSkills, currentNeed)) {
                        continue;
                    }

                    int nextIdx = nextMask << 1;
                    skillNeed[nextIdx] = nextSkills;
                    skillNeed[nextIdx + 1] = max5(currentNeed, packedNeed[remIdx]);
                    weight[nextMask] = maskWeight + itemWeight[remaining[remIdx]];
                    reach[nextWord] |= nextBit;
                }
            }

            if (bestCount == remainingCount) {
                break;
            }
        }

        return bestMask;
    }

    private void applyPlayerBonuses(HelixPlayer player, long selectedMask) {
        int total0 = 0;
        int total1 = 0;
        int total2 = 0;
        int total3 = 0;
        int total4 = 0;
        for (long bits = selectedMask; bits != 0L; bits &= bits - 1L) {
            int idx = Long.numberOfTrailingZeros(bits);
            total0 += bonus0[idx];
            total1 += bonus1[idx];
            total2 += bonus2[idx];
            total3 += bonus3[idx];
            total4 += bonus4[idx];
        }
        player.bonus[STR] = total0;
        player.bonus[DEX] = total1;
        player.bonus[INT] = total2;
        player.bonus[DEF] = total3;
        player.bonus[AGI] = total4;
        player.weight = total0 + total1 + total2 + total3 + total4;
    }

    private Result createResult(long selectedMask) {
        int selectedCount = Long.bitCount(selectedMask);
        if (selectedCount == itemCount) {
            return new Result(new MaskEquipmentList(items, selectedMask, selectedCount), Collections.emptyList());
        }
        if (selectedCount == 0) {
            return new Result(Collections.emptyList(), new MaskEquipmentList(items, allItemsMask(), itemCount));
        }
        long invalidMask = allItemsMask() & ~selectedMask;
        return new Result(
                new MaskEquipmentList(items, selectedMask, selectedCount),
                new MaskEquipmentList(items, invalidMask, itemCount - selectedCount));
    }

    private long allItemsMask() {
        return itemCount == 64 ? -1L : (1L << itemCount) - 1L;
    }

    private long remToItemMask(long mask, int remainingCount) {
        long out = 0L;
        for (int remIdx = 0; remIdx < remainingCount; remIdx++) {
            if ((mask & (1L << remIdx)) != 0L) {
                out |= 1L << remaining[remIdx];
            }
        }
        return out;
    }

    private int remMaskWeight(long mask) {
        int weight = 0;
        for (long bits = mask; bits != 0L; bits &= bits - 1L) {
            weight += itemWeight[remaining[Long.numberOfTrailingZeros(bits)]];
        }
        return weight;
    }

    private boolean canEquipItem(int idx, int s0, int s1, int s2, int s3, int s4) {
        return (req0[idx] == 0 || req0[idx] <= s0)
                && (req1[idx] == 0 || req1[idx] <= s1)
                && (req2[idx] == 0 || req2[idx] <= s2)
                && (req3[idx] == 0 || req3[idx] <= s3)
                && (req4[idx] == 0 || req4[idx] <= s4);
    }

    private boolean meetsCascadeItem(int idx, int s0, int s1, int s2, int s3, int s4) {
        return (req0[idx] == 0 || req0[idx] + bonus0[idx] <= s0)
                && (req1[idx] == 0 || req1[idx] + bonus1[idx] <= s1)
                && (req2[idx] == 0 || req2[idx] + bonus2[idx] <= s2)
                && (req3[idx] == 0 || req3[idx] + bonus3[idx] <= s3)
                && (req4[idx] == 0 || req4[idx] + bonus4[idx] <= s4);
    }

    private long packReq(int idx) {
        return (long) (req0[idx] != 0 ? req0[idx] + BIAS : 0)
                | ((long) (req1[idx] != 0 ? req1[idx] + BIAS : 0) << 12)
                | ((long) (req2[idx] != 0 ? req2[idx] + BIAS : 0) << 24)
                | ((long) (req3[idx] != 0 ? req3[idx] + BIAS : 0) << 36)
                | ((long) (req4[idx] != 0 ? req4[idx] + BIAS : 0) << 48);
    }

    private long packNeed(int idx) {
        return (long) (req0[idx] != 0 ? req0[idx] + bonus0[idx] + BIAS : 0)
                | ((long) (req1[idx] != 0 ? req1[idx] + bonus1[idx] + BIAS : 0) << 12)
                | ((long) (req2[idx] != 0 ? req2[idx] + bonus2[idx] + BIAS : 0) << 24)
                | ((long) (req3[idx] != 0 ? req3[idx] + bonus3[idx] + BIAS : 0) << 36)
                | ((long) (req4[idx] != 0 ? req4[idx] + bonus4[idx] + BIAS : 0) << 48);
    }

    private static long pack5(int d0, int d1, int d2, int d3, int d4) {
        return (long) (d0 + BIAS)
                | ((long) (d1 + BIAS) << 12)
                | ((long) (d2 + BIAS) << 24)
                | ((long) (d3 + BIAS) << 36)
                | ((long) (d4 + BIAS) << 48);
    }

    private static boolean ge5(long skills, long threshold) {
        return (((skills | GUARD) - threshold) & GUARD) == GUARD;
    }

    private static long max5(long a, long b) {
        long gt = ((a | GUARD) - b) & GUARD;
        long ones = gt >>> 11;
        long mask = gt | (gt - ones);
        return (a & mask) | (b & ~mask);
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
            long remaining = mask;
            for (int i = 0; i < index; i++) {
                remaining &= remaining - 1L;
            }
            return items[Long.numberOfTrailingZeros(remaining)];
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof List<?> other) || other.size() != size) {
                return false;
            }

            long remaining = mask;
            for (Object otherItem : other) {
                IEquipment item = items[Long.numberOfTrailingZeros(remaining)];
                if (item != otherItem && !item.equals(otherItem)) {
                    return false;
                }
                remaining &= remaining - 1L;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 1;
            long remaining = mask;
            while (remaining != 0L) {
                IEquipment item = items[Long.numberOfTrailingZeros(remaining)];
                hash = 31 * hash + item.hashCode();
                remaining &= remaining - 1L;
            }
            return hash;
        }
    }
}
