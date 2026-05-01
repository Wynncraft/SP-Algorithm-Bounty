package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * V4 is a recombination, not an original algorithm. It applies V3's
 * allocation discipline (instance-resident scratch, cached reqs/bonuses,
 * equipment.get(i) instead of toArray) on top of the SWAR + bitmask-DP BFS
 * core that originated in TheThirdAlgorithm (kcinsoft) and was further
 * tuned in TheCuteCatAlgo (AiverAiva).
 *
 * Specifically, the following are taken effectively wholesale from those
 * algorithms and credit goes to their authors:
 *   - 12-bit-slot SWAR packing with BIAS = 1024, BIAS5, GUARD constants.
 *   - pack5 / packReq / packNeed / ge5 / max5 — math and implementation.
 *   - The pNeed[j] = req+bon (biased) representation and the per-mask
 *     "cascade floor = max5(pNeed) over activated" trick that collapses
 *     cascade-sustain into one ge5 (TheThirdAlgorithm).
 *   - Three-phase structure (free-pre-pass, scalar greedy, SWAR BFS).
 *   - Bitset-reach BFS with `processed` mask + flat word iteration.
 *   - globalMaxReq early-out for the per-item ge5 in the BFS expand step
 *     (TheCuteCatAlgo).
 *
 * What V4 contributes on top:
 *   - Zero per-call scratch allocation: per-item arrays grow lazily and
 *     are reused across run() invocations. Lets us amortize setup cost in
 *     the OneByOne workload where the leaders re-allocate each call.
 *   - reqMask/negMask bitmasks restricted to the cascade re-walk, so it
 *     skips activated items whose reqs don't intersect the negative attrs.
 *   - Hot-loop state held in 5 local sp ints and an activeMask int, with
 *     keep[] written once at the very end.
 */
@Information(name = "WynnSolver", version = 4, authors = {"Alex-Guha", "kcinsoft", "AiverAiva"})
public class WynnSolverV4Algorithm implements IAlgorithm<WynnPlayer> {

    private static final int SP_COUNT = 5;
    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int CAP = 16;
    private static final int MAX_K = 10;
    private static final int MAX_MASKS = 1 << MAX_K;

    private static final int BIAS = 1024;
    private static final long BIAS5 =
            (long) BIAS
            | ((long) BIAS << 12)
            | ((long) BIAS << 24)
            | ((long) BIAS << 36)
            | ((long) BIAS << 48);
    private static final long GUARD = 0x0800_8008_0080_0800L;

    // Per-item scratch.
    private IEquipment[] items = new IEquipment[CAP];
    private int[][] reqsRaw = new int[CAP][];
    private int[][] bonusesRaw = new int[CAP][];
    private int[] reqMask = new int[CAP];
    private int[] negMask = new int[CAP];
    private boolean[] keep = new boolean[CAP];

    // Phase-3 packed-item slots.
    private long[] pReq = new long[CAP];
    private long[] pBon = new long[CAP];
    private long[] pNeed = new long[CAP];
    private int[] remIdx = new int[CAP];
    private int[] bonusSum = new int[CAP];
    private boolean[] hasNeg = new boolean[CAP];

    // Mask-indexed BFS buffers.
    private final long[] skillByMask = new long[MAX_MASKS];
    private final long[] floorByMask = new long[MAX_MASKS];
    private final int[] weightByMask = new int[MAX_MASKS];
    private final long[] reach = new long[MAX_MASKS / 64];

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int n = equipment.size();

        if (items.length < n) {
            int cap = Math.max(n, items.length * 2);
            items = new IEquipment[cap];
            reqsRaw = new int[cap][];
            bonusesRaw = new int[cap][];
            reqMask = new int[cap];
            negMask = new int[cap];
            keep = new boolean[cap];
            pReq = new long[cap];
            pBon = new long[cap];
            pNeed = new long[cap];
            remIdx = new int[cap];
            bonusSum = new int[cap];
            hasNeg = new boolean[cap];
        }

        IEquipment[] items = this.items;
        int[][] reqsRaw = this.reqsRaw;
        int[][] bonusesRaw = this.bonusesRaw;
        int[] reqMask = this.reqMask;
        int[] negMask = this.negMask;
        boolean[] keep = this.keep;

