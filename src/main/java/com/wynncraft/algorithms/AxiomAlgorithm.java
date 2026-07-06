package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Exact subset-reachability solver.
 *
 * <p>A state is the set of equipped candidate items. Its skill totals and
 * cascade thresholds are determined solely by that set, so all equip orders
 * reaching the same mask are equivalent. This turns a permutation search into
 * a subset search. Five independent skill comparisons are executed together
 * in one packed {@code long} on the normal path.</p>
 */
@Information(name = "Axiom", version = 2, authors = {"Dospacite"})
public final class AxiomAlgorithm implements IAlgorithm<AxiomPlayer> {

    private static final int FAST_BITS = 16;
    private static final int FAST_STATES = 1 << FAST_BITS;
    private static final long GUARDS = 0x0800_8008_0080_0800L;
    private static final List<IEquipment> EMPTY = Collections.emptyList();

    private final int[] reached = new int[FAST_STATES];
    private final long[] totals = new long[FAST_STATES];
    private final long[] thresholds = new long[FAST_STATES];
    private final int[] subsetWeights = new int[FAST_STATES];
    private int generation;

    private int winningPackedMask;
    private long winningPackedTotal;

    private IEquipment[] cachedItems;
    private int cachedCandidateCount = -1;
    private long cachedBase;
    private int cachedMask;
    private long cachedTotal;
    private long cachedThreshold;

    private final IEquipment[] scalarItems = new IEquipment[63];
    private final int[][] scalarRequirements = new int[63][];
    private final int[][] scalarBonuses = new int[63][];
    private final int[] scalarWeights = new int[63];
    private final int[] scalarState = new int[5];
    private final int[] scalarThreshold = new int[5];
    private final Set<Long> scalarReached = new HashSet<>();
    private int scalarCount;
    private int scalarBestCount;
    private int scalarBestWeight;
    private long scalarBestMask;
    private long scalarAllMask;

    @Override
    public void clearCache() {
        cachedItems = null;
        cachedCandidateCount = -1;
    }

