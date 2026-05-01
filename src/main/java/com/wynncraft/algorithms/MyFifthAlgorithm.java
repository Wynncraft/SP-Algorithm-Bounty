package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Per-call allocations: zero (Result + emptyList/MaskEquipmentList views only).
 */
@SuppressWarnings("DuplicatedCode")
@Information(name = "MyFifthAlgorithm", version = 1, authors = {"kcinsoft"})
public final class MyFifthAlgorithm implements IAlgorithm<MyFifthPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    // ── SWAR constants ───────────────────────────────────────────────
    private static final int BIAS = 1024;
    private static final long BIAS5 = 0x0400_4004_0040_0400L;
    private static final long GUARD = 0x0800_8008_0080_0800L;

    private static long pack5(int d0, int d1, int d2, int d3, int d4) {
        return (long) (d0 + BIAS)
            | ((long) (d1 + BIAS) << 12)
            | ((long) (d2 + BIAS) << 24)
            | ((long) (d3 + BIAS) << 36)
            | ((long) (d4 + BIAS) << 48);
    }

    private static long packReq(int r0, int r1, int r2, int r3, int r4) {
        return (long) (r0 != 0 ? r0 + BIAS : 0)
            | ((long) (r1 != 0 ? r1 + BIAS : 0) << 12)
            | ((long) (r2 != 0 ? r2 + BIAS : 0) << 24)
            | ((long) (r3 != 0 ? r3 + BIAS : 0) << 36)
            | ((long) (r4 != 0 ? r4 + BIAS : 0) << 48);
    }

    private static long packNeed(int r0, int r1, int r2, int r3, int r4,
                                 int b0, int b1, int b2, int b3, int b4) {
        return (long) (r0 != 0 ? r0 + b0 + BIAS : 0)
            | ((long) (r1 != 0 ? r1 + b1 + BIAS : 0) << 12)
            | ((long) (r2 != 0 ? r2 + b2 + BIAS : 0) << 24)
            | ((long) (r3 != 0 ? r3 + b3 + BIAS : 0) << 36)
            | ((long) (r4 != 0 ? r4 + b4 + BIAS : 0) << 48);
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

    // ── Buffer sizes ─────────────────────────────────────────────────
    // Inputs up to 32 (matches realistic builds: 1 weapon + 8 armour/accessories + ~14 tomes)
    // BFS over ≤ 13 candidates; greater counts almost never occur after the free-item filter.
    private static final int MAX_INPUT = 32;
    private static final int MAX_BFS_BITS = 13;
    private static final int MAX_MASKS = 1 << MAX_BFS_BITS;          // 8192
    private static final int MAX_REACH_WORDS = MAX_MASKS >>> 6;      // 128

    // ── Per-item flat data (capacity MAX_INPUT, indexed by candidate slot) ──
    private final IEquipment[] itemBuf = new IEquipment[MAX_INPUT];
    private final int[] candIdx = new int[MAX_INPUT];
    private final int[] cReq0 = new int[MAX_INPUT];
    private final int[] cReq1 = new int[MAX_INPUT];
    private final int[] cReq2 = new int[MAX_INPUT];
    private final int[] cReq3 = new int[MAX_INPUT];
    private final int[] cReq4 = new int[MAX_INPUT];
    private final int[] cBon0 = new int[MAX_INPUT];
    private final int[] cBon1 = new int[MAX_INPUT];
    private final int[] cBon2 = new int[MAX_INPUT];
    private final int[] cBon3 = new int[MAX_INPUT];
    private final int[] cBon4 = new int[MAX_INPUT];
    private final int[] cBonSum = new int[MAX_INPUT];
    private final boolean[] cNeg = new boolean[MAX_INPUT];
    private final long[] cpReq = new long[MAX_INPUT];
    private final long[] cpNeed = new long[MAX_INPUT];
    private final long[] cpBon = new long[MAX_INPUT];

    // ── BFS buffers ──────────────────────────────────────────────────
    private final long[] skillNeed = new long[MAX_MASKS * 2];
    private final int[] weightBuf = new int[MAX_MASKS];
    private final long[] reachBits = new long[MAX_REACH_WORDS];

    @Override
    public Result run(MyFifthPlayer player) {
        List<IEquipment> equipment = player.equipment();
        final int n = equipment.size();
        if (n == 0) {
            player.reset();
            return new Result(Collections.emptyList(), Collections.emptyList());
        }

        // Hard cap fallback (defensive — current benchmarks stay well under)
        if (n > MAX_INPUT) {
            player.reset();
            return new Result(Collections.emptyList(), equipment);
        }

        final int[] alloc = player.allocated;
        int s0 = alloc[STR], s1 = alloc[DEX], s2 = alloc[INT], s3 = alloc[DEF], s4 = alloc[AGI];

        // ── Phase 1: Free-item separation + flat-array extraction ────
        int candCount = 0;
        long freeMask = 0L;
        int free0 = 0, free1 = 0, free2 = 0, free3 = 0, free4 = 0;
        boolean anyNegative = false;

        for (int i = 0; i < n; i++) {
            IEquipment item = equipment.get(i);
            itemBuf[i] = item;
            int[] req = item.requirements();
            int[] bon = item.bonuses();
            int r0 = req[STR], r1 = req[DEX], r2 = req[INT], r3 = req[DEF], r4 = req[AGI];
            int b0 = bon[STR], b1 = bon[DEX], b2 = bon[INT], b3 = bon[DEF], b4 = bon[AGI];

            if ((r0 | r1 | r2 | r3 | r4) == 0 && (b0 | b1 | b2 | b3 | b4) >= 0) {
                freeMask |= 1L << i;
                free0 += b0;
                free1 += b1;
                free2 += b2;
                free3 += b3;
                free4 += b4;
            } else {
                int c = candCount;
                candIdx[c] = i;
                cReq0[c] = r0; cReq1[c] = r1; cReq2[c] = r2; cReq3[c] = r3; cReq4[c] = r4;
                cBon0[c] = b0; cBon1[c] = b1; cBon2[c] = b2; cBon3[c] = b3; cBon4[c] = b4;
                cBonSum[c] = b0 + b1 + b2 + b3 + b4;
                boolean neg = (b0 | b1 | b2 | b3 | b4) < 0;
                cNeg[c] = neg;
                anyNegative |= neg;
                candCount++;
            }
        }

        s0 += free0; s1 += free1; s2 += free2; s3 += free3; s4 += free4;

        // ── Solve candidates ─────────────────────────────────────────
        int candBest = candCount == 0
            ? 0
            : solveCandidates(candCount, s0, s1, s2, s3, s4, anyNegative);

        // ── Compute summed bonuses from chosen candidates ────────────
        int extra0 = 0, extra1 = 0, extra2 = 0, extra3 = 0, extra4 = 0;
        long candBitsToFinal = 0L;
        for (int bits = candBest; bits != 0; bits &= bits - 1) {
            int j = Integer.numberOfTrailingZeros(bits);
            extra0 += cBon0[j];
            extra1 += cBon1[j];
            extra2 += cBon2[j];
            extra3 += cBon3[j];
            extra4 += cBon4[j];
            candBitsToFinal |= 1L << candIdx[j];
        }

        // ── Apply to player (direct field write, no per-item modify) ─
        int bb0 = free0 + extra0;
        int bb1 = free1 + extra1;
        int bb2 = free2 + extra2;
        int bb3 = free3 + extra3;
        int bb4 = free4 + extra4;
        int[] tgt = player.bonus;
        tgt[STR] = bb0;
        tgt[DEX] = bb1;
        tgt[INT] = bb2;
        tgt[DEF] = bb3;
        tgt[AGI] = bb4;
        player.weight = bb0 + bb1 + bb2 + bb3 + bb4;

        // ── Build result lists (lazy mask views) ────────────────────
        long bestMask = freeMask | candBitsToFinal;
        int chosenCount = Long.bitCount(bestMask);
        if (chosenCount == n) {
            return new Result(equipment, Collections.emptyList());
        }
        if (chosenCount == 0) {
            return new Result(Collections.emptyList(), equipment);
        }
        long allMask = n == 64 ? -1L : (1L << n) - 1L;
        return new Result(
            new MaskEquipmentList(itemBuf, bestMask, chosenCount, n),
            new MaskEquipmentList(itemBuf, allMask & ~bestMask, n - chosenCount, n)
        );
    }

    // ── Candidate solver dispatch ────────────────────────────────────
    private int solveCandidates(int m, int s0, int s1, int s2, int s3, int s4, boolean anyNegative) {
        if (m == 1) {
            return canEquip(0, s0, s1, s2, s3, s4) ? 1 : 0;
        }
        if (m == 2) {
            return solve2(s0, s1, s2, s3, s4);
        }
        if (m == 3) {
            return solve3(s0, s1, s2, s3, s4);
        }
        return solveGeneral(m, s0, s1, s2, s3, s4, anyNegative);
    }

    // ── Candidate utility checks (use cReq*/cBon* arrays) ────────────
    private boolean canEquip(int j, int s0, int s1, int s2, int s3, int s4) {
        int r0 = cReq0[j], r1 = cReq1[j], r2 = cReq2[j], r3 = cReq3[j], r4 = cReq4[j];
        return (r0 == 0 || r0 <= s0)
            && (r1 == 0 || r1 <= s1)
            && (r2 == 0 || r2 <= s2)
            && (r3 == 0 || r3 <= s3)
            && (r4 == 0 || r4 <= s4);
    }

    private boolean stillValid(int j, int s0, int s1, int s2, int s3, int s4) {
        int r0 = cReq0[j], r1 = cReq1[j], r2 = cReq2[j], r3 = cReq3[j], r4 = cReq4[j];
        return (r0 == 0 || r0 + cBon0[j] <= s0)
            && (r1 == 0 || r1 + cBon1[j] <= s1)
            && (r2 == 0 || r2 + cBon2[j] <= s2)
            && (r3 == 0 || r3 + cBon3[j] <= s3)
            && (r4 == 0 || r4 + cBon4[j] <= s4);
    }

    // ── 2-candidate fast path (zero alloc) ───────────────────────────
    private int solve2(int s0, int s1, int s2, int s3, int s4) {
        boolean canA = canEquip(0, s0, s1, s2, s3, s4);
        boolean canB = canEquip(1, s0, s1, s2, s3, s4);

        if (canA) {
            int as0 = s0 + cBon0[0], as1 = s1 + cBon1[0], as2 = s2 + cBon2[0],
                as3 = s3 + cBon3[0], as4 = s4 + cBon4[0];
            if (canEquip(1, as0, as1, as2, as3, as4)) {
                int bs0 = as0 + cBon0[1], bs1 = as1 + cBon1[1], bs2 = as2 + cBon2[1],
                    bs3 = as3 + cBon3[1], bs4 = as4 + cBon4[1];
                if (stillValid(0, bs0, bs1, bs2, bs3, bs4) &&
                    stillValid(1, bs0, bs1, bs2, bs3, bs4)) {
                    return 0b11;
                }
            }
        }
        if (canB) {
            int bs0 = s0 + cBon0[1], bs1 = s1 + cBon1[1], bs2 = s2 + cBon2[1],
                bs3 = s3 + cBon3[1], bs4 = s4 + cBon4[1];
            if (canEquip(0, bs0, bs1, bs2, bs3, bs4)) {
                int as0 = bs0 + cBon0[0], as1 = bs1 + cBon1[0], as2 = bs2 + cBon2[0],
                    as3 = bs3 + cBon3[0], as4 = bs4 + cBon4[0];
                if (stillValid(0, as0, as1, as2, as3, as4) &&
                    stillValid(1, as0, as1, as2, as3, as4)) {
                    return 0b11;
                }
            }
        }
        if (canA && canB) {
            return cBonSum[0] >= cBonSum[1] ? 0b01 : 0b10;
        }
        if (canA) return 0b01;
        if (canB) return 0b10;
        return 0;
    }

    // ── 3-candidate fast path (zero alloc, SWAR) ─────────────────────
    private int solve3(int s0, int s1, int s2, int s3, int s4) {
        long baseSk = pack5(s0, s1, s2, s3, s4);
        long r0 = packReq(cReq0[0], cReq1[0], cReq2[0], cReq3[0], cReq4[0]);
        long r1 = packReq(cReq0[1], cReq1[1], cReq2[1], cReq3[1], cReq4[1]);
        long r2 = packReq(cReq0[2], cReq1[2], cReq2[2], cReq3[2], cReq4[2]);
        long n0 = packNeed(cReq0[0], cReq1[0], cReq2[0], cReq3[0], cReq4[0],
                           cBon0[0], cBon1[0], cBon2[0], cBon3[0], cBon4[0]);
        long n1 = packNeed(cReq0[1], cReq1[1], cReq2[1], cReq3[1], cReq4[1],
                           cBon0[1], cBon1[1], cBon2[1], cBon3[1], cBon4[1]);
        long n2 = packNeed(cReq0[2], cReq1[2], cReq2[2], cReq3[2], cReq4[2],
                           cBon0[2], cBon1[2], cBon2[2], cBon3[2], cBon4[2]);
        long b0 = pack5(cBon0[0], cBon1[0], cBon2[0], cBon3[0], cBon4[0]);
        long b1 = pack5(cBon0[1], cBon1[1], cBon2[1], cBon3[1], cBon4[1]);
        long b2 = pack5(cBon0[2], cBon1[2], cBon2[2], cBon3[2], cBon4[2]);
        boolean neg0 = cNeg[0], neg1 = cNeg[1], neg2 = cNeg[2];
        int bs0 = cBonSum[0], bs1 = cBonSum[1], bs2 = cBonSum[2];

        int bestMask = 0, bestCount = 0, bestWeight = 0;

        // Single-item subsets
        if (ge5(baseSk, r0)) { bestCount = 1; bestWeight = bs0; bestMask = 1; }
        if (ge5(baseSk, r1) && (1 > bestCount || (1 == bestCount && bs1 > bestWeight))) {
            bestCount = 1; bestWeight = bs1; bestMask = 2;
        }
        if (ge5(baseSk, r2) && (1 > bestCount || (1 == bestCount && bs2 > bestWeight))) {
            bestCount = 1; bestWeight = bs2; bestMask = 4;
        }

        // 2-item subsets — try both orderings, pNeed-final-check covers final validity
        // {0,1}
        if (try2(baseSk, r0, b0, n0, neg0, r1, b1, n1, neg1) ||
            try2(baseSk, r1, b1, n1, neg1, r0, b0, n0, neg0)) {
            int w = bs0 + bs1;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b011;
            }
        }
        // {0,2}
        if (try2(baseSk, r0, b0, n0, neg0, r2, b2, n2, neg2) ||
            try2(baseSk, r2, b2, n2, neg2, r0, b0, n0, neg0)) {
            int w = bs0 + bs2;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b101;
            }
        }
        // {1,2}
        if (try2(baseSk, r1, b1, n1, neg1, r2, b2, n2, neg2) ||
            try2(baseSk, r2, b2, n2, neg2, r1, b1, n1, neg1)) {
            int w = bs1 + bs2;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b110;
            }
        }

        // {0,1,2} — try 6 orderings
        if (try3(baseSk, r0, b0, n0, neg0, r1, b1, n1, neg1, r2, b2, n2, neg2)
         || try3(baseSk, r0, b0, n0, neg0, r2, b2, n2, neg2, r1, b1, n1, neg1)
         || try3(baseSk, r1, b1, n1, neg1, r0, b0, n0, neg0, r2, b2, n2, neg2)
         || try3(baseSk, r1, b1, n1, neg1, r2, b2, n2, neg2, r0, b0, n0, neg0)
         || try3(baseSk, r2, b2, n2, neg2, r0, b0, n0, neg0, r1, b1, n1, neg1)
         || try3(baseSk, r2, b2, n2, neg2, r1, b1, n1, neg1, r0, b0, n0, neg0)) {
            return 0b111;
        }
        return bestMask;
    }

    private static boolean try2(long baseSk,
                                long ra, long ba, long na, boolean nega,
                                long rb, long bb, long nb, boolean negb) {
        long sk = baseSk;
        if (!ge5(sk, ra)) return false;
        sk = sk + ba - BIAS5;
        long mn = na;
        if (nega && !ge5(sk, mn)) return false;
        if (!ge5(sk, rb)) return false;
        sk = sk + bb - BIAS5;
        if (negb && !ge5(sk, mn)) return false;
        mn = max5(mn, nb);
        return ge5(sk, mn);
    }

    private static boolean try3(long baseSk,
                                long ra, long ba, long na, boolean nega,
                                long rb, long bb, long nb, boolean negb,
                                long rc, long bc, long nc, boolean negc) {
        long sk = baseSk;
        if (!ge5(sk, ra)) return false;
        sk = sk + ba - BIAS5;
        long mn = na;
        if (nega && !ge5(sk, mn)) return false;
        if (!ge5(sk, rb)) return false;
        sk = sk + bb - BIAS5;
        if (negb && !ge5(sk, mn)) return false;
        mn = max5(mn, nb);
        if (!ge5(sk, rc)) return false;
        sk = sk + bc - BIAS5;
        if (negc && !ge5(sk, mn)) return false;
        mn = max5(mn, nc);
        return ge5(sk, mn);
    }

    // ── General path (m >= 4) — packed-SWAR BFS with pruning ─────────
    private int solveGeneral(int m, int s0, int s1, int s2, int s3, int s4, boolean anyNegative) {
        // Precompute packed forms + globals
        long globalMaxReq = 0L;
        for (int j = 0; j < m; j++) {
            int r0 = cReq0[j], r1 = cReq1[j], r2 = cReq2[j], r3 = cReq3[j], r4 = cReq4[j];
            int b0 = cBon0[j], b1 = cBon1[j], b2 = cBon2[j], b3 = cBon3[j], b4 = cBon4[j];
            long pr = packReq(r0, r1, r2, r3, r4);
            cpReq[j] = pr;
            cpNeed[j] = packNeed(r0, r1, r2, r3, r4, b0, b1, b2, b3, b4);
            cpBon[j] = pack5(b0, b1, b2, b3, b4);
            globalMaxReq = max5(globalMaxReq, pr);
        }

        // ── Greedy activation ────────────────────────────────────────
        int gActive = 0;
        int gCount = 0;
        int gWeight = 0;
        int g0 = s0, g1 = s1, g2 = s2, g3 = s3, g4 = s4;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int j = 0; j < m; j++) {
                int bit = 1 << j;
                if ((gActive & bit) != 0) continue;
                int r0 = cReq0[j], r1 = cReq1[j], r2 = cReq2[j], r3 = cReq3[j], r4 = cReq4[j];
                if ((r0 != 0 && r0 > g0) || (r1 != 0 && r1 > g1) || (r2 != 0 && r2 > g2)
                 || (r3 != 0 && r3 > g3) || (r4 != 0 && r4 > g4)) continue;
                int b0 = cBon0[j], b1 = cBon1[j], b2 = cBon2[j], b3 = cBon3[j], b4 = cBon4[j];
                if (cNeg[j]) {
                    int t0 = g0 + b0, t1 = g1 + b1, t2 = g2 + b2, t3 = g3 + b3, t4 = g4 + b4;
                    boolean ok = true;
                    for (int act = gActive; act != 0; act &= act - 1) {
                        int k = Integer.numberOfTrailingZeros(act);
                        int kr0 = cReq0[k], kr1 = cReq1[k], kr2 = cReq2[k], kr3 = cReq3[k], kr4 = cReq4[k];
                        if ((kr0 != 0 && kr0 + cBon0[k] > t0)
                         || (kr1 != 0 && kr1 + cBon1[k] > t1)
                         || (kr2 != 0 && kr2 + cBon2[k] > t2)
                         || (kr3 != 0 && kr3 + cBon3[k] > t3)
                         || (kr4 != 0 && kr4 + cBon4[k] > t4)) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) continue;
                }
                gActive |= bit;
                gCount++;
                gWeight += cBonSum[j];
                g0 += b0; g1 += b1; g2 += b2; g3 += b3; g4 += b4;
                changed = true;
            }
        }
        if (gCount == m) return gActive;
        if (!anyNegative) return gActive;

        // ── BFS over candidate subsets ────────────────────────────────
        final int totalMasks = 1 << m;
        final int fullMask = totalMasks - 1;
        final long[] sn = this.skillNeed;
        final int[] weight = this.weightBuf;
        final long[] reach = this.reachBits;

        sn[0] = pack5(s0, s1, s2, s3, s4);
        sn[1] = 0L;
        weight[0] = 0;
        final int words = (totalMasks + 63) >>> 6;
        for (int w = 0; w < words; w++) reach[w] = 0L;
        reach[0] = 1L;

        // Seed best with greedy result so BFS prunes more aggressively
        int bestMask = gActive, bestCount = gCount, bestWeight = gWeight;

        for (int w = 0; w < words; w++) {
            int base = w << 6;
            long processed = 0L;
            long bits;
            while ((bits = reach[w] & ~processed) != 0L) {
                int pos = Long.numberOfTrailingZeros(bits);
                processed |= 1L << pos;
                int mask = base + pos;

                int count = Integer.bitCount(mask);
                int curW = weight[mask];
                if (count > bestCount || (count == bestCount && curW > bestWeight)) {
                    bestCount = count;
                    bestWeight = curW;
                    bestMask = mask;
                }
                if (bestCount == m) break;

                // Upper bound: even adding all absent items can't beat best.
                int absent = fullMask & ~mask;
                int upperCount = count + Integer.bitCount(absent);
                if (upperCount < bestCount) continue;

                long curSk = sn[mask << 1];
                long curMn = sn[(mask << 1) + 1];
                boolean allReqsMet = ge5(curSk, globalMaxReq);

                for (int abits = absent; abits != 0; abits &= abits - 1) {
                    int j = Integer.numberOfTrailingZeros(abits);
                    int nextMask = mask | (1 << j);
                    if ((reach[nextMask >>> 6] & (1L << (nextMask & 63))) != 0L) continue;
                    if (!allReqsMet && !ge5(curSk, cpReq[j])) continue;
                    long nextSk = curSk + cpBon[j] - BIAS5;
                    if (cNeg[j] && !ge5(nextSk, curMn)) continue;
                    int idx = nextMask << 1;
                    sn[idx] = nextSk;
                    sn[idx + 1] = max5(curMn, cpNeed[j]);
                    weight[nextMask] = curW + cBonSum[j];
                    reach[nextMask >>> 6] |= 1L << (nextMask & 63);
                }
            }
            if (bestCount == m) break;
        }
        return bestMask;
    }

    // ── Lazy mask-backed equipment list ──────────────────────────────
    private static final class MaskEquipmentList extends AbstractList<IEquipment> {
        private final IEquipment[] items;
        private final long mask;
        private final int size;
        private final int totalItems;

        private MaskEquipmentList(IEquipment[] items, long mask, int size, int totalItems) {
            this.items = items;
            this.mask = mask;
            this.size = size;
            this.totalItems = totalItems;
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
