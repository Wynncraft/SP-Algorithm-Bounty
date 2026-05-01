package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Curious Algorithm.
 *
 * Faster with more caching and fewer repeated item scans.
 *
 * @author Melon Team (riege and trethore)
 * @version 2
 */
@SuppressWarnings("DuplicatedCode")
@Information(name = "Curious Algorythm", version = 1, authors = "Melon")
public final class CuriousAlgorithm implements IAlgorithm<CuriousPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;
    private static final int SKILLS = AGI + 1;
    private static final int EMPTY_CAPACITY = 0;

    /*
     * The visited table is great while the mask is small.
     * After that it gets fat, so we stop using it before memory starts being silly.
     */
    private static final int VISITED_LIMIT = 22;
    private static final int VISITED_WORDS = (1 << VISITED_LIMIT) >>> 6;

    private final int[] state = new int[SKILLS];
    private final int[] bestBonuses = new int[SKILLS];
    private final long[] requiredBySkill = new long[SKILLS];
    private final long[] visited = new long[VISITED_WORDS];

    /*
     * Flatten every item into boring arrays.
     * Objects are nicer to read, but arrays win when the search calls this stuff nonstop.
     */
    private IEquipment[] items = new IEquipment[EMPTY_CAPACITY];
    private int[] weights = new int[EMPTY_CAPACITY];
    private int[] req0 = new int[EMPTY_CAPACITY];
    private int[] req1 = new int[EMPTY_CAPACITY];
    private int[] req2 = new int[EMPTY_CAPACITY];
    private int[] req3 = new int[EMPTY_CAPACITY];
    private int[] req4 = new int[EMPTY_CAPACITY];
    private int[] bonus0 = new int[EMPTY_CAPACITY];
    private int[] bonus1 = new int[EMPTY_CAPACITY];
    private int[] bonus2 = new int[EMPTY_CAPACITY];
    private int[] bonus3 = new int[EMPTY_CAPACITY];
    private int[] bonus4 = new int[EMPTY_CAPACITY];
    private int[] adjReq0 = new int[EMPTY_CAPACITY];
    private int[] adjReq1 = new int[EMPTY_CAPACITY];
    private int[] adjReq2 = new int[EMPTY_CAPACITY];
    private int[] adjReq3 = new int[EMPTY_CAPACITY];
    private int[] adjReq4 = new int[EMPTY_CAPACITY];
    private boolean[] hasRequirement = new boolean[EMPTY_CAPACITY];
    private boolean[] hasNegativeBonus = new boolean[EMPTY_CAPACITY];
    /*
     * If a negative item touches STR, only STR-requiring items can become invalid.
     * impactMasks stores those shortcuts so we do not recheck the whole build every time.
     */
    private long[] impactMasks = new long[EMPTY_CAPACITY];

    private int base0;
    private int base1;
    private int base2;
    private int base3;
    private int base4;
    private int itemCount;
    private boolean useVisited;
    private long negativeMask;
    private long bestMask;
    private int bestCount;
    private int bestWeight;

    /*
     * Free positive items are always good, so we keep one aggregate chunk for them.
     * One mask + five sums means activating them is basically O(1).
     */
    private long freeMask;
    private int freeBonus0;
    private int freeBonus1;
    private int freeBonus2;
    private int freeBonus3;
    private int freeBonus4;
    private int freeWeight;
    private int totalWeight;
    private boolean impactMasksDirty = true;

    /*
     * Prepared data cache.
     * The bounty runner calls the same player shape a lot, so Curious remembers the expensive
     * item preprocessing and only rebuilds it when the equipment list really changed.
     */
    private CuriousPlayer preparedPlayer;
    private CuriousPlayer activePreparedPlayer;
    private IEquipment[] preparedItems = new IEquipment[EMPTY_CAPACITY];
    private int[] preparedWeights = new int[EMPTY_CAPACITY];
    private int[] preparedReq0 = new int[EMPTY_CAPACITY];
    private int[] preparedReq1 = new int[EMPTY_CAPACITY];
    private int[] preparedReq2 = new int[EMPTY_CAPACITY];
    private int[] preparedReq3 = new int[EMPTY_CAPACITY];
    private int[] preparedReq4 = new int[EMPTY_CAPACITY];
    private int[] preparedBonus0 = new int[EMPTY_CAPACITY];
    private int[] preparedBonus1 = new int[EMPTY_CAPACITY];
    private int[] preparedBonus2 = new int[EMPTY_CAPACITY];
    private int[] preparedBonus3 = new int[EMPTY_CAPACITY];
    private int[] preparedBonus4 = new int[EMPTY_CAPACITY];
    private int[] preparedAdjReq0 = new int[EMPTY_CAPACITY];
    private int[] preparedAdjReq1 = new int[EMPTY_CAPACITY];
    private int[] preparedAdjReq2 = new int[EMPTY_CAPACITY];
    private int[] preparedAdjReq3 = new int[EMPTY_CAPACITY];
    private int[] preparedAdjReq4 = new int[EMPTY_CAPACITY];
    private boolean[] preparedHasRequirement = new boolean[EMPTY_CAPACITY];
    private boolean[] preparedHasNegativeBonus = new boolean[EMPTY_CAPACITY];
    private long[] preparedImpactMasks = new long[EMPTY_CAPACITY];
    private long preparedNegativeMask;
    private long preparedFreeMask;
    private int preparedFreeBonus0;
    private int preparedFreeBonus1;
    private int preparedFreeBonus2;
    private int preparedFreeBonus3;
    private int preparedFreeBonus4;
    private int preparedFreeWeight;
    private int preparedTotalWeight;

    // Prefix-reuse state: if the list only grew at the end, keep the old work and append the new items.
    private CuriousPlayer lastPlayer;
    private List<IEquipment> lastEquipment;
    private int lastItemCount;
    private boolean lastEquipmentReusable;

    @Override
    public Result run(CuriousPlayer player) {
        List<IEquipment> equipment = player.equipment;
        prepareLight(player, equipment);

        long allMask = allItemsMask();

        // No negative bonuses means nothing can break later. Take the cheap closure and leave DFS at home (ask EDGN about this one).
        if (negativeMask == 0L) {
            long activeMask = activateFreeItems();
            long addedMask = closePositiveItems(allMask & ~activeMask);
            bestMask = activeMask | addedMask;
            setPlayerToCurrentState(player);
            if (bestMask == allMask) {
                return new Result(equipment, Collections.emptyList());
            }
            bestCount = Long.bitCount(bestMask);
            return createResult(equipment);
        }

        if (impactMasksDirty) {
            prepareImpactMasks();
            impactMasksDirty = false;
        }

        // First be curious, not heroic: try a greedy pass before opening the recursion box.
        long fastMask = greedyAllMask(allMask);
        if (fastMask == allMask) {
            setPlayerToCurrentState(player);
            return new Result(equipment, Collections.emptyList());
        }
        if ((fastMask & negativeMask) == 0L && noInactiveItemCanEquip(fastMask, allMask)) {
            bestMask = fastMask;
            bestCount = Long.bitCount(fastMask);
            bestWeight = maskWeight(fastMask);
            captureBestBonuses();
            return finish(player, equipment);
        }

        resetStateToBase();
        long activeMask = activateFreeItems();
        long remainingMask = allMask & ~activeMask;
        bestMask = activeMask;
        bestCount = Long.bitCount(activeMask);
        bestWeight = freeWeight;
        captureBestBonuses();

        if (tryEquipEverything(remainingMask, allMask)) {
            return finish(player, equipment);
        }

        // Greedy failed, so now we do the real search with pruning.
        prepareVisited();
        search(activeMask, remainingMask, bestCount, bestWeight, totalWeight - freeWeight);
        return finish(player, equipment);
    }

    private void prepareLight(CuriousPlayer player, List<IEquipment> equipment) {
        itemCount = equipment.size();
        if (player == preparedPlayer) {
            // Same player, same prepared arrays. Grab the cached copy and skip all the item decoding.
            restorePrepared(player);
        } else {
            prepareEquipment(player, equipment);
        }
        loadBaseSkillPoints(player);
        resetStateToBase();
    }

    private void restorePrepared(CuriousPlayer player) {
        if (player == activePreparedPlayer) {
            impactMasksDirty = false;
            return;
        }
        items = preparedItems;
        weights = preparedWeights;
        req0 = preparedReq0;
        req1 = preparedReq1;
        req2 = preparedReq2;
        req3 = preparedReq3;
        req4 = preparedReq4;
        bonus0 = preparedBonus0;
        bonus1 = preparedBonus1;
        bonus2 = preparedBonus2;
        bonus3 = preparedBonus3;
        bonus4 = preparedBonus4;
        adjReq0 = preparedAdjReq0;
        adjReq1 = preparedAdjReq1;
        adjReq2 = preparedAdjReq2;
        adjReq3 = preparedAdjReq3;
        adjReq4 = preparedAdjReq4;
        hasRequirement = preparedHasRequirement;
        hasNegativeBonus = preparedHasNegativeBonus;
        impactMasks = preparedImpactMasks;
        negativeMask = preparedNegativeMask;
        freeMask = preparedFreeMask;
        freeBonus0 = preparedFreeBonus0;
        freeBonus1 = preparedFreeBonus1;
        freeBonus2 = preparedFreeBonus2;
        freeBonus3 = preparedFreeBonus3;
        freeBonus4 = preparedFreeBonus4;
        freeWeight = preparedFreeWeight;
        totalWeight = preparedTotalWeight;
        activePreparedPlayer = player;
        impactMasksDirty = false;
    }

    private void prepareEquipment(CuriousPlayer player, List<IEquipment> equipment) {
        activePreparedPlayer = null;
        ensureCapacity(itemCount);
        boolean append = canReuseEquipmentPrefix(equipment);
        int start = append ? lastItemCount : 0;
        boolean reusable = append && lastEquipmentReusable;
        if (!append) {
            resetPreparedStats();
            reusable = true;
        }
        loadNewItems(equipment, start, reusable);
        updatePreparedPlayer(player);
    }

    private void resetPreparedStats() {
        negativeMask = 0L;
        freeMask = 0L;
        freeBonus0 = 0;
        freeBonus1 = 0;
        freeBonus2 = 0;
        freeBonus3 = 0;
        freeBonus4 = 0;
        freeWeight = 0;
        totalWeight = 0;
    }

    private void loadNewItems(List<IEquipment> equipment, int start, boolean reusable) {
        if (start < itemCount) {
            impactMasksDirty = true;
        }
        for (int i = start; i < itemCount; i++) {
            IEquipment item = equipment.get(i);
            int[] req = item.requirements();
            int[] bonus = item.bonuses();
            items[i] = item;
            loadItemStatsLight(i, req, bonus, item.hasNegativeBonus());
            reusable &= item instanceof com.wynncraft.enums.Equipment;
        }
        lastEquipment = equipment;
        lastItemCount = itemCount;
        lastEquipmentReusable = reusable;
    }

    private void updatePreparedPlayer(CuriousPlayer player) {
        if (player == lastPlayer && lastEquipmentReusable) {
            capturePrepared(player);
            return;
        }
        lastPlayer = player;
    }

    private void loadBaseSkillPoints(CuriousPlayer player) {
        base0 = player.allocated[STR];
        base1 = player.allocated[DEX];
        base2 = player.allocated[INT];
        base3 = player.allocated[DEF];
        base4 = player.allocated[AGI];
    }

    private boolean canReuseEquipmentPrefix(List<IEquipment> equipment) {
        if (equipment != lastEquipment || itemCount < lastItemCount || !lastEquipmentReusable) {
            return false;
        }
        for (int i = 0; i < lastItemCount; i++) {
            if (equipment.get(i) != items[i]) {
                return false;
            }
        }
        return true;
    }

    private void capturePrepared(CuriousPlayer player) {
        prepareImpactMasks();
        impactMasksDirty = false;

        preparedItems = Arrays.copyOf(items, itemCount);
        preparedWeights = Arrays.copyOf(weights, itemCount);
        preparedReq0 = Arrays.copyOf(req0, itemCount);
        preparedReq1 = Arrays.copyOf(req1, itemCount);
        preparedReq2 = Arrays.copyOf(req2, itemCount);
        preparedReq3 = Arrays.copyOf(req3, itemCount);
        preparedReq4 = Arrays.copyOf(req4, itemCount);
        preparedBonus0 = Arrays.copyOf(bonus0, itemCount);
        preparedBonus1 = Arrays.copyOf(bonus1, itemCount);
        preparedBonus2 = Arrays.copyOf(bonus2, itemCount);
        preparedBonus3 = Arrays.copyOf(bonus3, itemCount);
        preparedBonus4 = Arrays.copyOf(bonus4, itemCount);
        preparedAdjReq0 = Arrays.copyOf(adjReq0, itemCount);
        preparedAdjReq1 = Arrays.copyOf(adjReq1, itemCount);
        preparedAdjReq2 = Arrays.copyOf(adjReq2, itemCount);
        preparedAdjReq3 = Arrays.copyOf(adjReq3, itemCount);
        preparedAdjReq4 = Arrays.copyOf(adjReq4, itemCount);
        preparedHasRequirement = Arrays.copyOf(hasRequirement, itemCount);
        preparedHasNegativeBonus = Arrays.copyOf(hasNegativeBonus, itemCount);
        preparedImpactMasks = Arrays.copyOf(impactMasks, itemCount);
        preparedNegativeMask = negativeMask;
        preparedFreeMask = freeMask;
        preparedFreeBonus0 = freeBonus0;
        preparedFreeBonus1 = freeBonus1;
        preparedFreeBonus2 = freeBonus2;
        preparedFreeBonus3 = freeBonus3;
        preparedFreeBonus4 = freeBonus4;
        preparedFreeWeight = freeWeight;
        preparedTotalWeight = totalWeight;
        preparedPlayer = player;
    }

    private void loadItemStatsLight(int index, int[] req, int[] bonus, boolean negativeBonus) {
        long bit = 1L << index;
        int r0 = req[STR];
        int r1 = req[DEX];
        int r2 = req[INT];
        int r3 = req[DEF];
        int r4 = req[AGI];
        int b0 = bonus[STR];
        int b1 = bonus[DEX];
        int b2 = bonus[INT];
        int b3 = bonus[DEF];
        int b4 = bonus[AGI];
        req0[index] = r0;
        req1[index] = r1;
        req2[index] = r2;
        req3[index] = r3;
        req4[index] = r4;
        bonus0[index] = b0;
        bonus1[index] = b1;
        bonus2[index] = b2;
        bonus3[index] = b3;
        bonus4[index] = b4;
        // Precompute threshold for itemInvalidWithoutOwnBonus:
        adjReq0[index] = r0 > 0 ? r0 + b0 : Integer.MIN_VALUE;
        adjReq1[index] = r1 > 0 ? r1 + b1 : Integer.MIN_VALUE;
        adjReq2[index] = r2 > 0 ? r2 + b2 : Integer.MIN_VALUE;
        adjReq3[index] = r3 > 0 ? r3 + b3 : Integer.MIN_VALUE;
        adjReq4[index] = r4 > 0 ? r4 + b4 : Integer.MIN_VALUE;
        int w = b0 + b1 + b2 + b3 + b4;
        weights[index] = w;
        boolean hasReq = r0 > 0 || r1 > 0 || r2 > 0 || r3 > 0 || r4 > 0;
        hasRequirement[index] = hasReq;
        hasNegativeBonus[index] = negativeBonus;
        negativeMask |= negativeBonus ? bit : 0L;
        totalWeight += w;
        if (!hasReq && !negativeBonus) {
            freeMask |= bit;
            freeBonus0 += b0;
            freeBonus1 += b1;
            freeBonus2 += b2;
            freeBonus3 += b3;
            freeBonus4 += b4;
            freeWeight += w;
        }
    }

    // Arrays utils somewhat cannot be beaten even on specific edge cases, im just sad.
    private void prepareImpactMasks() {
        Arrays.fill(requiredBySkill, 0L);
        for (int i = 0; i < itemCount; i++) {
            markRequiredSkills(i, 1L << i);
        }
        for (int i = 0; i < itemCount; i++) {
            impactMasks[i] = impactedRequirements(i);
        }
    }

    private void markRequiredSkills(int item, long bit) {
        if (req0[item] > 0) requiredBySkill[STR] |= bit;
        if (req1[item] > 0) requiredBySkill[DEX] |= bit;
        if (req2[item] > 0) requiredBySkill[INT] |= bit;
        if (req3[item] > 0) requiredBySkill[DEF] |= bit;
        if (req4[item] > 0) requiredBySkill[AGI] |= bit;
    }

    private long impactedRequirements(int item) {
        long mask = 0L;
        if (bonus0[item] < 0) mask |= requiredBySkill[STR];
        if (bonus1[item] < 0) mask |= requiredBySkill[DEX];
        if (bonus2[item] < 0) mask |= requiredBySkill[INT];
        if (bonus3[item] < 0) mask |= requiredBySkill[DEF];
        if (bonus4[item] < 0) mask |= requiredBySkill[AGI];
        return mask;
    }

    // Sry for the crazy nesting here <3
    private long greedyAllMask(long allMask) {
        long activeMask = activateFreeItems();
        long remainingMask = allMask & ~activeMask;
        long changedMask;
        do {
            changedMask = 0L;
            long candidates = remainingMask;
            while (candidates != 0L) {
                long bit = candidates & -candidates;
                candidates &= candidates - 1L;
                int item = Long.numberOfTrailingZeros(bit);
                if (canEquipNow(item)) {
                    addBonus(item);
                    long impacted = activeMask & impactMasks[item];
                    if (hasNegativeBonus[item] && impacted != 0L && activeItemsHaveInvalidRequirement(impacted)) {
                        subtractBonus(item);
                    } else {
                        changedMask |= bit;
                        activeMask |= bit;
                        remainingMask &= ~bit;
                    }
                }
            }
        } while (changedMask != 0L);
        return activeMask;
    }

    private boolean noInactiveItemCanEquip(long activeMask, long allMask) {
        long candidates = allMask & ~activeMask;
        while (candidates != 0L) {
            long bit = candidates & -candidates;
            candidates &= candidates - 1L;
            if (canEquipNow(Long.numberOfTrailingZeros(bit))) {
                return false;
            }
        }
        return true;
    }

    private long allItemsMask() {
        return itemCount == 64 ? -1L : (1L << itemCount) - 1L;
    }

    private void resetStateToBase() {
        state[STR] = base0;
        state[DEX] = base1;
        state[INT] = base2;
        state[DEF] = base3;
        state[AGI] = base4;
    }

    private void prepareVisited() {
        useVisited = itemCount <= VISITED_LIMIT;
        if (useVisited) {
            int words = Math.min(VISITED_WORDS, ((1 << itemCount) >>> 6) + 1);
            Arrays.fill(visited, 0, words, 0L);
        }
    }

    private void ensureCapacity(int size) {
        if (items.length >= size) {
            return;
        }
        int capacity = Math.max(size, items.length == 0 ? 16 : items.length * 2);
        items = Arrays.copyOf(items, capacity);
        weights = Arrays.copyOf(weights, capacity);
        req0 = Arrays.copyOf(req0, capacity);
        req1 = Arrays.copyOf(req1, capacity);
        req2 = Arrays.copyOf(req2, capacity);
        req3 = Arrays.copyOf(req3, capacity);
        req4 = Arrays.copyOf(req4, capacity);
        bonus0 = Arrays.copyOf(bonus0, capacity);
        bonus1 = Arrays.copyOf(bonus1, capacity);
        bonus2 = Arrays.copyOf(bonus2, capacity);
        bonus3 = Arrays.copyOf(bonus3, capacity);
        bonus4 = Arrays.copyOf(bonus4, capacity);
        adjReq0 = Arrays.copyOf(adjReq0, capacity);
        adjReq1 = Arrays.copyOf(adjReq1, capacity);
        adjReq2 = Arrays.copyOf(adjReq2, capacity);
        adjReq3 = Arrays.copyOf(adjReq3, capacity);
        adjReq4 = Arrays.copyOf(adjReq4, capacity);
        hasRequirement = Arrays.copyOf(hasRequirement, capacity);
        hasNegativeBonus = Arrays.copyOf(hasNegativeBonus, capacity);
        impactMasks = Arrays.copyOf(impactMasks, capacity);
    }


    // O(1) activation using precomputed freeMask/freeBonus aggregates. This one is as lazy as me.
    private long activateFreeItems() {
        state[STR] += freeBonus0;
        state[DEX] += freeBonus1;
        state[INT] += freeBonus2;
        state[DEF] += freeBonus3;
        state[AGI] += freeBonus4;
        return freeMask;
    }

    private boolean tryEquipEverything(long remainingMask, long allMask) {
        if (!allCanEquipNow(remainingMask)) {
            return false;
        }
        addBonuses(remainingMask);
        if (activeItemsRemainValid(allMask & impactedBy(remainingMask))) {
            bestMask = allMask;
            bestCount = itemCount;
            bestWeight += maskWeight(remainingMask);
            captureBestBonuses();
            return true;
        }
        removeBonuses(remainingMask);
        return false;
    }

    private boolean allCanEquipNow(long mask) {
        long remaining = mask;
        while (remaining != 0L) {
            long bit = remaining & -remaining;
            remaining &= remaining - 1L;
            if (!canEquipNow(Long.numberOfTrailingZeros(bit))) {
                return false;
            }
        }
        return true;
    }

    private long impactedBy(long mask) {
        long impacted = 0L;
        long remaining = mask & negativeMask;
        while (remaining != 0L) {
            long bit = remaining & -remaining;
            remaining &= remaining - 1L;
            impacted |= impactMasks[Long.numberOfTrailingZeros(bit)];
        }
        return impacted;
    }

    private int maskWeight(long mask) {
        int weight = 0;
        long remaining = mask;
        while (remaining != 0L) {
            long bit = remaining & -remaining;
            remaining &= remaining - 1L;
            weight += weights[Long.numberOfTrailingZeros(bit)];
        }
        return weight;
    }

    private void search(long activeMask, long remainingMask, int count, int weight, int remainingWeight) {
        // When only positive items are left, close them immediately instead of branching one by one.
        long addedPositiveMask = (remainingMask & negativeMask) == 0L ? closePositiveItems(remainingMask) : 0L;
        if (addedPositiveMask != 0L) {
            int addedWeight = maskWeight(addedPositiveMask);
            activeMask |= addedPositiveMask;
            remainingMask &= ~addedPositiveMask;
            count += Long.bitCount(addedPositiveMask);
            weight += addedWeight;
            remainingWeight -= addedWeight;
        }

        if (alreadyVisited(activeMask)) {
            removeBonuses(addedPositiveMask);
            return;
        }

        recordBest(activeMask, count, weight);

        if (isSearchExhausted(count, remainingMask, weight, remainingWeight)) {
            removeBonuses(addedPositiveMask);
            return;
        }

        long candidates = remainingMask;
        while (candidates != 0L) {
            long bit = candidates & -candidates;
            candidates &= candidates - 1L;
            int item = Long.numberOfTrailingZeros(bit);

            if (!canEquipNow(item)) {
                continue;
            }

            addBonus(item);
            long impacted = activeMask & impactMasks[item];
            if (!hasNegativeBonus[item] || impacted == 0L || activeItemsRemainValid(impacted)) {
                search(activeMask | bit, remainingMask & ~bit, count + 1, weight + weights[item], remainingWeight - weights[item]);
            }
            subtractBonus(item);
        }

        removeBonuses(addedPositiveMask);
    }

    private void recordBest(long activeMask, int count, int weight) {
        if (count > bestCount || (count == bestCount && weight > bestWeight)) {
            bestMask = activeMask;
            bestCount = count;
            bestWeight = weight;
            captureBestBonuses();
        }
    }

    private boolean isSearchExhausted(int count, long remainingMask, int weight, int remainingWeight) {
        if (bestCount == itemCount) {
            return true;
        }
        int maximumReachableCount = count + Long.bitCount(remainingMask);
        return maximumReachableCount < bestCount || (maximumReachableCount == bestCount && weight + remainingWeight <= bestWeight);
    }

    private long closePositiveItems(long remainingMask) {
        long addedMask = 0L;
        long changedMask;
        do {
            changedMask = 0L;
            long candidates = remainingMask & ~addedMask;
            while (candidates != 0L) {
                long bit = candidates & -candidates;
                candidates &= candidates - 1L;
                int item = Long.numberOfTrailingZeros(bit);
                if (!hasNegativeBonus[item] && canEquipNow(item)) {
                    changedMask |= bit;
                    if (weights[item] != 0) {
                        addBonus(item);
                    }
                }
            }
            addedMask |= changedMask;
        } while (changedMask != 0L);
        return addedMask;
    }

    private boolean alreadyVisited(long mask) {
        if (!useVisited) {
            return false;
        }
        int index = (int) mask;
        int word = index >>> 6;
        long bit = 1L << (index & 63);
        if ((visited[word] & bit) != 0L) {
            return true;
        }
        visited[word] |= bit;
        return false;
    }

    private boolean canEquipNow(int item) {
        return (req0[item] <= 0 || state[STR] >= req0[item])
                && (req1[item] <= 0 || state[DEX] >= req1[item])
                && (req2[item] <= 0 || state[INT] >= req2[item])
                && (req3[item] <= 0 || state[DEF] >= req3[item])
                && (req4[item] <= 0 || state[AGI] >= req4[item]);
    }

    private boolean activeItemsRemainValid(long activeMask) {
        return !activeItemsHaveInvalidRequirement(activeMask);
    }

    private boolean activeItemsHaveInvalidRequirement(long activeMask) {
        long remaining = activeMask;
        while (remaining != 0L) {
            long bit = remaining & -remaining;
            remaining &= remaining - 1L;
            if (itemInvalidWithoutOwnBonus(Long.numberOfTrailingZeros(bit))) {
                return true;
            }
        }
        return false;
    }

    private boolean itemInvalidWithoutOwnBonus(int item) {
        return state[STR] < adjReq0[item]
                || state[DEX] < adjReq1[item]
                || state[INT] < adjReq2[item]
                || state[DEF] < adjReq3[item]
                || state[AGI] < adjReq4[item];
    }

    private void addBonuses(long mask) {
        long remaining = mask;
        while (remaining != 0L) {
            long bit = remaining & -remaining;
            remaining &= remaining - 1L;
            addBonus(Long.numberOfTrailingZeros(bit));
        }
    }

    private void removeBonuses(long mask) {
        long remaining = mask;
        while (remaining != 0L) {
            long bit = remaining & -remaining;
            remaining &= remaining - 1L;
            subtractBonus(Long.numberOfTrailingZeros(bit));
        }
    }

    private void addBonus(int item) {
        state[STR] += bonus0[item];
        state[DEX] += bonus1[item];
        state[INT] += bonus2[item];
        state[DEF] += bonus3[item];
        state[AGI] += bonus4[item];
    }

    private void subtractBonus(int item) {
        state[STR] -= bonus0[item];
        state[DEX] -= bonus1[item];
        state[INT] -= bonus2[item];
        state[DEF] -= bonus3[item];
        state[AGI] -= bonus4[item];
    }

    private void setPlayerToCurrentState(CuriousPlayer player) {
        int b0 = state[STR] - base0;
        int b1 = state[DEX] - base1;
        int b2 = state[INT] - base2;
        int b3 = state[DEF] - base3;
        int b4 = state[AGI] - base4;
        int[] target = player.bonus;
        target[STR] = b0;
        target[DEX] = b1;
        target[INT] = b2;
        target[DEF] = b3;
        target[AGI] = b4;
        player.weight = b0 + b1 + b2 + b3 + b4;
    }

    private void applyBestResult(CuriousPlayer player) {
        if (bestCount == 0) {
            player.reset();
            return;
        }
        int[] target = player.bonus;
        target[STR] = bestBonuses[STR];
        target[DEX] = bestBonuses[DEX];
        target[INT] = bestBonuses[INT];
        target[DEF] = bestBonuses[DEF];
        target[AGI] = bestBonuses[AGI];
        player.weight = bestBonuses[STR] + bestBonuses[DEX] + bestBonuses[INT] + bestBonuses[DEF] + bestBonuses[AGI];
    }

    private Result finish(CuriousPlayer player, List<IEquipment> equipment) {
        applyBestResult(player);
        return createResult(equipment);
    }

    private Result createResult(List<IEquipment> equipment) {
        if (bestCount == itemCount) {
            return new Result(equipment, Collections.emptyList());
        }
        if (bestCount == 0) {
            return new Result(Collections.emptyList(), equipment);
        }
        long allMask = allItemsMask();
        return new Result(
                new MaskEquipmentList(items, bestMask, bestCount),
                new MaskEquipmentList(items, allMask & ~bestMask, itemCount - bestCount));
    }

    private void captureBestBonuses() {
        bestBonuses[STR] = state[STR] - base0;
        bestBonuses[DEX] = state[DEX] - base1;
        bestBonuses[INT] = state[INT] - base2;
        bestBonuses[DEF] = state[DEF] - base3;
        bestBonuses[AGI] = state[AGI] - base4;
    }

    /*
     * Lazy result view. Building ArrayLists here would allocate for no good reason,
     * because callers usually just compare/read the result once.
     */
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
            if (object == this) return true;
            if (!(object instanceof List<?> other) || other.size() != size) return false;
            long remaining = mask;
            for (Object otherItem : other) {
                IEquipment item = items[Long.numberOfTrailingZeros(remaining)];
                if (item != otherItem && !item.equals(otherItem)) return false;
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