    @Override
    public Result run(AxiomPlayer player) {
        int itemCount = player.itemCount;
        if (itemCount == 0) {
            player.reset();
            return new Result(EMPTY, EMPTY);
        }

        int candidates = player.candidateCount;
        if (candidates == 0) {
            return allItems(player);
        }
        if (!player.packedSafe || candidates > FAST_BITS) {
            return solveScalar(player);
        }

        if (cachedItems == player.data.items
            && cachedCandidateCount == candidates
            && cachedBase == player.basePacked) {
            if (cachedMask == (1 << candidates) - 1) {
                return allItems(player);
            }
            return selectedItems(player, cachedMask, cachedTotal);
        }

        if (cachedItems == player.data.items
            && cachedCandidateCount + 1 == candidates
            && cachedBase == player.basePacked
            && cachedMask == (1 << cachedCandidateCount) - 1) {
            int item = candidates - 1;
            if (greaterOrEqual(cachedTotal, player.data.requirements[item])) {
                long nextTotal = cachedTotal + player.data.bonuses[item] - AxiomPlayer.BIAS_5;
                if ((player.data.negativeCandidates & (1L << item)) == 0L
                    || greaterOrEqual(nextTotal, cachedThreshold)) {
                    int nextMask = (1 << candidates) - 1;
                    cache(
                        player,
                        nextMask,
                        nextTotal,
                        laneMaximum(cachedThreshold, player.data.needs[item])
                    );
                    return allItems(player);
                }
            }
        }

        int fullMask = (1 << candidates) - 1;
        if (player.data.negativeCandidates == 0L
            && cachedItems == player.data.items
            && cachedCandidateCount <= candidates
            && greaterOrEqual(player.basePacked, cachedBase)) {
            int active = cachedMask;
            int remaining = fullMask & ~active;
            long total = cachedTotal + player.basePacked - cachedBase;
            boolean changed;
            do {
                changed = false;
                for (int bits = remaining; bits != 0; bits &= bits - 1) {
                    int item = Integer.numberOfTrailingZeros(bits);
                    if (!greaterOrEqual(total, player.data.requirements[item])) {
                        continue;
                    }
                    int bit = 1 << item;
                    active |= bit;
                    remaining ^= bit;
                    total += player.data.bonuses[item] - AxiomPlayer.BIAS_5;
                    changed = true;
                }
            } while (changed && remaining != 0);
            cache(player, active, total, 0L);
            if (active == fullMask) {
                return allItems(player);
            }
            return selectedItems(player, active, total);
        }

        int active = 0;
        int remaining = fullMask;
        int activeWeight = 0;
        long total = player.basePacked;
        long threshold = 0L;
        boolean changed;

        do {
            changed = false;
            for (int bits = remaining; bits != 0; bits &= bits - 1) {
                int item = Integer.numberOfTrailingZeros(bits);
                if (!greaterOrEqual(total, player.data.requirements[item])) {
                    continue;
                }
                long nextTotal = total + player.data.bonuses[item] - AxiomPlayer.BIAS_5;
                long bit = 1L << item;
                if ((player.data.negativeCandidates & bit) != 0L
                    && !greaterOrEqual(nextTotal, threshold)) {
                    continue;
                }
                int intBit = 1 << item;
                active |= intBit;
                remaining ^= intBit;
                activeWeight += player.data.weights[item];
                total = nextTotal;
                if (player.data.negativeCandidates != 0L) {
                    threshold = laneMaximum(threshold, player.data.needs[item]);
                }
                changed = true;
            }
        } while (changed && remaining != 0);

        if (active == fullMask) {
            cache(player, active, total, threshold);
            return allItems(player);
        }
        if (player.data.negativeCandidates == 0L) {
            cache(player, active, total, threshold);
            return selectedItems(player, active, total);
        }

        // No candidate transition exists from the strongest state reachable
        // without a negative item. With no candidate active there is no
        // alternative chain that can start, so the unconditional free set is
        // already optimal.
        if (active == 0) {
            cache(player, 0, player.basePacked, 0L);
            return selectedItems(player, 0, player.basePacked);
        }

        solvePacked(player, fullMask, active, activeWeight, total);
        cache(player, winningPackedMask, winningPackedTotal, thresholds[winningPackedMask]);
        if (winningPackedMask == fullMask) {
            return allItems(player);
        }
        return selectedItems(player, winningPackedMask, winningPackedTotal);
    }

    private void solvePacked(
        AxiomPlayer player,
        int fullMask,
        int initialBestMask,
        int initialBestWeight,
        long initialBestTotal
    ) {
        int stamp = ++generation;
        if (stamp == 0) {
            java.util.Arrays.fill(reached, 0);
            stamp = ++generation;
        }

        reached[0] = stamp;
        totals[0] = player.basePacked;
        thresholds[0] = 0L;
        subsetWeights[0] = 0;

        winningPackedMask = initialBestMask;
        winningPackedTotal = initialBestTotal;
        int bestCount = Integer.bitCount(initialBestMask);
        int bestWeight = initialBestWeight;

        for (int mask = 0; mask <= fullMask; mask++) {
            if (reached[mask] != stamp) {
                continue;
            }
            long currentTotal = totals[mask];
            long currentThreshold = thresholds[mask];
            int currentWeight = subsetWeights[mask];
            int absent = fullMask & ~mask;

            for (int bits = absent; bits != 0; bits &= bits - 1) {
                int item = Integer.numberOfTrailingZeros(bits);
                if (!greaterOrEqual(currentTotal, player.data.requirements[item])) {
                    continue;
                }
                long nextTotal = currentTotal + player.data.bonuses[item] - AxiomPlayer.BIAS_5;
                long itemBit = 1L << item;
                if ((player.data.negativeCandidates & itemBit) != 0L
                    && !greaterOrEqual(nextTotal, currentThreshold)) {
                    continue;
                }

                int nextMask = mask | (1 << item);
                if (reached[nextMask] == stamp) {
                    continue;
                }
                int nextWeight = currentWeight + player.data.weights[item];
                reached[nextMask] = stamp;
                totals[nextMask] = nextTotal;
                thresholds[nextMask] = laneMaximum(currentThreshold, player.data.needs[item]);
                subsetWeights[nextMask] = nextWeight;

                int nextCount = Integer.bitCount(nextMask);
                if (nextCount > bestCount || (nextCount == bestCount && nextWeight > bestWeight)) {
                    bestCount = nextCount;
                    bestWeight = nextWeight;
                    winningPackedMask = nextMask;
                    winningPackedTotal = nextTotal;
                    if (nextMask == fullMask) {
                        return;
                    }
                }
            }
        }
    }