        int sp0 = player.allocated(SKILL_POINTS[0]);
        int sp1 = player.allocated(SKILL_POINTS[1]);
        int sp2 = player.allocated(SKILL_POINTS[2]);
        int sp3 = player.allocated(SKILL_POINTS[3]);
        int sp4 = player.allocated(SKILL_POINTS[4]);

        // ── Phase 1 ────────────────────────────────────────────────────
        int globalNegMask = 0;
        int constrainedSet = 0;
        for (int i = 0; i < n; i++) {
            IEquipment it = equipment.get(i);
            items[i] = it;
            int[] r = it.requirements();
            int[] b = it.bonuses();
            reqsRaw[i] = r;
            bonusesRaw[i] = b;
            keep[i] = false;

            int rm = 0, nm = 0;
            int r0 = r[0], r1 = r[1], r2 = r[2], r3 = r[3], r4 = r[4];
            int b0 = b[0], b1 = b[1], b2 = b[2], b3 = b[3], b4 = b[4];
            if (r0 > 0) rm |= 1;
            if (r1 > 0) rm |= 2;
            if (r2 > 0) rm |= 4;
            if (r3 > 0) rm |= 8;
            if (r4 > 0) rm |= 16;
            if (b0 < 0) nm |= 1;
            if (b1 < 0) nm |= 2;
            if (b2 < 0) nm |= 4;
            if (b3 < 0) nm |= 8;
            if (b4 < 0) nm |= 16;
            reqMask[i] = rm;
            negMask[i] = nm;
            globalNegMask |= nm;

            if ((rm | nm) == 0) {
                keep[i] = true;
                sp0 += b0; sp1 += b1; sp2 += b2; sp3 += b3; sp4 += b4;
            } else {
                constrainedSet |= 1 << i;
            }
        }

        if (constrainedSet == 0) {
            return finishResult(items, bonusesRaw, keep, player, n);
        }

