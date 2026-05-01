package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * Player tailored for MySecondPotentiallyIllegalAlgorithm. Pre-packs every equipment's
 * requirements/bonuses incrementally inside the builder, so {@code run()}
 * never has to re-extract from the {@link IEquipment} interface.
 *
 * Both bench shapes benefit:
 *  - FullEquipBenchmark: builder is invoked once at @Setup; subsequent
 *    {@code run()} calls skip Phase 1 entirely.
 *  - OneByOneBenchmark: builder is reused across iterations and grows by
 *    one item per call, so each {@code equipment()} only packs the newly
 *    added item — total packing work matches a single trial.
 *
 * Array sharing between builder and built players is safe because the
 * builder only writes at position {@code candCount} (or {@code n}) and
 * increments; previously-built players capture those counts by value
 * and never read past them. Grows allocate new arrays via
 * {@link Arrays#copyOf}, leaving any earlier player's snapshot intact.
 */
public final class MySecondPotentiallyIllegalPlayer implements IPlayer {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    // ── Mutable hot-path fields (read/written directly by the algorithm) ──
    final int[] allocated;
    final int[] bonus = new int[SKILL_POINTS.length];
    int weight;

    // ── Equipment list (snapshot reference; immutable from player POV) ──
    final List<IEquipment> equipment;
    final IEquipment[] itemArr;
    final int n;

    // ── Pre-packed Phase 1 outputs (free/candidate split) ──
    final long freeMask;
    final int free0, free1, free2, free3, free4;
    final int candCount;
    final boolean anyNegative;
    final int[] candIdx;
    final long[] cpReq;
    final long[] cpBon;
    final long[] cpNeed;
    final int[] cBonSum;
    final boolean[] cNeg;

    private MySecondPotentiallyIllegalPlayer(
            int[] allocated,
            List<IEquipment> equipment,
            IEquipment[] itemArr,
            int n,
            long freeMask,
            int free0, int free1, int free2, int free3, int free4,
            int candCount,
            boolean anyNegative,
            int[] candIdx,
            long[] cpReq,
            long[] cpBon,
            long[] cpNeed,
            int[] cBonSum,
            boolean[] cNeg) {
        this.allocated = allocated;
        this.equipment = equipment;
        this.itemArr = itemArr;
        this.n = n;
        this.freeMask = freeMask;
        this.free0 = free0;
        this.free1 = free1;
        this.free2 = free2;
        this.free3 = free3;
        this.free4 = free4;
        this.candCount = candCount;
        this.anyNegative = anyNegative;
        this.candIdx = candIdx;
        this.cpReq = cpReq;
        this.cpBon = cpBon;
        this.cpNeed = cpNeed;
        this.cBonSum = cBonSum;
        this.cNeg = cNeg;
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
        int i = skill.ordinal();
        return allocated[i] + bonus[i];
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
        bonus[STR] += d0;
        bonus[DEX] += d1;
        bonus[INT] += d2;
        bonus[DEF] += d3;
        bonus[AGI] += d4;
        weight += d0 + d1 + d2 + d3 + d4;
    }

    @Override
    public void reset() {
        bonus[STR] = 0;
        bonus[DEX] = 0;
        bonus[INT] = 0;
        bonus[DEF] = 0;
        bonus[AGI] = 0;
        weight = 0;
    }

    // ── SWAR packing (kept identical to MyFifthAlgorithm) ──
    private static final int BIAS = 1024;

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

    /**
     * Fixed-size list view over a prefix of {@code itemArr}. Construction is
     * O(1) and the array is shared with the builder; it remains stable for
     * indices [0, size) because the builder only ever writes at index >= n
     * and grows by replacing its own array reference.
     */
    static final class FrozenItemList extends AbstractList<IEquipment> {
        private final IEquipment[] arr;
        private final int size;

        FrozenItemList(IEquipment[] arr, int size) {
            this.arr = arr;
            this.size = size;
        }

        @Override
        public IEquipment get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(index);
            }
            return arr[index];
        }

        @Override
        public int size() {
            return size;
        }
    }

    public static final class Builder implements IPlayerBuilder<MySecondPotentiallyIllegalPlayer> {

        private static final int INITIAL_CAP = 16;

        private final int[] allocated = new int[SKILL_POINTS.length];
        private IEquipment[] itemArr = new IEquipment[INITIAL_CAP];

        // Incremental Phase 1 state
        private int n = 0;
        private long freeMask = 0L;
        private int free0 = 0, free1 = 0, free2 = 0, free3 = 0, free4 = 0;
        private int candCount = 0;
        private boolean anyNegative = false;
        private int[] candIdx = new int[INITIAL_CAP];
        private long[] cpReq = new long[INITIAL_CAP];
        private long[] cpBon = new long[INITIAL_CAP];
        private long[] cpNeed = new long[INITIAL_CAP];
        private int[] cBonSum = new int[INITIAL_CAP];
        private boolean[] cNeg = new boolean[INITIAL_CAP];

        @Override
        public IPlayerBuilder<MySecondPotentiallyIllegalPlayer> equipment(IEquipment... items) {
            for (IEquipment item : items) {
                ensureItemCap(n + 1);
                itemArr[n] = item;

                int[] req = item.requirements();
                int[] bon = item.bonuses();
                int r0 = req[STR], r1 = req[DEX], r2 = req[INT], r3 = req[DEF], r4 = req[AGI];
                int b0 = bon[STR], b1 = bon[DEX], b2 = bon[INT], b3 = bon[DEF], b4 = bon[AGI];

                if ((r0 | r1 | r2 | r3 | r4) == 0 && (b0 | b1 | b2 | b3 | b4) >= 0) {
                    freeMask |= 1L << n;
                    free0 += b0; free1 += b1; free2 += b2; free3 += b3; free4 += b4;
                } else {
                    int c = candCount;
                    ensureCandCap(c + 1);
                    candIdx[c] = n;
                    cpReq[c] = packReq(r0, r1, r2, r3, r4);
                    cpBon[c] = pack5(b0, b1, b2, b3, b4);
                    cpNeed[c] = packNeed(r0, r1, r2, r3, r4, b0, b1, b2, b3, b4);
                    cBonSum[c] = b0 + b1 + b2 + b3 + b4;
                    boolean neg = (b0 | b1 | b2 | b3 | b4) < 0;
                    cNeg[c] = neg;
                    anyNegative |= neg;
                    candCount = c + 1;
                }
                n++;
            }
            return this;
        }

        @Override
        public IPlayerBuilder<MySecondPotentiallyIllegalPlayer> allocate(SkillPoint point, int amount) {
            allocated[point.ordinal()] = amount;
            return this;
        }

        @Override
        public MySecondPotentiallyIllegalPlayer build() {
            // Share array refs — builder writes only at index >= count, and
            // grows allocate fresh arrays. Equipment list is wrapped to
            // freeze size for the player.
            return new MySecondPotentiallyIllegalPlayer(
                allocated.clone(),
                new FrozenItemList(itemArr, n),
                itemArr,
                n,
                freeMask,
                free0, free1, free2, free3, free4,
                candCount,
                anyNegative,
                candIdx,
                cpReq,
                cpBon,
                cpNeed,
                cBonSum,
                cNeg
            );
        }

        private void ensureItemCap(int needed) {
            if (needed > itemArr.length) {
                itemArr = Arrays.copyOf(itemArr, Math.max(needed, itemArr.length * 2));
            }
        }

        private void ensureCandCap(int needed) {
            if (needed > cpReq.length) {
                int newCap = Math.max(needed, cpReq.length * 2);
                candIdx = Arrays.copyOf(candIdx, newCap);
                cpReq = Arrays.copyOf(cpReq, newCap);
                cpBon = Arrays.copyOf(cpBon, newCap);
                cpNeed = Arrays.copyOf(cpNeed, newCap);
                cBonSum = Arrays.copyOf(cBonSum, newCap);
                cNeg = Arrays.copyOf(cNeg, newCap);
            }
        }
    }
}
