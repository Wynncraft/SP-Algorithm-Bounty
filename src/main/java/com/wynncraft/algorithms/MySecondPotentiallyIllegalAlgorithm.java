package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Variant of {@link MyFifthAlgorithm} pairing with {@link MySecondPotentiallyIllegalPlayer}.
 *
 * Phase 1 (free/candidate split, SWAR packing of req/bon/need) is performed
 * once incrementally inside {@link MySecondPotentiallyIllegalPlayer.Builder#equipment}, so
 * {@code run()} reads the pre-packed data straight off the player and
 * dispatches into the same solver kernels (m=1/2/3 fast paths + general
 * SWAR BFS) used by MyFifthAlgorithm.
 */
@SuppressWarnings("DuplicatedCode")
@Information(name = "MySecondPotentiallyIllegalAlgorithm", version = 1, authors = {"kcinsoft"})
public final class MySecondPotentiallyIllegalAlgorithm implements IAlgorithm<MySecondPotentiallyIllegalPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    // ── SWAR constants (mirror MyFifthAlgorithm) ────────────────────
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

    // ── BFS buffers (sized like MyFifthAlgorithm) ───────────────────
    private static final int MAX_BFS_BITS = 13;
    private static final int MAX_MASKS = 1 << MAX_BFS_BITS;
    private static final int MAX_REACH_WORDS = MAX_MASKS >>> 6;

    private final long[] skillNeed = new long[MAX_MASKS * 2];
    private final int[] weightBuf = new int[MAX_MASKS];
    private final long[] reachBits = new long[MAX_REACH_WORDS];

    @Override
    public Result run(MySecondPotentiallyIllegalPlayer player) {
        final int n = player.n;
        if (n == 0) {
            player.reset();
            return new Result(Collections.emptyList(), Collections.emptyList());
        }

        final int candCount = player.candCount;
        final int[] alloc = player.allocated;
        int s0 = alloc[STR] + player.free0;
        int s1 = alloc[DEX] + player.free1;
        int s2 = alloc[INT] + player.free2;
        int s3 = alloc[DEF] + player.free3;
        int s4 = alloc[AGI] + player.free4;

        // ── Solve candidates ─────────────────────────────────────────
        int candBest = candCount == 0
            ? 0
            : solveCandidates(player, candCount, s0, s1, s2, s3, s4, player.anyNegative);

        // ── Sum chosen-candidate bonuses (unpack lanes from cpBon) ───
        long candBitsToFinal = 0L;
        int extra0 = 0, extra1 = 0, extra2 = 0, extra3 = 0, extra4 = 0;
        if (candBest != 0) {
            final long[] cpBon = player.cpBon;
            final int[] candIdx = player.candIdx;
            for (int bits = candBest; bits != 0; bits &= bits - 1) {
                int j = Integer.numberOfTrailingZeros(bits);
                long pb = cpBon[j];
                extra0 += (int) (pb & 0xFFFL) - BIAS;
                extra1 += (int) ((pb >>> 12) & 0xFFFL) - BIAS;
                extra2 += (int) ((pb >>> 24) & 0xFFFL) - BIAS;
                extra3 += (int) ((pb >>> 36) & 0xFFFL) - BIAS;
                extra4 += (int) ((pb >>> 48) & 0xFFFL) - BIAS;
                candBitsToFinal |= 1L << candIdx[j];
            }
        }

        // ── Apply to player (direct field write) ─────────────────────
        int bb0 = player.free0 + extra0;
        int bb1 = player.free1 + extra1;
        int bb2 = player.free2 + extra2;
        int bb3 = player.free3 + extra3;
        int bb4 = player.free4 + extra4;
        int[] tgt = player.bonus;
        tgt[STR] = bb0;
        tgt[DEX] = bb1;
        tgt[INT] = bb2;
        tgt[DEF] = bb3;
        tgt[AGI] = bb4;
        player.weight = bb0 + bb1 + bb2 + bb3 + bb4;

        // ── Build result lists ───────────────────────────────────────
        long bestMask = player.freeMask | candBitsToFinal;
        int chosenCount = Long.bitCount(bestMask);
        if (chosenCount == n) {
            return new Result(player.equipment(), Collections.emptyList());
        }
        if (chosenCount == 0) {
            return new Result(Collections.emptyList(), player.equipment());
        }
        long allMask = n == 64 ? -1L : (1L << n) - 1L;
        return new Result(
            new MaskEquipmentList(player.itemArr, bestMask, chosenCount),
            new MaskEquipmentList(player.itemArr, allMask & ~bestMask, n - chosenCount)
        );
    }

    // ── Candidate solver dispatch ────────────────────────────────────
    private int solveCandidates(MySecondPotentiallyIllegalPlayer p, int m,
                                int s0, int s1, int s2, int s3, int s4,
                                boolean anyNegative) {
        if (m == 1) {
            return canEquip(p, 0, s0, s1, s2, s3, s4) ? 1 : 0;
        }
        if (m == 2) {
            return solve2(p, s0, s1, s2, s3, s4);
        }
        if (m == 3) {
            return solve3(p, s0, s1, s2, s3, s4);
        }
        return solveGeneral(p, m, s0, s1, s2, s3, s4, anyNegative);
    }

    private boolean canEquip(MySecondPotentiallyIllegalPlayer p, int j, int s0, int s1, int s2, int s3, int s4) {
        return ge5(pack5(s0, s1, s2, s3, s4), p.cpReq[j]);
    }

    // ── 2-candidate fast path ────────────────────────────────────────
    private int solve2(MySecondPotentiallyIllegalPlayer p, int s0, int s1, int s2, int s3, int s4) {
        long base = pack5(s0, s1, s2, s3, s4);
        long r0 = p.cpReq[0], r1 = p.cpReq[1];
        long b0 = p.cpBon[0], b1 = p.cpBon[1];
        long n0 = p.cpNeed[0], n1 = p.cpNeed[1];
        int bs0 = p.cBonSum[0], bs1 = p.cBonSum[1];

        boolean canA = ge5(base, r0);
        boolean canB = ge5(base, r1);

        // Try {0,1} both orderings
        if (canA) {
            long sk = base + b0 - BIAS5;
            if (ge5(sk, r1)) {
                long sk2 = sk + b1 - BIAS5;
                long mn = max5(n0, n1);
                if (ge5(sk2, mn)) return 0b11;
            }
        }
        if (canB) {
            long sk = base + b1 - BIAS5;
            if (ge5(sk, r0)) {
                long sk2 = sk + b0 - BIAS5;
                long mn = max5(n0, n1);
                if (ge5(sk2, mn)) return 0b11;
            }
        }
        if (canA && canB) {
            return bs0 >= bs1 ? 0b01 : 0b10;
        }
        if (canA) return 0b01;
        if (canB) return 0b10;
        return 0;
    }

    // ── 3-candidate fast path ────────────────────────────────────────
    private int solve3(MySecondPotentiallyIllegalPlayer p, int s0, int s1, int s2, int s3, int s4) {
        long base = pack5(s0, s1, s2, s3, s4);
        long r0 = p.cpReq[0], r1 = p.cpReq[1], r2 = p.cpReq[2];
        long b0 = p.cpBon[0], b1 = p.cpBon[1], b2 = p.cpBon[2];
        long n0 = p.cpNeed[0], n1 = p.cpNeed[1], n2 = p.cpNeed[2];
        boolean neg0 = p.cNeg[0], neg1 = p.cNeg[1], neg2 = p.cNeg[2];
        int bs0 = p.cBonSum[0], bs1 = p.cBonSum[1], bs2 = p.cBonSum[2];

        int bestMask = 0, bestCount = 0, bestWeight = 0;

        // Singles
        if (ge5(base, r0)) { bestCount = 1; bestWeight = bs0; bestMask = 1; }
        if (ge5(base, r1) && (1 > bestCount || (1 == bestCount && bs1 > bestWeight))) {
            bestCount = 1; bestWeight = bs1; bestMask = 2;
        }
        if (ge5(base, r2) && (1 > bestCount || (1 == bestCount && bs2 > bestWeight))) {
            bestCount = 1; bestWeight = bs2; bestMask = 4;
        }

        // Pairs
        if (try2(base, r0, b0, n0, neg0, r1, b1, n1, neg1) ||
            try2(base, r1, b1, n1, neg1, r0, b0, n0, neg0)) {
            int w = bs0 + bs1;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b011;
            }
        }
        if (try2(base, r0, b0, n0, neg0, r2, b2, n2, neg2) ||
            try2(base, r2, b2, n2, neg2, r0, b0, n0, neg0)) {
            int w = bs0 + bs2;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b101;
            }
        }
        if (try2(base, r1, b1, n1, neg1, r2, b2, n2, neg2) ||
            try2(base, r2, b2, n2, neg2, r1, b1, n1, neg1)) {
            int w = bs1 + bs2;
            if (2 > bestCount || (2 == bestCount && w > bestWeight)) {
                bestCount = 2; bestWeight = w; bestMask = 0b110;
            }
        }

        // Triple
        if (try3(base, r0, b0, n0, neg0, r1, b1, n1, neg1, r2, b2, n2, neg2)
         || try3(base, r0, b0, n0, neg0, r2, b2, n2, neg2, r1, b1, n1, neg1)
         || try3(base, r1, b1, n1, neg1, r0, b0, n0, neg0, r2, b2, n2, neg2)
         || try3(base, r1, b1, n1, neg1, r2, b2, n2, neg2, r0, b0, n0, neg0)
         || try3(base, r2, b2, n2, neg2, r0, b0, n0, neg0, r1, b1, n1, neg1)
         || try3(base, r2, b2, n2, neg2, r1, b1, n1, neg1, r0, b0, n0, neg0)) {
            return 0b111;
        }
        return bestMask;
    }

    private static boolean try2(long base,
                                long ra, long ba, long na, boolean nega,
                                long rb, long bb, long nb, boolean negb) {
        long sk = base;
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

    private static boolean try3(long base,
                                long ra, long ba, long na, boolean nega,
                                long rb, long bb, long nb, boolean negb,
                                long rc, long bc, long nc, boolean negc) {
        long sk = base;
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

    // ── General path (m >= 4) — packed-SWAR BFS with greedy seed ─────
    private int solveGeneral(MySecondPotentiallyIllegalPlayer p, int m,
                             int s0, int s1, int s2, int s3, int s4,
                             boolean anyNegative) {
        final long[] cpReq = p.cpReq;
        final long[] cpBon = p.cpBon;
        final long[] cpNeed = p.cpNeed;
        final int[] cBonSum = p.cBonSum;
        final boolean[] cNeg = p.cNeg;

        // Compute global max requirement (used for batch req-met short-circuit)
        long globalMaxReq = 0L;
        for (int j = 0; j < m; j++) {
            globalMaxReq = max5(globalMaxReq, cpReq[j]);
        }

        // ── Greedy activation (using packed forms) ───────────────────
        int gActive = 0;
        int gCount = 0;
        int gWeight = 0;
        long gSk = pack5(s0, s1, s2, s3, s4);
        long gNeed = 0L;
        boolean changed = true;
        while (changed) {
            changed = false;
            int absent = ((1 << m) - 1) & ~gActive;
            for (int abits = absent; abits != 0; abits &= abits - 1) {
                int j = Integer.numberOfTrailingZeros(abits);
                if (!ge5(gSk, cpReq[j])) continue;
                long nextSk = gSk + cpBon[j] - BIAS5;
                if (cNeg[j] && !ge5(nextSk, gNeed)) continue;
                gActive |= 1 << j;
                gCount++;
                gWeight += cBonSum[j];
                gSk = nextSk;
                gNeed = max5(gNeed, cpNeed[j]);
                changed = true;
            }
        }
        if (gCount == m) return gActive;
        if (!anyNegative) return gActive;

        // ── BFS over candidate subsets ───────────────────────────────
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

    // ── Lazy mask-backed equipment list ──────────────────────────────
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
