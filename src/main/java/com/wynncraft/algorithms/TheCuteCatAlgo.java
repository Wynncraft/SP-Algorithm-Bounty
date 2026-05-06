package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid greedy + Bitset BFS with zero-alloc base cases.
 *
 * Key optimizations over TheSixthAlgorithm:
 * 1. Zero-alloc solve3 — fully unrolled permutations, no temp arrays
 * 2. Zero-alloc solve2 — identical to Sixth
 * 3. Flat bitset word iteration — identical BFS core to Sixth
 * 4. Same bitset dedup, SWAR packing, greedy prepass as Sixth
 *
 * Preserves:
 * - SWAR bit-packing (5 dims in one long, 12-bit slots)
 * - Free-item separation, base cases 1-2-3, greedy pre-pass
 * - negMask bitmask, no-negative-bonus shortcut
 * - Instance-field buffers (zero per-call allocation)
 */
@Information(name = "TheCuteCatAlgo", version = 1, authors = {"AiverAiva"})
public class TheCuteCatAlgo implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    // ── SWAR constants ───────────────────────────────────────────────
    private static final int BIAS = 1024;
    private static final long BIAS5 = 0x0400_4004_0040_0400L;
    private static final long GUARD = 0x0800_8008_0080_0800L;

    private static long pack5(int d0, int d1, int d2, int d3, int d4)
    {
        return (long)(d0 + BIAS)
            | ((long)(d1 + BIAS) << 12)
            | ((long)(d2 + BIAS) << 24)
            | ((long)(d3 + BIAS) << 36)
            | ((long)(d4 + BIAS) << 48);
    }

    private static long packReq(int[] req)
    {
        return  (long)(req[0] != 0 ? req[0] + BIAS : 0)
            | ((long)(req[1] != 0 ? req[1] + BIAS : 0) << 12)
            | ((long)(req[2] != 0 ? req[2] + BIAS : 0) << 24)
            | ((long)(req[3] != 0 ? req[3] + BIAS : 0) << 36)
            | ((long)(req[4] != 0 ? req[4] + BIAS : 0) << 48);
    }

    private static long packNeed(int[] req, int[] bon)
    {
        return  (long)(req[0] != 0 ? req[0] + bon[0] + BIAS : 0)
            | ((long)(req[1] != 0 ? req[1] + bon[1] + BIAS : 0) << 12)
            | ((long)(req[2] != 0 ? req[2] + bon[2] + BIAS : 0) << 24)
            | ((long)(req[3] != 0 ? req[3] + bon[3] + BIAS : 0) << 36)
            | ((long)(req[4] != 0 ? req[4] + bon[4] + BIAS : 0) << 48);
    }

    private static boolean ge5(long skills, long threshold)
    {
        return (((skills | GUARD) - threshold) & GUARD) == GUARD;
    }

    private static long max5(long a, long b)
    {
        long gt = ((a | GUARD) - b) & GUARD;
        long ones = gt >>> 11;
        long mask = gt | (gt - ones);
        return (a & mask) | (b & ~mask);
    }

    // ── Instance-field buffers ───────────────────────────────────────
    private static final int MAX_ITEMS = 10;
    private static final int MAX_MASKS = 1 << 8;

    // Mask-indexed storage
    private final long[] skillNeed = new long[MAX_MASKS * 2];
    private final int[] weightBuf = new int[MAX_MASKS];

    // Bitset dedup (4 longs = 32 bytes)
    private final long[] reachBits = new long[4];

    // Packed item data
    private final long[] pReq = new long[MAX_ITEMS];
    private final long[] pNeed = new long[MAX_ITEMS];
    private final long[] pBon = new long[MAX_ITEMS];

    // Per-call buffers
    private final int[] remainingIndices = new int[MAX_ITEMS];
    private final int[] itemBonusSum = new int[MAX_ITEMS];

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
        int negMask = 0;
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
                boolean neg = (bonuses[0] | bonuses[1] | bonuses[2] | bonuses[3] | bonuses[4]) < 0;
                if (neg) negMask |= (1 << remainingCount);
                itemBonusSum[remainingCount] = bonuses[0] + bonuses[1] + bonuses[2] + bonuses[3] + bonuses[4];
                remainingIndices[remainingCount++] = i;
            }
        }
        if (remainingCount == 0) return result;

        // ── Base case: remainingCount == 1 ───────────────────────────
        if (remainingCount == 1)
        {
            final int[] req = items[remainingIndices[0]].requirements();
            if ((req[0] == 0 || req[0] <= skill0)
                    && (req[1] == 0 || req[1] <= skill1)
                    && (req[2] == 0 || req[2] <= skill2)
                    && (req[3] == 0 || req[3] <= skill3)
                    && (req[4] == 0 || req[4] <= skill4))
            {
                result[remainingIndices[0]] = true;
            }
            return result;
        }

        // ── Base case: remainingCount == 2 ───────────────────────────
        if (remainingCount == 2)
            return solve2(items, remainingIndices, skill0, skill1, skill2, skill3, skill4,
                          itemBonusSum, result);

        // ── Base case: remainingCount == 3 ───────────────────────────
        if (remainingCount == 3)
            return solve3(items, remainingIndices, negMask, skill0, skill1, skill2, skill3, skill4, result);

        // ── Phase 2: Greedy activation ────────────────────────────────
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
                    if ((negMask & (1 << j)) != 0)
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

            if (negMask == 0)
            {
                for (int activeBits = activeMask; activeBits != 0; activeBits &= activeBits - 1)
                    result[remainingIndices[Integer.numberOfTrailingZeros(activeBits)]] = true;
                return result;
            }
        }

        // ── Phase 3: Bitset BFS with flat word iteration ──────────────
        final int totalMasks = 1 << remainingCount;
        final int fullMask = totalMasks - 1;

        long globalMaxReq = 0;
        for (int j = 0; j < remainingCount; j++)
        {
            final int[] req = items[remainingIndices[j]].requirements();
            final int[] bon = items[remainingIndices[j]].bonuses();
            pReq[j] = packReq(req);
            pNeed[j] = packNeed(req, bon);
            pBon[j] = pack5(bon[0], bon[1], bon[2], bon[3], bon[4]);
            globalMaxReq = max5(globalMaxReq, pReq[j]);
        }

        final long[] sn = this.skillNeed;
        final int[] weight = this.weightBuf;
        final long[] reach = this.reachBits;

        sn[0] = pack5(skill0, skill1, skill2, skill3, skill4);
        sn[1] = 0;
        weight[0] = 0;

        final int words = (totalMasks + 63) >>> 6;
        for (int w = 0; w < words; w++) reach[w] = 0;
        reach[0] = 1L;

        int bestMask = 0, bestCount = 0, bestWeight = 0;

        for (int w = 0; w < words; w++)
        {
            int base = w << 6;
            long processed = 0;
            long bits;
            while ((bits = reach[w] & ~processed) != 0)
            {
                int pos = Long.numberOfTrailingZeros(bits);
                processed |= 1L << pos;
                int mask = base + pos;

                int count = Integer.bitCount(mask);
                int maskWeight = weight[mask];
                if (count > bestCount || (count == bestCount && maskWeight > bestWeight))
                {
                    bestCount = count;
                    bestWeight = maskWeight;
                    bestMask = mask;
                }
                if (bestCount == remainingCount) break;

                long curSk = sn[mask << 1];
                long curMn = sn[(mask << 1) + 1];

                boolean allReqsMet = ge5(curSk, globalMaxReq);

                for (int absent = fullMask & ~mask; absent != 0; absent &= absent - 1)
                {
                    int j = Integer.numberOfTrailingZeros(absent);
                    int nextMask = mask | (1 << j);
                    if ((reach[nextMask >>> 6] & (1L << (nextMask & 63))) != 0) continue;
                    if (!allReqsMet && !ge5(curSk, pReq[j])) continue;
                    long nextSk = curSk + pBon[j] - BIAS5;
                    if ((negMask & (1 << j)) != 0)
                    {
                        if (!ge5(nextSk, curMn)) continue;
                    }
                    int nextSnIdx = nextMask << 1;
                    sn[nextSnIdx] = nextSk;
                    sn[nextSnIdx + 1] = max5(curMn, pNeed[j]);
                    weight[nextMask] = maskWeight + itemBonusSum[j];
                    reach[nextMask >>> 6] |= (1L << (nextMask & 63));
                }
            }
            if (bestCount == remainingCount) break;
        }

        for (int j = 0; j < remainingCount; j++)
        {
            if ((bestMask & (1 << j)) != 0) result[remainingIndices[j]] = true;
        }
        return result;
    }

    // ── 2-item fast path ─────────────────────────────────────────────
    private static boolean[] solve2(
            IEquipment[] items, int[] ri,
            int s0, int s1, int s2, int s3, int s4,
            int[] bonusSum,
            boolean[] result)
    {
        final int[] reqA = items[ri[0]].requirements(), bonA = items[ri[0]].bonuses();
        final int[] reqB = items[ri[1]].requirements(), bonB = items[ri[1]].bonuses();

        boolean canEquipA = (reqA[0] == 0 || reqA[0] <= s0)
                         && (reqA[1] == 0 || reqA[1] <= s1)
                         && (reqA[2] == 0 || reqA[2] <= s2)
                         && (reqA[3] == 0 || reqA[3] <= s3)
                         && (reqA[4] == 0 || reqA[4] <= s4);
        boolean canEquipB = (reqB[0] == 0 || reqB[0] <= s0)
                         && (reqB[1] == 0 || reqB[1] <= s1)
                         && (reqB[2] == 0 || reqB[2] <= s2)
                         && (reqB[3] == 0 || reqB[3] <= s3)
                         && (reqB[4] == 0 || reqB[4] <= s4);

        if (canEquipA)
        {
            int as0 = s0 + bonA[0], as1 = s1 + bonA[1], as2 = s2 + bonA[2],
                as3 = s3 + bonA[3], as4 = s4 + bonA[4];
            boolean bAfterA = (reqB[0] == 0 || reqB[0] <= as0)
                           && (reqB[1] == 0 || reqB[1] <= as1)
                           && (reqB[2] == 0 || reqB[2] <= as2)
                           && (reqB[3] == 0 || reqB[3] <= as3)
                           && (reqB[4] == 0 || reqB[4] <= as4);
            if (bAfterA)
            {
                int abs0 = as0 + bonB[0], abs1 = as1 + bonB[1], abs2 = as2 + bonB[2],
                    abs3 = as3 + bonB[3], abs4 = as4 + bonB[4];
                boolean aStillValid = (reqA[0] == 0 || reqA[0] + bonA[0] <= abs0)
                                   && (reqA[1] == 0 || reqA[1] + bonA[1] <= abs1)
                                   && (reqA[2] == 0 || reqA[2] + bonA[2] <= abs2)
                                   && (reqA[3] == 0 || reqA[3] + bonA[3] <= abs3)
                                   && (reqA[4] == 0 || reqA[4] + bonA[4] <= abs4);
                boolean bStillValid = (reqB[0] == 0 || reqB[0] + bonB[0] <= abs0)
                                   && (reqB[1] == 0 || reqB[1] + bonB[1] <= abs1)
                                   && (reqB[2] == 0 || reqB[2] + bonB[2] <= abs2)
                                   && (reqB[3] == 0 || reqB[3] + bonB[3] <= abs3)
                                   && (reqB[4] == 0 || reqB[4] + bonB[4] <= abs4);
                if (aStillValid && bStillValid)
                {
                    result[ri[0]] = true;
                    result[ri[1]] = true;
                    return result;
                }
            }
        }

        if (canEquipB)
        {
            int bs0 = s0 + bonB[0], bs1 = s1 + bonB[1], bs2 = s2 + bonB[2],
                bs3 = s3 + bonB[3], bs4 = s4 + bonB[4];
            boolean aAfterB = (reqA[0] == 0 || reqA[0] <= bs0)
                           && (reqA[1] == 0 || reqA[1] <= bs1)
                           && (reqA[2] == 0 || reqA[2] <= bs2)
                           && (reqA[3] == 0 || reqA[3] <= bs3)
                           && (reqA[4] == 0 || reqA[4] <= bs4);
            if (aAfterB)
            {
                int bas0 = bs0 + bonA[0], bas1 = bs1 + bonA[1], bas2 = bs2 + bonA[2],
                    bas3 = bs3 + bonA[3], bas4 = bs4 + bonA[4];
                boolean bStillValid = (reqB[0] == 0 || reqB[0] + bonB[0] <= bas0)
                                   && (reqB[1] == 0 || reqB[1] + bonB[1] <= bas1)
                                   && (reqB[2] == 0 || reqB[2] + bonB[2] <= bas2)
                                   && (reqB[3] == 0 || reqB[3] + bonB[3] <= bas3)
                                   && (reqB[4] == 0 || reqB[4] + bonB[4] <= bas4);
                boolean aStillValid = (reqA[0] == 0 || reqA[0] + bonA[0] <= bas0)
                                   && (reqA[1] == 0 || reqA[1] + bonA[1] <= bas1)
                                   && (reqA[2] == 0 || reqA[2] + bonA[2] <= bas2)
                                   && (reqA[3] == 0 || reqA[3] + bonA[3] <= bas3)
                                   && (reqA[4] == 0 || reqA[4] + bonA[4] <= bas4);
                if (aStillValid && bStillValid)
                {
                    result[ri[0]] = true;
                    result[ri[1]] = true;
                    return result;
                }
            }
        }

        if (canEquipA && canEquipB)
        {
            result[ri[bonusSum[0] >= bonusSum[1] ? 0 : 1]] = true;
        }
        else if (canEquipA) result[ri[0]] = true;
        else if (canEquipB) result[ri[1]] = true;
        return result;
    }

    // ── 3-item SWAR fast path (ZERO ALLOCATIONS) ────────────────────
    // Fully unrolled — no temp arrays, no heap allocation whatsoever.
    private static boolean[] solve3(
            IEquipment[] items, int[] ri,
            int negMask,
            int s0, int s1, int s2, int s3, int s4,
            boolean[] result)
    {
        long baseSk = pack5(s0, s1, s2, s3, s4);

        long r0 = packReq(items[ri[0]].requirements());
        long r1 = packReq(items[ri[1]].requirements());
        long r2 = packReq(items[ri[2]].requirements());

        long n0 = packNeed(items[ri[0]].requirements(), items[ri[0]].bonuses());
        long n1 = packNeed(items[ri[1]].requirements(), items[ri[1]].bonuses());
        long n2 = packNeed(items[ri[2]].requirements(), items[ri[2]].bonuses());

        long b0 = pack5(items[ri[0]].bonuses()[0], items[ri[0]].bonuses()[1], items[ri[0]].bonuses()[2], items[ri[0]].bonuses()[3], items[ri[0]].bonuses()[4]);
        long b1 = pack5(items[ri[1]].bonuses()[0], items[ri[1]].bonuses()[1], items[ri[1]].bonuses()[2], items[ri[1]].bonuses()[3], items[ri[1]].bonuses()[4]);
        long b2 = pack5(items[ri[2]].bonuses()[0], items[ri[2]].bonuses()[1], items[ri[2]].bonuses()[2], items[ri[2]].bonuses()[3], items[ri[2]].bonuses()[4]);

        int bs0 = items[ri[0]].bonuses()[0] + items[ri[0]].bonuses()[1] + items[ri[0]].bonuses()[2] + items[ri[0]].bonuses()[3] + items[ri[0]].bonuses()[4];
        int bs1 = items[ri[1]].bonuses()[0] + items[ri[1]].bonuses()[1] + items[ri[1]].bonuses()[2] + items[ri[1]].bonuses()[3] + items[ri[1]].bonuses()[4];
        int bs2 = items[ri[2]].bonuses()[0] + items[ri[2]].bonuses()[1] + items[ri[2]].bonuses()[2] + items[ri[2]].bonuses()[3] + items[ri[2]].bonuses()[4];

        boolean neg0 = (negMask & 1) != 0, neg1 = (negMask & 2) != 0, neg2 = (negMask & 4) != 0;

        int bestMask = 0, bestCount = 0, bestWeight = 0;

        // ── Single-item subsets ──
        if (ge5(baseSk, r0))
        {
            if (1 > bestCount || (1 == bestCount && bs0 > bestWeight))
            { bestCount = 1; bestWeight = bs0; bestMask = 1; }
        }
        if (ge5(baseSk, r1))
        {
            if (1 > bestCount || (1 == bestCount && bs1 > bestWeight))
            { bestCount = 1; bestWeight = bs1; bestMask = 2; }
        }
        if (ge5(baseSk, r2))
        {
            if (1 > bestCount || (1 == bestCount && bs2 > bestWeight))
            { bestCount = 1; bestWeight = bs2; bestMask = 4; }
        }

        // ── Two-item subsets ──
        // {0,1}
        {
            long sk = baseSk; long mn = 0; boolean valid = true;
            if (!ge5(sk, r0)) valid = false;
            if (valid) { sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) valid = false; mn = max5(mn, n0); }
            if (valid) { if (!ge5(sk, r1)) valid = false; }
            if (valid) { sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) valid = false; mn = max5(mn, n1); }
            if (valid) { if (!ge5(sk, n0) || !ge5(sk, n1)) valid = false; }
            if (valid) { int w = bs0 + bs1; if (2 > bestCount || (2 == bestCount && w > bestWeight)) { bestCount = 2; bestWeight = w; bestMask = 3; } }
        }
        // {1,0}
        {
            long sk = baseSk; long mn = 0; boolean valid = true;
            if (!ge5(sk, r1)) valid = false;
            if (valid) { sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) valid = false; mn = max5(mn, n1); }
            if (valid) { if (!ge5(sk, r0)) valid = false; }
            if (valid) { sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) valid = false; mn = max5(mn, n0); }
            if (valid) { if (!ge5(sk, n0) || !ge5(sk, n1)) valid = false; }
            if (valid) { int w = bs0 + bs1; if (2 > bestCount || (2 == bestCount && w > bestWeight)) { bestCount = 2; bestWeight = w; bestMask = 3; } }
        }
        // {0,2}
        {
            long sk = baseSk; long mn = 0; boolean valid = true;
            if (!ge5(sk, r0)) valid = false;
            if (valid) { sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) valid = false; mn = max5(mn, n0); }
            if (valid) { if (!ge5(sk, r2)) valid = false; }
            if (valid) { sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) valid = false; mn = max5(mn, n2); }
            if (valid) { if (!ge5(sk, n0) || !ge5(sk, n2)) valid = false; }
            if (valid) { int w = bs0 + bs2; if (2 > bestCount || (2 == bestCount && w > bestWeight)) { bestCount = 2; bestWeight = w; bestMask = 5; } }
        }
        // {2,0}
        {
            long sk = baseSk; long mn = 0; boolean valid = true;
            if (!ge5(sk, r2)) valid = false;
            if (valid) { sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) valid = false; mn = max5(mn, n2); }
            if (valid) { if (!ge5(sk, r0)) valid = false; }
            if (valid) { sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) valid = false; mn = max5(mn, n0); }
            if (valid) { if (!ge5(sk, n0) || !ge5(sk, n2)) valid = false; }
            if (valid) { int w = bs0 + bs2; if (2 > bestCount || (2 == bestCount && w > bestWeight)) { bestCount = 2; bestWeight = w; bestMask = 5; } }
        }
        // {1,2}
        {
            long sk = baseSk; long mn = 0; boolean valid = true;
            if (!ge5(sk, r1)) valid = false;
            if (valid) { sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) valid = false; mn = max5(mn, n1); }
            if (valid) { if (!ge5(sk, r2)) valid = false; }
            if (valid) { sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) valid = false; mn = max5(mn, n2); }
            if (valid) { if (!ge5(sk, n1) || !ge5(sk, n2)) valid = false; }
            if (valid) { int w = bs1 + bs2; if (2 > bestCount || (2 == bestCount && w > bestWeight)) { bestCount = 2; bestWeight = w; bestMask = 6; } }
        }
        // {2,1}
        {
            long sk = baseSk; long mn = 0; boolean valid = true;
            if (!ge5(sk, r2)) valid = false;
            if (valid) { sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) valid = false; mn = max5(mn, n2); }
            if (valid) { if (!ge5(sk, r1)) valid = false; }
            if (valid) { sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) valid = false; mn = max5(mn, n1); }
            if (valid) { if (!ge5(sk, n1) || !ge5(sk, n2)) valid = false; }
            if (valid) { int w = bs1 + bs2; if (2 > bestCount || (2 == bestCount && w > bestWeight)) { bestCount = 2; bestWeight = w; bestMask = 6; } }
        }

        // ── Three-item subset {0,1,2} — fully unrolled 6 permutations ──
        // Uses labeled break for zero-alloc early exit. Java has no goto.
        boolean found3 = false;

        // Permutation 0,1,2
        p012: {
            long sk = baseSk; long mn = 0;
            if (!ge5(sk, r0)) break p012;
            sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) break p012; mn = max5(mn, n0);
            if (!ge5(sk, r1)) break p012;
            sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) break p012; mn = max5(mn, n1);
            if (!ge5(sk, r2)) break p012;
            sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) break p012; mn = max5(mn, n2);
            if (!ge5(sk, n0) || !ge5(sk, n1) || !ge5(sk, n2)) break p012;
            bestCount = 3; bestWeight = bs0 + bs1 + bs2; bestMask = 7; found3 = true;
        }
        // Permutation 0,2,1
        if (!found3) p021: {
            long sk = baseSk; long mn = 0;
            if (!ge5(sk, r0)) break p021;
            sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) break p021; mn = max5(mn, n0);
            if (!ge5(sk, r2)) break p021;
            sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) break p021; mn = max5(mn, n2);
            if (!ge5(sk, r1)) break p021;
            sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) break p021; mn = max5(mn, n1);
            if (!ge5(sk, n0) || !ge5(sk, n1) || !ge5(sk, n2)) break p021;
            bestCount = 3; bestWeight = bs0 + bs1 + bs2; bestMask = 7; found3 = true;
        }
        // Permutation 1,0,2
        if (!found3) p102: {
            long sk = baseSk; long mn = 0;
            if (!ge5(sk, r1)) break p102;
            sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) break p102; mn = max5(mn, n1);
            if (!ge5(sk, r0)) break p102;
            sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) break p102; mn = max5(mn, n0);
            if (!ge5(sk, r2)) break p102;
            sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) break p102; mn = max5(mn, n2);
            if (!ge5(sk, n0) || !ge5(sk, n1) || !ge5(sk, n2)) break p102;
            bestCount = 3; bestWeight = bs0 + bs1 + bs2; bestMask = 7; found3 = true;
        }
        // Permutation 1,2,0
        if (!found3) p120: {
            long sk = baseSk; long mn = 0;
            if (!ge5(sk, r1)) break p120;
            sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) break p120; mn = max5(mn, n1);
            if (!ge5(sk, r2)) break p120;
            sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) break p120; mn = max5(mn, n2);
            if (!ge5(sk, r0)) break p120;
            sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) break p120; mn = max5(mn, n0);
            if (!ge5(sk, n0) || !ge5(sk, n1) || !ge5(sk, n2)) break p120;
            bestCount = 3; bestWeight = bs0 + bs1 + bs2; bestMask = 7; found3 = true;
        }
        // Permutation 2,0,1
        if (!found3) p201: {
            long sk = baseSk; long mn = 0;
            if (!ge5(sk, r2)) break p201;
            sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) break p201; mn = max5(mn, n2);
            if (!ge5(sk, r0)) break p201;
            sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) break p201; mn = max5(mn, n0);
            if (!ge5(sk, r1)) break p201;
            sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) break p201; mn = max5(mn, n1);
            if (!ge5(sk, n0) || !ge5(sk, n1) || !ge5(sk, n2)) break p201;
            bestCount = 3; bestWeight = bs0 + bs1 + bs2; bestMask = 7; found3 = true;
        }
        // Permutation 2,1,0
        if (!found3) p210: {
            long sk = baseSk; long mn = 0;
            if (!ge5(sk, r2)) break p210;
            sk = sk + b2 - BIAS5; if (neg2 && !ge5(sk, mn)) break p210; mn = max5(mn, n2);
            if (!ge5(sk, r1)) break p210;
            sk = sk + b1 - BIAS5; if (neg1 && !ge5(sk, mn)) break p210; mn = max5(mn, n1);
            if (!ge5(sk, r0)) break p210;
            sk = sk + b0 - BIAS5; if (neg0 && !ge5(sk, mn)) break p210; mn = max5(mn, n0);
            if (!ge5(sk, n0) || !ge5(sk, n1) || !ge5(sk, n2)) break p210;
            bestCount = 3; bestWeight = bs0 + bs1 + bs2; bestMask = 7;
        }

        for (int j = 0; j < 3; j++)
            if ((bestMask & (1 << j)) != 0) result[ri[j]] = true;
        return result;
    }
}
