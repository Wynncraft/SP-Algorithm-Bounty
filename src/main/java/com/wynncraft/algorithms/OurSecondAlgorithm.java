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
 * Hybrid scalar/packed algorithm based on kcinsoft's MyFirstAlgorithm.
 *
 * IDENTICAL:
 * Phases 1-2 (fast path): scalar code with MySecondAlgorithm micro-opts
 * (bitwise OR negative detection, no allCanBeValid pre-check).
 *
 * NEW FUNNY CLAUDE IMPLEMENTED OPTIMIZATION:
 * Phase 3 (slow path): ALL 5 skill dims packed into a single long with
 * 12-bit slots and guard-bit SWAR comparison + maxNeed precomputation.
 *
 * Packing layout: 5 dims in 12-bit slots of one long.
 *   [11:0]=dim0, [23:12]=dim1, [35:24]=dim2, [47:36]=dim3, [59:48]=dim4
 * Each slot biased by +1024 so all values are unsigned (fits 11 bits).
 * Bit 11 of each slot is a guard bit for overflow/underflow detection.
 *
 * Vulnerability: reliance on game constraint: cumulative skill values
 * need to always stay within [-1024, 1023]. This guarantees:
 * - Biased values fit in 11 bits [0, 2047]
 * - SWAR addition has no cross-slot carry (max sum 4094 < 4096)
 * - SWAR subtraction (bias correction) has no cross-slot borrow
 */
@Information(name = "OurSecondAlgorithm", version = 1, authors = {"mitcyf"})
public class OurSecondAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    private static final int BIAS = 1024;
    private static final long BIAS5 = 0x0400_4004_0040_0400L;
    private static final long GUARD = 0x0800_8008_0080_0800L;

    private static final int MAX_ITEMS = 10;
    private static final int MAX_MASKS = 1 << 8;

    // Pre-allocated Phase 3 per-item buffers
    private final long[] pReq = new long[MAX_ITEMS];
    private final long[] pNeed = new long[MAX_ITEMS];
    private final long[] pBon = new long[MAX_ITEMS];

    // Pre-allocated per-mask buffers
    private final long[] pMaskSk = new long[MAX_MASKS];
    private final long[] maxNeedBuf = new long[MAX_MASKS];
    private final int[] weightBuf = new int[MAX_MASKS];
    private final boolean[] reachBuf = new boolean[MAX_MASKS];

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

    /** Pack all 5 skill values with bias into 12-bit slots. */
    private static long pack5(int d0, int d1, int d2, int d3, int d4)
    {
        return (long)(d0 + BIAS)
             | ((long)(d1 + BIAS) << 12)
             | ((long)(d2 + BIAS) << 24)
             | ((long)(d3 + BIAS) << 36)
             | ((long)(d4 + BIAS) << 48);
    }

    /**
     * Check: for all 5 packed dims, skills >= threshold.
     * One subtraction replaces 10 branches.
     */
    private static boolean ge5(long skills, long threshold)
    {
        return (((skills | GUARD) - threshold) & GUARD) == GUARD;
    }

    /**
     * Per-slot max of two packed longs (all 5 dims).
     * Guard bits at positions 11, 23, 35, 47, 59.
     * >>> 11 moves each guard bit to bit 0 of its 12-bit slot.
     */
    private static long max5(long a, long b)
    {
        long gt = ((a | GUARD) - b) & GUARD;
        long ones = gt >>> 11;
        long mask = gt | (gt - ones);
        return (a & mask) | (b & ~mask);
    }

    private boolean[] check(IEquipment[] items, int[] assignedSkillpoints)
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
                    && (bonuses[0] | bonuses[1] | bonuses[2] | bonuses[3] | bonuses[4]) >= 0)
            {
                result[i] = true;
                skill0 += bonuses[0]; skill1 += bonuses[1]; skill2 += bonuses[2];
                skill3 += bonuses[3]; skill4 += bonuses[4];
            }
            else
            {
                hasNegativeBonus[remainingCount] = (bonuses[0] | bonuses[1] | bonuses[2] | bonuses[3] | bonuses[4]) < 0;
                itemBonusSum[remainingCount] = bonuses[0] + bonuses[1] + bonuses[2] + bonuses[3] + bonuses[4];
                remainingIndices[remainingCount++] = i;
            }
        }
        if (remainingCount == 0) return result;

        // ── Phase 2: Greedy ordering attempt (unconditional) ─────────
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

        // ── Phase 3: Packed BitmaskDP — all 5 dims in one long ──────
        final int totalMasks = 1 << remainingCount;
        final long[] pmsk = this.pMaskSk;
        final long[] mn = this.maxNeedBuf;
        final int[] weight = this.weightBuf;
        final boolean[] reachable = this.reachBuf;

        final long[] pr = this.pReq;
        final long[] pn = this.pNeed;
        final long[] pb = this.pBon;

        for (int j = 0; j < remainingCount; j++)
        {
            final int[] req = items[remainingIndices[j]].requirements();
            final int[] bon = items[remainingIndices[j]].bonuses();
            pr[j] = ((long)(req[0] != 0 ? req[0] + BIAS : 0))
                   | ((long)(req[1] != 0 ? req[1] + BIAS : 0) << 12)
                   | ((long)(req[2] != 0 ? req[2] + BIAS : 0) << 24)
                   | ((long)(req[3] != 0 ? req[3] + BIAS : 0) << 36)
                   | ((long)(req[4] != 0 ? req[4] + BIAS : 0) << 48);
            pn[j] = ((long)(req[0] != 0 ? req[0] + bon[0] + BIAS : 0))
                   | ((long)(req[1] != 0 ? req[1] + bon[1] + BIAS : 0) << 12)
                   | ((long)(req[2] != 0 ? req[2] + bon[2] + BIAS : 0) << 24)
                   | ((long)(req[3] != 0 ? req[3] + bon[3] + BIAS : 0) << 36)
                   | ((long)(req[4] != 0 ? req[4] + bon[4] + BIAS : 0) << 48);
            pb[j] = pack5(bon[0], bon[1], bon[2], bon[3], bon[4]);
        }

        // BFS with lazy computation — only compute pmsk/weight/mn
        // for masks that are actually reached, not all 2^n masks.
        Arrays.fill(reachable, 0, totalMasks, false);
        reachable[0] = true;
        pmsk[0] = pack5(skill0, skill1, skill2, skill3, skill4);
        weight[0] = 0;
        mn[0] = 0;
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

            long curSk = pmsk[mask];
            long curMn = mn[mask];
            int curW = weight[mask];

            for (int j = 0; j < remainingCount; j++)
            {
                if ((mask & (1 << j)) != 0) continue;
                int nextMask = mask | (1 << j);
                if (reachable[nextMask]) continue;
                if (!ge5(curSk, pr[j])) continue;
                long nextSk = curSk + pb[j] - BIAS5;
                if (hasNegativeBonus[j])
                {
                    if (!ge5(nextSk, curMn)) continue;
                }
                // First reach of nextMask — store its data
                reachable[nextMask] = true;
                pmsk[nextMask] = nextSk;
                weight[nextMask] = curW + itemBonusSum[j];
                mn[nextMask] = max5(curMn, pn[j]);
            }
        }

        for (int j = 0; j < remainingCount; j++)
        {
            if ((bestMask & (1 << j)) != 0) result[remainingIndices[j]] = true;
        }
        return result;
    }
}