    private Result solveScalar(AxiomPlayer player) {
        scalarCount = 0;
        for (int i = 0; i < player.itemCount; i++) {
            if ((player.data.freeItems & (1L << i)) != 0L) {
                continue;
            }
            IEquipment item = player.data.items[i];
            scalarItems[scalarCount] = item;
            scalarRequirements[scalarCount] = item.requirements();
            scalarBonuses[scalarCount] = item.bonuses();
            int[] bonus = scalarBonuses[scalarCount];
            scalarWeights[scalarCount] = bonus[0] + bonus[1] + bonus[2] + bonus[3] + bonus[4];
            scalarCount++;
        }

        scalarState[0] = player.allocatedValue(0) + player.freeBonus(0);
        scalarState[1] = player.allocatedValue(12) + player.freeBonus(12);
        scalarState[2] = player.allocatedValue(24) + player.freeBonus(24);
        scalarState[3] = player.allocatedValue(36) + player.freeBonus(36);
        scalarState[4] = player.allocatedValue(48) + player.freeBonus(48);
        scalarThreshold[0] = Integer.MIN_VALUE;
        scalarThreshold[1] = Integer.MIN_VALUE;
        scalarThreshold[2] = Integer.MIN_VALUE;
        scalarThreshold[3] = Integer.MIN_VALUE;
        scalarThreshold[4] = Integer.MIN_VALUE;
        scalarBestCount = 0;
        scalarBestWeight = 0;
        scalarBestMask = 0L;
        scalarAllMask = scalarCount == 63 ? Long.MAX_VALUE : (1L << scalarCount) - 1L;
        scalarReached.clear();
        scalarReached.add(0L);
        searchScalar(0L, 0, 0);

        if (scalarBestMask == scalarAllMask) {
            return allItems(player);
        }

        int b0 = player.freeBonus(0);
        int b1 = player.freeBonus(12);
        int b2 = player.freeBonus(24);
        int b3 = player.freeBonus(36);
        int b4 = player.freeBonus(48);
        for (long bits = scalarBestMask; bits != 0L; bits &= bits - 1L) {
            int item = Long.numberOfTrailingZeros(bits);
            int[] bonus = scalarBonuses[item];
            b0 += bonus[0];
            b1 += bonus[1];
            b2 += bonus[2];
            b3 += bonus[3];
            b4 += bonus[4];
        }
        player.applyBonuses(b0, b1, b2, b3, b4);
        return selectedItems(player, scalarBestMask);
    }

