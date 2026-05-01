package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Player whose Builder pre-computes equipment metadata (packed SWAR forms,
 * free vs candidate partition, per-item bonus sums, negativity flags).
 *
 * Designed for TheSeventhAlgorithm: by the time {@link #run} is called the
 * algorithm only has to do the solver math — extraction is already done.
 *
 * The builder is incremental — each {@code equipment(X)} appends to internal
 * arrays in O(1) and each {@code build()} snapshots a fresh player view that
 * shares those arrays read-only.
 */
@SuppressWarnings("DuplicatedCode")
@Information(name = "MyFirstPotentiallyIllegalPlayer", version = 1, authors = "kcinsoft")
public final class MyFirstPotentiallyIllegalPlayer implements IPlayer {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    static final int BIAS = 1024;
    static final long BIAS5 = 0x0400_4004_0040_0400L;

    static long pack5(int d0, int d1, int d2, int d3, int d4) {
        return (long) (d0 + BIAS)
            | ((long) (d1 + BIAS) << 12)
            | ((long) (d2 + BIAS) << 24)
            | ((long) (d3 + BIAS) << 36)
            | ((long) (d4 + BIAS) << 48);
    }

    static long packReq(int r0, int r1, int r2, int r3, int r4) {
        return (long) (r0 != 0 ? r0 + BIAS : 0)
            | ((long) (r1 != 0 ? r1 + BIAS : 0) << 12)
            | ((long) (r2 != 0 ? r2 + BIAS : 0) << 24)
            | ((long) (r3 != 0 ? r3 + BIAS : 0) << 36)
            | ((long) (r4 != 0 ? r4 + BIAS : 0) << 48);
    }

    static long packNeed(int r0, int r1, int r2, int r3, int r4,
                         int b0, int b1, int b2, int b3, int b4) {
        return (long) (r0 != 0 ? r0 + b0 + BIAS : 0)
            | ((long) (r1 != 0 ? r1 + b1 + BIAS : 0) << 12)
            | ((long) (r2 != 0 ? r2 + b2 + BIAS : 0) << 24)
            | ((long) (r3 != 0 ? r3 + b3 + BIAS : 0) << 36)
            | ((long) (r4 != 0 ? r4 + b4 + BIAS : 0) << 48);
    }

    // ---- Player state visible to the algorithm ----
    final List<IEquipment> equipment;
    final IEquipment[] itemBuf;          // length n, indexed by original equipment index
    final int n;                         // total items

    final int[] allocated;
    final int[] bonus = new int[5];
    int weight;

    // Free items: zero-req, all-nonneg bonuses
    final long freeMask;
    final int free0, free1, free2, free3, free4;

    // Candidates
    final int candCount;
    final int[] candIdx;     // shared with builder; read-only [0, candCount)
    final int[] cReq0, cReq1, cReq2, cReq3, cReq4;
    final int[] cBon0, cBon1, cBon2, cBon3, cBon4;
    final int[] cBonSum;
    final boolean[] cNeg;
    final long[] cpReq, cpNeed, cpBon;
    final boolean anyNegative;

    private MyFirstPotentiallyIllegalPlayer(List<IEquipment> equipment,
                             IEquipment[] itemBuf,
                             int n,
                             int[] allocated,
                             long freeMask,
                             int free0, int free1, int free2, int free3, int free4,
                             int candCount,
                             int[] candIdx,
                             int[] cReq0, int[] cReq1, int[] cReq2, int[] cReq3, int[] cReq4,
                             int[] cBon0, int[] cBon1, int[] cBon2, int[] cBon3, int[] cBon4,
                             int[] cBonSum, boolean[] cNeg,
                             long[] cpReq, long[] cpNeed, long[] cpBon,
                             boolean anyNegative) {
        this.equipment = equipment;
        this.itemBuf = itemBuf;
        this.n = n;
        this.allocated = allocated;
        this.freeMask = freeMask;
        this.free0 = free0; this.free1 = free1; this.free2 = free2; this.free3 = free3; this.free4 = free4;
        this.candCount = candCount;
        this.candIdx = candIdx;
        this.cReq0 = cReq0; this.cReq1 = cReq1; this.cReq2 = cReq2; this.cReq3 = cReq3; this.cReq4 = cReq4;
        this.cBon0 = cBon0; this.cBon1 = cBon1; this.cBon2 = cBon2; this.cBon3 = cBon3; this.cBon4 = cBon4;
        this.cBonSum = cBonSum;
        this.cNeg = cNeg;
        this.cpReq = cpReq; this.cpNeed = cpNeed; this.cpBon = cpBon;
        this.anyNegative = anyNegative;
    }

    @Override
    public List<IEquipment> equipment() {
        return equipment;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public int total(SkillPoint skill) {
        int idx = skill.ordinal();
        return allocated[idx] + bonus[idx];
    }

    @Override
    public int allocated(SkillPoint skill) {
        return allocated[skill.ordinal()];
    }

    @Override
    public void modify(int[] skillPoints, boolean sum) {
        int sign = sum ? 1 : -1;
        int d0 = skillPoints[STR] * sign;
        int d1 = skillPoints[DEX] * sign;
        int d2 = skillPoints[INT] * sign;
        int d3 = skillPoints[DEF] * sign;
        int d4 = skillPoints[AGI] * sign;
        bonus[STR] += d0; bonus[DEX] += d1; bonus[INT] += d2; bonus[DEF] += d3; bonus[AGI] += d4;
        weight += d0 + d1 + d2 + d3 + d4;
    }

    @Override
    public void reset() {
        bonus[STR] = 0; bonus[DEX] = 0; bonus[INT] = 0; bonus[DEF] = 0; bonus[AGI] = 0;
        weight = 0;
    }

    public static final class Builder implements IPlayerBuilder<MyFirstPotentiallyIllegalPlayer> {

        // Builders are short-lived. Allocate generous capacity so the
        // shared arrays NEVER reallocate during a builder's lifetime;
        // that lets multiple snapshot Players safely share them read-only.
        private static final int CAP = 64;

        private final ArrayList<IEquipment> equipment = new ArrayList<>(CAP);
        private final IEquipment[] itemBuf = new IEquipment[CAP];
        private final int[] allocated = new int[5];

        // Running free state
        private long freeMask = 0L;
        private int free0 = 0, free1 = 0, free2 = 0, free3 = 0, free4 = 0;

        // Running candidate state
        private int candCount = 0;
        private final int[] candIdx = new int[CAP];
        private final int[] cReq0 = new int[CAP];
        private final int[] cReq1 = new int[CAP];
        private final int[] cReq2 = new int[CAP];
        private final int[] cReq3 = new int[CAP];
        private final int[] cReq4 = new int[CAP];
        private final int[] cBon0 = new int[CAP];
        private final int[] cBon1 = new int[CAP];
        private final int[] cBon2 = new int[CAP];
        private final int[] cBon3 = new int[CAP];
        private final int[] cBon4 = new int[CAP];
        private final int[] cBonSum = new int[CAP];
        private final boolean[] cNeg = new boolean[CAP];
        private final long[] cpReq = new long[CAP];
        private final long[] cpNeed = new long[CAP];
        private final long[] cpBon = new long[CAP];
        private boolean anyNegative = false;

        private int n = 0;

        @Override
        public IPlayerBuilder<MyFirstPotentiallyIllegalPlayer> equipment(IEquipment... items) {
            for (IEquipment item : items) {
                appendOne(item);
            }
            return this;
        }

        private void appendOne(IEquipment item) {
            int i = n;
            equipment.add(item);
            itemBuf[i] = item;
            int[] req = item.requirements();
            int[] bon = item.bonuses();
            int r0 = req[STR], r1 = req[DEX], r2 = req[INT], r3 = req[DEF], r4 = req[AGI];
            int b0 = bon[STR], b1 = bon[DEX], b2 = bon[INT], b3 = bon[DEF], b4 = bon[AGI];

            if ((r0 | r1 | r2 | r3 | r4) == 0 && (b0 | b1 | b2 | b3 | b4) >= 0) {
                freeMask |= 1L << i;
                free0 += b0; free1 += b1; free2 += b2; free3 += b3; free4 += b4;
            } else {
                int c = candCount;
                candIdx[c] = i;
                cReq0[c] = r0; cReq1[c] = r1; cReq2[c] = r2; cReq3[c] = r3; cReq4[c] = r4;
                cBon0[c] = b0; cBon1[c] = b1; cBon2[c] = b2; cBon3[c] = b3; cBon4[c] = b4;
                cBonSum[c] = b0 + b1 + b2 + b3 + b4;
                boolean neg = (b0 | b1 | b2 | b3 | b4) < 0;
                cNeg[c] = neg;
                anyNegative |= neg;
                cpReq[c] = packReq(r0, r1, r2, r3, r4);
                cpNeed[c] = packNeed(r0, r1, r2, r3, r4, b0, b1, b2, b3, b4);
                cpBon[c] = pack5(b0, b1, b2, b3, b4);
                candCount++;
            }
            n++;
        }

        @Override
        public IPlayerBuilder<MyFirstPotentiallyIllegalPlayer> allocate(SkillPoint point, int amount) {
            allocated[point.ordinal()] = amount;
            return this;
        }

        @Override
        public MyFirstPotentiallyIllegalPlayer build() {
            // Snapshot: share the (sufficiently-large) arrays by reference. Future
            // builder writes go to indices >= current n / candCount only, so the
            // snapshot's read window [0, n) / [0, candCount) stays immutable.
            // The equipment List must be a stable snapshot — copy the prefix.
            List<IEquipment> equipSnap = n == 0
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(equipment));
            return new MyFirstPotentiallyIllegalPlayer(
                equipSnap,
                itemBuf,
                n,
                allocated.clone(),
                freeMask,
                free0, free1, free2, free3, free4,
                candCount,
                candIdx,
                cReq0, cReq1, cReq2, cReq3, cReq4,
                cBon0, cBon1, cBon2, cBon3, cBon4,
                cBonSum, cNeg,
                cpReq, cpNeed, cpBon,
                anyNegative
            );
        }
    }
}