        // ── Phase 2: scalar greedy fixed-point with cascade re-check ──
        int activeMask = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            int m = constrainedSet & ~activeMask;
            while (m != 0) {
                int i = Integer.numberOfTrailingZeros(m);
                int bit = 1 << i;
                m &= m - 1;

                int[] r = reqsRaw[i];
                // r[s] > 0 guards: req of 0 must pass even when sp is negative.
                if ((r[0] > 0 && r[0] > sp0) ||
                    (r[1] > 0 && r[1] > sp1) ||
                    (r[2] > 0 && r[2] > sp2) ||
                    (r[3] > 0 && r[3] > sp3) ||
                    (r[4] > 0 && r[4] > sp4)) continue;

                int[] b = bonusesRaw[i];
                int nm = negMask[i];
                int newSp0 = sp0 + b[0], newSp1 = sp1 + b[1], newSp2 = sp2 + b[2],
                    newSp3 = sp3 + b[3], newSp4 = sp4 + b[4];

                if (nm != 0) {
                    // Cascade re-check, only on attrs that subtracted SP.
                    boolean ok = true;
                    int active = activeMask;
                    while (active != 0) {
                        int x = Integer.numberOfTrailingZeros(active);
                        active &= active - 1;
                        int rxm = reqMask[x];
                        if ((rxm & nm) == 0) continue;
                        int[] rx = reqsRaw[x];
                        int[] bx = bonusesRaw[x];
                        if ((nm & 1) != 0 && (rxm & 1) != 0 && rx[0] + bx[0] > newSp0) { ok = false; break; }
                        if ((nm & 2) != 0 && (rxm & 2) != 0 && rx[1] + bx[1] > newSp1) { ok = false; break; }
                        if ((nm & 4) != 0 && (rxm & 4) != 0 && rx[2] + bx[2] > newSp2) { ok = false; break; }
                        if ((nm & 8) != 0 && (rxm & 8) != 0 && rx[3] + bx[3] > newSp3) { ok = false; break; }
                        if ((nm & 16) != 0 && (rxm & 16) != 0 && rx[4] + bx[4] > newSp4) { ok = false; break; }
                    }
                    if (!ok) continue;
                }

                sp0 = newSp0; sp1 = newSp1; sp2 = newSp2; sp3 = newSp3; sp4 = newSp4;
                activeMask |= bit;
                changed = true;
            }
        }

        if (activeMask == constrainedSet) {
            // Greedy activated everything.
            int m2 = activeMask;
            while (m2 != 0) {
                int i = Integer.numberOfTrailingZeros(m2);
                m2 &= m2 - 1;
                keep[i] = true;
            }
            return finishResult(items, bonusesRaw, keep, player, n);
        }

        if (globalNegMask == 0) {
            // Provably optimal — sp can't grow further.
            int m2 = activeMask;
            while (m2 != 0) {
                int i = Integer.numberOfTrailingZeros(m2);
                m2 &= m2 - 1;
                keep[i] = true;
            }
            return finishResult(items, bonusesRaw, keep, player, n);
        }

        // ── Phase 3: SWAR bitmask-DP BFS ──────────────────────────────
        // Pack constrained items (NOT free items). Re-derive base sp by undoing
        // the greedy's bonus contributions back to (assigned + free) — that's the BFS root.
        int greedyMask = activeMask;
        int greedyCount = Integer.bitCount(greedyMask);
        int greedyWeight = 0;
        {
            int m2 = greedyMask;
            while (m2 != 0) {
                int i = Integer.numberOfTrailingZeros(m2);
                m2 &= m2 - 1;
                int[] b = bonusesRaw[i];
                sp0 -= b[0]; sp1 -= b[1]; sp2 -= b[2]; sp3 -= b[3]; sp4 -= b[4];
                greedyWeight += b[0] + b[1] + b[2] + b[3] + b[4];
            }
        }

        long[] pReq = this.pReq;
        long[] pBon = this.pBon;
        long[] pNeed = this.pNeed;
        int[] remIdx = this.remIdx;
        int[] bonusSum = this.bonusSum;
        boolean[] hasNeg = this.hasNeg;

        int k = 0;
        int greedyMaskBfs = 0;
        {
            int m2 = constrainedSet;
            while (m2 != 0) {
                int i = Integer.numberOfTrailingZeros(m2);
                m2 &= m2 - 1;
                int[] r = reqsRaw[i];
                int[] b = bonusesRaw[i];
                pReq[k] = packReq(r);
                pBon[k] = pack5(b[0], b[1], b[2], b[3], b[4]);
                pNeed[k] = packNeed(r, b);
                hasNeg[k] = negMask[i] != 0;
                bonusSum[k] = b[0] + b[1] + b[2] + b[3] + b[4];
                remIdx[k] = i;
                if ((greedyMask & (1 << i)) != 0) greedyMaskBfs |= 1 << k;
                k++;
            }
        }

        if (k > MAX_K) {
            // Defensive — keep greedy result.
            int m2 = greedyMask;
            while (m2 != 0) {
                int i = Integer.numberOfTrailingZeros(m2);
                m2 &= m2 - 1;
                keep[i] = true;
            }
            return finishResult(items, bonusesRaw, keep, player, n);
        }

        int totalMasks = 1 << k;
        int words = (totalMasks + 63) >>> 6;

        long[] reach = this.reach;
        long[] skillByMask = this.skillByMask;
        long[] floorByMask = this.floorByMask;
        int[] weightByMask = this.weightByMask;

        for (int w = 0; w < words; w++) reach[w] = 0L;
        reach[0] = 1L;

        skillByMask[0] = pack5(sp0, sp1, sp2, sp3, sp4);
        floorByMask[0] = 0L;
        weightByMask[0] = 0;

        // Precompute max5(pReq[j]) — when curSk satisfies it, no per-item ge5 is needed.
        long globalMaxReq = 0L;
        for (int j = 0; j < k; j++) globalMaxReq = max5(globalMaxReq, pReq[j]);

        int bestMask = greedyMaskBfs;
        int bestCount = greedyCount;
        int bestWeight = greedyWeight;

        int allMask = totalMasks - 1;

        outer:
        for (int w = 0; w < words; w++) {
            int base = w << 6;
            long processed = 0L;
            long bits;
            while ((bits = reach[w] & ~processed) != 0) {
                int pos = Long.numberOfTrailingZeros(bits);
                processed |= 1L << pos;
                int mask = base + pos;

                int count = Integer.bitCount(mask);
                int wt = weightByMask[mask];
                if (count > bestCount || (count == bestCount && wt > bestWeight)) {
                    bestCount = count;
                    bestWeight = wt;
                    bestMask = mask;
                    if (bestCount == k) break outer;
                }

                int absent = allMask & ~mask;
                if (count + Integer.bitCount(absent) < bestCount) continue;

                long curSk = skillByMask[mask];
                long curFloor = floorByMask[mask];
                int curWt = wt;
                boolean allReqsMet = ge5(curSk, globalMaxReq);

                int a = absent;
                while (a != 0) {
                    int j = Integer.numberOfTrailingZeros(a);
                    int jbit = 1 << j;
                    a &= a - 1;

                    int nextMask = mask | jbit;
                    int nw = nextMask >>> 6;
                    long nbit = 1L << (nextMask & 63);
                    if ((reach[nw] & nbit) != 0) continue;

                    if (!allReqsMet && !ge5(curSk, pReq[j])) continue;
                    long nextSk = curSk + pBon[j] - BIAS5;
                    if (hasNeg[j] && mask != 0) {
                        if (!ge5(nextSk, curFloor)) continue;
                    }
                    skillByMask[nextMask] = nextSk;
                    floorByMask[nextMask] = (mask == 0) ? pNeed[j] : max5(curFloor, pNeed[j]);
                    weightByMask[nextMask] = curWt + bonusSum[j];
                    reach[nw] |= nbit;
                }
            }
        }

        int m2 = bestMask;
        while (m2 != 0) {
            int j = Integer.numberOfTrailingZeros(m2);
            m2 &= m2 - 1;
            keep[remIdx[j]] = true;
        }
        return finishResult(items, bonusesRaw, keep, player, n);
    }

    private static Result finishResult(IEquipment[] items, int[][] bonusesRaw, boolean[] keep, WynnPlayer player, int n) {
        List<IEquipment> valid = new ArrayList<>(n);
        List<IEquipment> invalid = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                valid.add(items[i]);
                player.modify(bonusesRaw[i], true);
            } else {
                invalid.add(items[i]);
            }
        }
        return new Result(valid, invalid);
    }

    private static long pack5(int a, int b, int c, int d, int e) {
        return (long) (a + BIAS)
                | ((long) (b + BIAS) << 12)
                | ((long) (c + BIAS) << 24)
                | ((long) (d + BIAS) << 36)
                | ((long) (e + BIAS) << 48);
    }

    private static long packReq(int[] r) {
        return (long) (r[0] != 0 ? r[0] + BIAS : 0)
                | ((long) (r[1] != 0 ? r[1] + BIAS : 0) << 12)
                | ((long) (r[2] != 0 ? r[2] + BIAS : 0) << 24)
                | ((long) (r[3] != 0 ? r[3] + BIAS : 0) << 36)
                | ((long) (r[4] != 0 ? r[4] + BIAS : 0) << 48);
    }

    private static long packNeed(int[] r, int[] b) {
        return (long) (r[0] != 0 ? r[0] + b[0] + BIAS : 0)
                | ((long) (r[1] != 0 ? r[1] + b[1] + BIAS : 0) << 12)
                | ((long) (r[2] != 0 ? r[2] + b[2] + BIAS : 0) << 24)
                | ((long) (r[3] != 0 ? r[3] + b[3] + BIAS : 0) << 36)
                | ((long) (r[4] != 0 ? r[4] + b[4] + BIAS : 0) << 48);
    }

    private static boolean ge5(long sk, long thr) {
        return (((sk | GUARD) - thr) & GUARD) == GUARD;
    }

    private static long max5(long a, long b) {
        long gt = ((a | GUARD) - b) & GUARD;
        long ones = gt >>> 11;
        long mask = gt | (gt - ones);
        return (a & mask) | (b & ~mask);
    }

    @Override
    public void clearCache() { }
}
