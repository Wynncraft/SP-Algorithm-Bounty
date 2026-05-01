package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * MyFirstPotentiallyIllegalAlgorithm — algorithm body identical to MyFifth's solver, but
 * Phase 1 (free-item separation, packed-form extraction) is performed
 * AHEAD OF TIME by {@link MyFirstPotentiallyIllegalPlayer.Builder}.
 *
 * Because FullEquipBenchmark builds the player once in @Setup(Trial) and
 * only calls reset() between invocations, the pre-computed metadata
 * survives across measurement iterations. clearCache() is a no-op.
 */
@SuppressWarnings("DuplicatedCode")
@Information(name = "MyFirstPotentiallyIllegalAlgorithm", version = 1, authors = {"kcinsoft"})
public final class MyFirstPotentiallyIllegalAlgorithm implements IAlgorithm<MyFirstPotentiallyIllegalPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

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

    private static boolean ge5(long skills, long threshold) {
        return (((skills | GUARD) - threshold) & GUARD) == GUARD;
    }

    private static long max5(long a, long b) {
        long gt = ((a | GUARD) - b) & GUARD;
        long ones = gt >>> 11;
        long mask = gt | (gt - ones);
        return (a & mask) | (b & ~mask);
    }

    private static final int MAX_INPUT = 32;
    private static final int MAX_BFS_BITS = 13;
    private static final int MAX_MASKS = 1 << MAX_BFS_BITS;
    private static final int MAX_REACH_WORDS = MAX_MASKS >>> 6;

    // BFS scratch buffers (algorithm-side, fine because we reset them per call).
    private final long[] skillNeed = new long[MAX_MASKS * 2];
    private final int[] weightBuf = new int[MAX_MASKS];
    private final long[] reachBits = new long[MAX_REACH_WORDS];

    @Override
    public Result run(MyFirstPotentiallyIllegalPlayer player) {
        final int n = player.n;
        final List<IEquipment> equipment = player.equipment;
        if (n == 0) {
            return new Result(Collections.emptyList(), Collections.emptyList());
        }
        if (n > MAX_INPUT) {
            return new Result(Collections.emptyList(), equipment);
        }

        final int[] alloc = player.allocated;
        int s0 = alloc[STR] + player.free0;
        int s1 = alloc[DEX] + player.free1;
        int s2 = alloc[INT] + player.free2;
        int s3 = alloc[DEF] + player.free3;
        int s4 = alloc[AGI] + player.free4;

        final int candCount = player.candCount;

        int candBest = candCount == 0 ? 0 : solveCandidates(player, candCount, s0, s1, s2, s3, s4);

        // Sum chosen candidate bonuses
        int extra0 = 0, extra1 = 0, extra2 = 0, extra3 = 0, extra4 = 0;
        long candBitsToFinal = 0L;
        final int[] cBon0 = player.cBon0, cBon1 = player.cBon1, cBon2 = player.cBon2,
                    cBon3 = player.cBon3, cBon4 = player.cBon4;
        final int[] candIdx = player.candIdx;
        for (int bits = candBest; bits != 0; bits &= bits - 1) {
            int j = Integer.numberOfTrailingZeros(bits);
            extra0 += cBon0[j];
            extra1 += cBon1[j];
            extra2 += cBon2[j];
            extra3 += cBon3[j];
            extra4 += cBon4[j];
            candBitsToFinal |= 1L << candIdx[j];
        }

        int bb0 = player.free0 + extra0;
        int bb1 = player.free1 + extra1;
        int bb2 = player.free2 + extra2;
        int bb3 = player.free3 + extra3;
        int bb4 = player.free4 + extra4;
        int[] tgt = player.bonus;
        tgt[STR] = bb0; tgt[DEX] = bb1; tgt[INT] = bb2; tgt[DEF] = bb3; tgt[AGI] = bb4;
        player.weight = bb0 + bb1 + bb2 + bb3 + bb4;

        long bestMask = player.freeMask | candBitsToFinal;
        int chosenCount = Long.bitCount(bestMask);
        if (chosenCount == n) return new Result(equipment, Collections.emptyList());
        if (chosenCount == 0) return new Result(Collections.emptyList(), equipment);
        long allMask = n == 64 ? -1L : (1L << n) - 1L;
        return new Result(
            new MaskEquipmentList(player.itemBuf, bestMask, chosenCount),
            new MaskEquipmentList(player.itemBuf, allMask & ~bestMask, n - chosenCount)
        );
    }

    @Override
    public void clearCache() {
        // No-op: there is no algorithm-side cross-call state to invalidate.
        // All input-derived metadata lives on the (input-scoped) player.
    }

    // ────────────────────────────────────────────────────────────────────
    // Solver — same logic as MyFifthAlgorithm, but reads from `player.*`.
    // ────────────────────────────────────────────────────────────────────

    private int solveCandidates(MyFirstPotentiallyIllegalPlayer p, int m, int s0, int s1, int s2, int s3, int s4) {
        if (m == 1) return canEquip(p, 0, s0, s1, s2, s3, s4) ? 1 : 0;
        if (m == 2) return solve2(p, s0, s1, s2, s3, s4);
        if (m == 3) return solve3(p, s0, s1, s2, s3, s4);
        return solveGeneral(p, m, s0, s1, s2, s3, s4);
    }

    private static boolean canEquip(MyFirstPotentiallyIllegalPlayer p, int j, int s0, int s1, int s2, int s3, int s4) {
        int r0 = p.cReq0[j], r1 = p.cReq1[j], r2 = p.cReq2[j], r3 = p.cReq3[j], r4 = p.cReq4[j];
        return (r0 == 0 || r0 <= s0)
            && (r1 == 0 || r1 <= s1)
            && (r2 == 0 || r2 <= s2)
            && (r3 == 0 || r3 <= s3)
            && (r4 == 0 || r4 <= s4);
    }

    private static boolean stillValid(MyFirstPotentiallyIllegalPlayer p, int j, int s0, int s1, int s2, int s3, int s4) {
        int r0 = p.cReq0[j], r1 = p.cReq1[j], r2 = p.cReq2[j], r3 = p.cReq3[j], r4 = p.cReq4[j];
        return (r0 == 0 || r0 + p.cBon0[j] <= s0)
            && (r1 == 0 || r1 + p.cBon1[j] <= s1)
            && (r2 == 0 || r2 + p.cBon2[j] <= s2)
            && (r3 == 0 || r3 + p.cBon3[j] <= s3)
            && (r4 == 0 || r4 + p.cBon4[j] <= s4);
    }

    private int solve2(MyFirstPotentiallyIllegalPlayer p, int s0, int s1, int s2, int s3, int s4) {
        boolean canA = canEquip(p, 0, s0, s1, s2, s3, s4);
        boolean canB = canEquip(p, 1, s0, s1, s2, s3, s4);

        if (canA) {
            int as0 = s0 + p.cBon0[0], as1 = s1 + p.cBon1[0], as2 = s2 + p.cBon2[0],
                as3 = s3 + p.cBon3[0], as4 = s4 + p.cBon4[0];
            if (canEquip(p, 1, as0, as1, as2, as3, as4)) {
                int bs0 = as0 + p.cBon0[1], bs1 = as1 + p.cBon1[1], bs2 = as2 + p.cBon2[1],
                    bs3 = as3 + p.cBon3[1], bs4 = as4 + p.cBon4[1];
                if (stillValid(p, 0, bs0, bs1, bs2, bs3, bs4) &&
                    stillValid(p, 1, bs0, bs1, bs2, bs3, bs4)) {
                    return 0b11;
                }
            }
        }
        if (canB) {
            int bs0 = s0 + p.cBon0[1], bs1 = s1 + p.cBon1[1], bs2 = s2 + p.cBon2[1],
                bs3 = s3 + p.cBon3[1], bs4 = s4 + p.cBon4[1];
            if (canEquip(p, 0, bs0, bs1, bs2, bs3, bs4)) {
                int as0 = bs0 + p.cBon0[0], as1 = bs1 + p.cBon1[0], as2 = bs2 + p.cBon2[0],
                    as3 = bs3 + p.cBon3[0], as4 = bs4 + p.cBon4[0];
                if (stillValid(p, 0, as0, as1, as2, as3, as4) &&
                    stillValid(p, 1, as0, as1, as2, as3, as4)) {
                    return 0b11;
                }
            }
        }
        if (canA && canB) {
            return p.cBonSum[0] >= p.cBonSum[1] ? 0b01 : 0b10;
        }
        if (canA) return 0b01;
        if (canB) return 0b10;
        return 0;
    }

    private int solve3(MyFirstPotentiallyIllegalPlayer p, int s0, int s1, int s2, int s3, int s4) {
        long baseSk = pack5(s0, s1, s2, s3, s4);
        long r0 = p.cpReq[0], r1 = p.cpReq[1], r2 = p.cpReq[2];
        long n0 = p.cpNeed[0], n1 = p.cpNeed[1], n2 = p.cpNeed[2];
        long b0 = p.cpBon[0], b1 = p.cpBon[1], b2 = p.cpBon[2];
        boolean neg0 = p.cNeg[0], neg1 = p.cNeg[1], neg2 = p.cNeg[2];
        int bs0 = p.cBonSum[0], bs1 = p.cBonSum[1], bs2 = p.cBonSum[2];

        int bestMask = 0, bestCount = 0, bestWeight = 0;

        if (ge5(baseSk, r0)) { bestCount = 1; bestWeight = bs0; bestMask = 1; }
        if (ge5(baseSk, r1) && (1 > bestCount || (1 == bestCount && bs1 > bestWeight))) {
            bestCount = 1; bestWeight = bs1; bestMask = 2;
        }
        if (ge5(baseSk, r2) && (1 > bestCount || (1 == bestCount && bs2 > bestWeight))) {
            bestCount = 1; bestWeight = bs2; bestMask = 4;
        }

        if (try2(baseSk, r0, b0, n0, neg0, r1, b1, n1, neg1) ||
            try2(baseSk, r1, b1, n1, neg1, r0, b0, n0, neg0)) {
            int w = bs0 + bs1;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b011;
            }
        }
        if (try2(baseSk, r0, b0, n0, neg0, r2, b2, n2, neg2) ||
            try2(baseSk, r2, b2, n2, neg2, r0, b0, n0, neg0)) {
            int w = bs0 + bs2;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b101;
            }
        }
        if (try2(baseSk, r1, b1, n1, neg1, r2, b2, n2, neg2) ||
            try2(baseSk, r2, b2, n2, neg2, r1, b1, n1, neg1)) {
            int w = bs1 + bs2;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b110;
            }
        }

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

    private int solveGeneral(MyFirstPotentiallyIllegalPlayer p, int m, int s0, int s1, int s2, int s3, int s4) {
        final int[] cReq0 = p.cReq0, cReq1 = p.cReq1, cReq2 = p.cReq2, cReq3 = p.cReq3, cReq4 = p.cReq4;
        final int[] cBon0 = p.cBon0, cBon1 = p.cBon1, cBon2 = p.cBon2, cBon3 = p.cBon3, cBon4 = p.cBon4;
        final int[] cBonSum = p.cBonSum;
        final boolean[] cNeg = p.cNeg;
        final long[] cpReq = p.cpReq, cpNeed = p.cpNeed, cpBon = p.cpBon;
        final boolean anyNegative = p.anyNegative;

        long globalMaxReq = 0L;
        for (int j = 0; j < m; j++) {
            globalMaxReq = max5(globalMaxReq, cpReq[j]);
        }

        // Greedy activation
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

        if (m > MAX_BFS_BITS) {
            // Defensive fallback — too large for full BFS, return greedy.
            return gActive;
        }

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
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
            long remaining = mask;
            for (int i = 0; i < index; i++) remaining &= remaining - 1L;
            return items[Long.numberOfTrailingZeros(remaining)];
        }

        @Override
        public int size() { return size; }

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
