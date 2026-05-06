package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hybrid greedy + BFS-over-bitmasks algorithm.
 *
 * Fast path: greedy activation handles the common case (all items equippable)
 * with zero precomputation overhead — typically under 100 ns.
 *
 * Slow path: BFS over bitmask states finds the provably optimal subset in
 * O(m^2 * 2^m) when the greedy can't equip everything.
 */
@Information(name = "MyFirstAlgorithm", version = 1, authors = {"kcinsoft"})
public class MyFirstAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    private static final int MAX_MASKS = 1 << 8;
    private final int[] skillpointBuffer = new int[MAX_MASKS * 5];
    private final int[] weightBuffer = new int[MAX_MASKS];
    private final boolean[] reachableBuffer = new boolean[MAX_MASKS];

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        IEquipment[] items = equipment.toArray(new IEquipment[0]);
        int[] assignedSP = new int[SKILL_POINTS.length];
        for (int i = 0; i < SKILL_POINTS.length; i++) {
            assignedSP[i] = player.allocated(SKILL_POINTS[i]);
        }

        boolean[] keep = check(items, assignedSP);

        List<IEquipment> valid = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            if (keep[i]) {
                valid.add(items[i]);
                player.modify(items[i].bonuses(), true);
            } else {
                invalid.add(items[i]);
            }
        }
        return new Result(valid, invalid);
    }

    public boolean[] check(IEquipment[] items, int[] assignedSkillpoints)
    {
        final int itemCount = items.length;
        final boolean[] result = new boolean[itemCount];
        if (itemCount == 0) return result;

        int skill0 = assignedSkillpoints[0], skill1 = assignedSkillpoints[1],
            skill2 = assignedSkillpoints[2], skill3 = assignedSkillpoints[3],
            skill4 = assignedSkillpoints[4];

        // ── Phase 1: Free items (no reqs, no negative bonuses) ───────
        int remainingCount = 0;
        final int[] remainingIndices = new int[itemCount];
        final boolean[] hasNegativeBonus = new boolean[itemCount];
        final int[] itemBonusSum = new int[itemCount];
        for (int i = 0; i < itemCount; i++)
        {
            final int[] requirements = items[i].requirements(), bonuses = items[i].bonuses();
            if ((requirements[0] | requirements[1] | requirements[2] | requirements[3] | requirements[4]) == 0
                    && bonuses[0] >= 0 && bonuses[1] >= 0 && bonuses[2] >= 0 && bonuses[3] >= 0 && bonuses[4] >= 0)
            {
                result[i] = true;
                skill0 += bonuses[0]; skill1 += bonuses[1]; skill2 += bonuses[2];
                skill3 += bonuses[3]; skill4 += bonuses[4];
            }
            else
            {
                hasNegativeBonus[remainingCount] = bonuses[0] < 0 || bonuses[1] < 0 || bonuses[2] < 0 || bonuses[3] < 0 || bonuses[4] < 0;
                itemBonusSum[remainingCount] = bonuses[0] + bonuses[1] + bonuses[2] + bonuses[3] + bonuses[4];
                remainingIndices[remainingCount++] = i;
            }
        }
        if (remainingCount == 0) return result;

        // ── Phase 2: Quick all-valid check at final SP ───────────────
        int finalSkill0 = skill0, finalSkill1 = skill1, finalSkill2 = skill2,
            finalSkill3 = skill3, finalSkill4 = skill4;
        for (int j = 0; j < remainingCount; j++)
        {
            final int[] bonuses = items[remainingIndices[j]].bonuses();
            finalSkill0 += bonuses[0]; finalSkill1 += bonuses[1]; finalSkill2 += bonuses[2];
            finalSkill3 += bonuses[3]; finalSkill4 += bonuses[4];
        }
        boolean allCanBeValid = true;
        for (int j = 0; j < remainingCount; j++)
        {
            final int[] requirements = items[remainingIndices[j]].requirements();
            final int[] bonuses = items[remainingIndices[j]].bonuses();
            if ((requirements[0] != 0 && requirements[0] + bonuses[0] > finalSkill0)
                    || (requirements[1] != 0 && requirements[1] + bonuses[1] > finalSkill1)
                    || (requirements[2] != 0 && requirements[2] + bonuses[2] > finalSkill2)
                    || (requirements[3] != 0 && requirements[3] + bonuses[3] > finalSkill3)
                    || (requirements[4] != 0 && requirements[4] + bonuses[4] > finalSkill4))
            {
                allCanBeValid = false;
                break;
            }
        }

        // ── Phase 3: Greedy ordering attempt ─────────────────────────
        if (allCanBeValid)
        {
            int currentSkill0 = skill0, currentSkill1 = skill1, currentSkill2 = skill2,
                currentSkill3 = skill3, currentSkill4 = skill4;
            int activeMask = 0;
            int activeCount = 0;
            boolean changed = true;
            while (changed)
            {
                changed = false;
                for (int j = 0; j < remainingCount; j++)
                {
                    if ((activeMask & (1 << j)) != 0) continue;
                    final int[] requirements = items[remainingIndices[j]].requirements();
                    if ((requirements[0] != 0 && requirements[0] > currentSkill0)
                            || (requirements[1] != 0 && requirements[1] > currentSkill1)
                            || (requirements[2] != 0 && requirements[2] > currentSkill2)
                            || (requirements[3] != 0 && requirements[3] > currentSkill3)
                            || (requirements[4] != 0 && requirements[4] > currentSkill4))
                        continue;

                    final int[] bonuses = items[remainingIndices[j]].bonuses();
                    if (hasNegativeBonus[j])
                    {
                        int testSkill0 = currentSkill0 + bonuses[0], testSkill1 = currentSkill1 + bonuses[1],
                            testSkill2 = currentSkill2 + bonuses[2], testSkill3 = currentSkill3 + bonuses[3],
                            testSkill4 = currentSkill4 + bonuses[4];
                        boolean valid = true;
                        for (int activeBits = activeMask; activeBits != 0; activeBits &= activeBits - 1)
                        {
                            int activeIdx = Integer.numberOfTrailingZeros(activeBits);
                            final int[] activeReqs = items[remainingIndices[activeIdx]].requirements();
                            final int[] activeBonuses = items[remainingIndices[activeIdx]].bonuses();
                            if ((activeReqs[0] != 0 && activeReqs[0] + activeBonuses[0] > testSkill0)
                                    || (activeReqs[1] != 0 && activeReqs[1] + activeBonuses[1] > testSkill1)
                                    || (activeReqs[2] != 0 && activeReqs[2] + activeBonuses[2] > testSkill2)
                                    || (activeReqs[3] != 0 && activeReqs[3] + activeBonuses[3] > testSkill3)
                                    || (activeReqs[4] != 0 && activeReqs[4] + activeBonuses[4] > testSkill4))
                            {
                                valid = false;
                                break;
                            }
                        }
                        if (!valid) continue;
                    }
                    activeMask |= (1 << j);
                    activeCount++;
                    currentSkill0 += bonuses[0]; currentSkill1 += bonuses[1]; currentSkill2 += bonuses[2];
                    currentSkill3 += bonuses[3]; currentSkill4 += bonuses[4];
                    changed = true;
                }
            }
            if (activeCount == remainingCount)
            {
                for (int j = 0; j < remainingCount; j++) result[remainingIndices[j]] = true;
                return result;
            }
        }

        // ── Phase 4: BitmaskDP fallback for optimal subset ───────────
        final int totalMasks = 1 << remainingCount;
        final int[] skillpoints = this.skillpointBuffer;
        final int[] weight = this.weightBuffer;
        final boolean[] reachable = this.reachableBuffer;

        skillpoints[0] = skill0; skillpoints[1] = skill1; skillpoints[2] = skill2;
        skillpoints[3] = skill3; skillpoints[4] = skill4;
        weight[0] = 0;

        for (int mask = 1; mask < totalMasks; mask++)
        {
            int lowestBit = Integer.numberOfTrailingZeros(mask);
            int prevMask = mask ^ (1 << lowestBit);
            final int[] bonuses = items[remainingIndices[lowestBit]].bonuses();
            int maskOffset = mask * 5, prevOffset = prevMask * 5;
            skillpoints[maskOffset]     = skillpoints[prevOffset]     + bonuses[0];
            skillpoints[maskOffset + 1] = skillpoints[prevOffset + 1] + bonuses[1];
            skillpoints[maskOffset + 2] = skillpoints[prevOffset + 2] + bonuses[2];
            skillpoints[maskOffset + 3] = skillpoints[prevOffset + 3] + bonuses[3];
            skillpoints[maskOffset + 4] = skillpoints[prevOffset + 4] + bonuses[4];
            weight[mask] = weight[prevMask] + itemBonusSum[lowestBit];
        }

        Arrays.fill(reachable, 0, totalMasks, false);
        reachable[0] = true;
        int bestMask = 0, bestCount = 0, bestWeight = 0;

        for (int mask = 0; mask < totalMasks; mask++)
        {
            if (!reachable[mask]) continue;
            int count = Integer.bitCount(mask);
            int maskWeight = weight[mask];
            if (count > bestCount || (count == bestCount && maskWeight > bestWeight))
            {
                bestCount = count;
                bestWeight = maskWeight;
                bestMask = mask;
            }
            if (bestCount == remainingCount) break;

            int maskBase = mask * 5;
            int maskSkill0 = skillpoints[maskBase], maskSkill1 = skillpoints[maskBase + 1],
                maskSkill2 = skillpoints[maskBase + 2], maskSkill3 = skillpoints[maskBase + 3],
                maskSkill4 = skillpoints[maskBase + 4];

            for (int j = 0; j < remainingCount; j++)
            {
                if ((mask & (1 << j)) != 0) continue;
                int nextMask = mask | (1 << j);
                if (reachable[nextMask]) continue;
                final int[] requirements = items[remainingIndices[j]].requirements();
                if ((requirements[0] != 0 && requirements[0] > maskSkill0)
                        || (requirements[1] != 0 && requirements[1] > maskSkill1)
                        || (requirements[2] != 0 && requirements[2] > maskSkill2)
                        || (requirements[3] != 0 && requirements[3] > maskSkill3)
                        || (requirements[4] != 0 && requirements[4] > maskSkill4))
                    continue;
                if (hasNegativeBonus[j])
                {
                    int nextOffset = nextMask * 5;
                    int nextSkill0 = skillpoints[nextOffset], nextSkill1 = skillpoints[nextOffset + 1],
                        nextSkill2 = skillpoints[nextOffset + 2], nextSkill3 = skillpoints[nextOffset + 3],
                        nextSkill4 = skillpoints[nextOffset + 4];
                    boolean valid = true;
                    for (int activeBits = mask; activeBits != 0; activeBits &= activeBits - 1)
                    {
                        int activeIdx = Integer.numberOfTrailingZeros(activeBits);
                        final int[] activeReqs = items[remainingIndices[activeIdx]].requirements();
                        final int[] activeBonuses = items[remainingIndices[activeIdx]].bonuses();
                        if ((activeReqs[0] != 0 && activeReqs[0] + activeBonuses[0] > nextSkill0)
                                || (activeReqs[1] != 0 && activeReqs[1] + activeBonuses[1] > nextSkill1)
                                || (activeReqs[2] != 0 && activeReqs[2] + activeBonuses[2] > nextSkill2)
                                || (activeReqs[3] != 0 && activeReqs[3] + activeBonuses[3] > nextSkill3)
                                || (activeReqs[4] != 0 && activeReqs[4] + activeBonuses[4] > nextSkill4))
                        {
                            valid = false;
                            break;
                        }
                    }
                    if (!valid) continue;
                }
                reachable[nextMask] = true;
            }
        }

        for (int j = 0; j < remainingCount; j++)
        {
            if ((bestMask & (1 << j)) != 0) result[remainingIndices[j]] = true;
        }
        return result;
    }
}