    private void searchScalar(long mask, int count, int weight) {
        if (count > scalarBestCount || (count == scalarBestCount && weight > scalarBestWeight)) {
            scalarBestCount = count;
            scalarBestWeight = weight;
            scalarBestMask = mask;
            if (mask == scalarAllMask) {
                return;
            }
        }

        long absent = scalarAllMask & ~mask;
        for (long bits = absent; bits != 0L; bits &= bits - 1L) {
            int item = Long.numberOfTrailingZeros(bits);
            int[] requirement = scalarRequirements[item];
            if ((requirement[0] > 0 && scalarState[0] < requirement[0])
                || (requirement[1] > 0 && scalarState[1] < requirement[1])
                || (requirement[2] > 0 && scalarState[2] < requirement[2])
                || (requirement[3] > 0 && scalarState[3] < requirement[3])
                || (requirement[4] > 0 && scalarState[4] < requirement[4])) {
                continue;
            }

            int[] bonus = scalarBonuses[item];
            int old0 = scalarState[0], old1 = scalarState[1], old2 = scalarState[2];
            int old3 = scalarState[3], old4 = scalarState[4];
            scalarState[0] += bonus[0];
            scalarState[1] += bonus[1];
            scalarState[2] += bonus[2];
            scalarState[3] += bonus[3];
            scalarState[4] += bonus[4];

            boolean negative = bonus[0] < 0 || bonus[1] < 0 || bonus[2] < 0 || bonus[3] < 0 || bonus[4] < 0;
            if (negative && (scalarState[0] < scalarThreshold[0]
                || scalarState[1] < scalarThreshold[1]
                || scalarState[2] < scalarThreshold[2]
                || scalarState[3] < scalarThreshold[3]
                || scalarState[4] < scalarThreshold[4])) {
                scalarState[0] = old0; scalarState[1] = old1; scalarState[2] = old2;
                scalarState[3] = old3; scalarState[4] = old4;
                continue;
            }

            int need0 = scalarThreshold[0], need1 = scalarThreshold[1], need2 = scalarThreshold[2];
            int need3 = scalarThreshold[3], need4 = scalarThreshold[4];
            if (requirement[0] > 0) scalarThreshold[0] = Math.max(need0, requirement[0] + bonus[0]);
            if (requirement[1] > 0) scalarThreshold[1] = Math.max(need1, requirement[1] + bonus[1]);
            if (requirement[2] > 0) scalarThreshold[2] = Math.max(need2, requirement[2] + bonus[2]);
            if (requirement[3] > 0) scalarThreshold[3] = Math.max(need3, requirement[3] + bonus[3]);
            if (requirement[4] > 0) scalarThreshold[4] = Math.max(need4, requirement[4] + bonus[4]);

            long nextMask = mask | (1L << item);
            if (scalarReached.add(nextMask)) {
                searchScalar(nextMask, count + 1, weight + scalarWeights[item]);
            }

            scalarState[0] = old0; scalarState[1] = old1; scalarState[2] = old2;
            scalarState[3] = old3; scalarState[4] = old4;
            scalarThreshold[0] = need0; scalarThreshold[1] = need1; scalarThreshold[2] = need2;
            scalarThreshold[3] = need3; scalarThreshold[4] = need4;
        }
    }

    private Result allItems(AxiomPlayer player) {
        player.applyAllBonuses();
        return new Result(player, EMPTY);
    }

    private void cache(AxiomPlayer player, int mask, long total, long threshold) {
        cachedItems = player.data.items;
        cachedCandidateCount = player.candidateCount;
        cachedBase = player.basePacked;
        cachedMask = mask;
        cachedTotal = total;
        cachedThreshold = threshold;
    }

    private Result selectedItems(AxiomPlayer player, int candidateMask, long packedTotal) {
        player.applyPackedTotal(packedTotal);
        return selectedItems(player, Integer.toUnsignedLong(candidateMask));
    }

    private Result selectedItems(AxiomPlayer player, long candidateMask) {
        long selected = player.data.freeItems;
        int candidate = 0;
        for (int i = 0; i < player.itemCount; i++) {
            long itemBit = 1L << i;
            if ((player.data.freeItems & itemBit) != 0L) {
                continue;
            }
            if ((candidateMask & (1L << candidate)) != 0L) {
                selected |= itemBit;
            }
            candidate++;
        }

        int selectedCount = Long.bitCount(selected);
        if (selectedCount == player.itemCount) {
            return new Result(player, EMPTY);
        }
        if (selectedCount == 0) {
            return new Result(EMPTY, player);
        }
        long all = player.itemCount == 63 ? Long.MAX_VALUE : (1L << player.itemCount) - 1L;
        return new Result(
            new MaskList(player.data.items, selected, selectedCount),
            new MaskList(player.data.items, all & ~selected, player.itemCount - selectedCount)
        );
    }

    private static boolean greaterOrEqual(long values, long limits) {
        return (((values | GUARDS) - limits) & GUARDS) == GUARDS;
    }

    private static long laneMaximum(long left, long right) {
        long greater = ((left | GUARDS) - right) & GUARDS;
        long lowBits = greater >>> 11;
        long selection = greater | (greater - lowBits);
        return (left & selection) | (right & ~selection);
    }

    private static final class MaskList extends AbstractList<IEquipment> {
        private final IEquipment[] items;
        private final long mask;
        private final int size;

        private MaskList(IEquipment[] items, long mask, int size) {
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
    }
}
