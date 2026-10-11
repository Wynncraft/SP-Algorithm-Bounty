package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * Preprocessed player snapshot for Cascade Sentinel.
 *
 * <p>The builder stores fast-path metadata in packed or scalar sidecars while
 * retaining original equipment references for result reporting. A built player
 * captures an item/candidate prefix and its ingested numeric values within the
 * packed and bounded scalar solver paths. The general fallback outside the
 * bounded mask domain reads the original equipment values instead.
 * Later builder appends may extend shared backing arrays
 * but never overwrite that published prefix. Reported item references retain identity.</p>
 *
 * <p>Allocation changes after publication fork the Data header, preserving the
 * allocated skill-point state seen by earlier snapshots.</p>
 */
public class CascadeSentinelV5Player extends AbstractList<IEquipment> implements IPlayer {
    static final int NO_NEED = Integer.MIN_VALUE;
    static final int PACK_BIAS = 1024;
    static final long PACK_BIAS_5 = 0x0400_4004_0040_0400L;
    private static final int DIRECT_NEGATIVE_META_SIGN = Integer.MIN_VALUE;
    private static final int NONNEGATIVE_DRIVERS_META = 1 << 13;
    private static final int MONOTONE_NEGATIVE_META = 1 << 14;

    final Data data;
    final int itemCount;
    // candidateMeta encoding: non-negative values are positive-snapshot counts;
    // generic/hardened negative snapshots store ~candidateCount. NegativePlayer
    // sets the sign bit, driverMask in bits 0..8, count in bits 9..12,
    // nonnegative-driver proof in bit 13 and, in bit 14, the proof that
    // negative bonuses affect no skill with a positive candidate requirement.
    final int candidateMeta;
    final long basePacked;

    // A zero solutionState means reset/unmodified. Packed totals use bit 63 as a
    // tag; scalar mutations and hardened results materialize five exact bonuses only when needed.
    private static final long PACKED_SOLUTION = Long.MIN_VALUE;
    private static final long SPILLED_SOLUTION = 1L << 62;
    private long solutionState;
    private int[] solutionSpill;

    private CascadeSentinelV5Player(
            Data data,
            int itemCount,
            int candidateCount,
            boolean anyNegative,
            long basePacked) {
        this.data = data;
        this.itemCount = itemCount;
        this.candidateMeta = anyNegative ? ~candidateCount : candidateCount;
        this.basePacked = basePacked;
    }

    private CascadeSentinelV5Player(
            Data data,
            int itemCount,
            int candidateCount,
            int driverMask,
            long basePacked, boolean nonnegativeDrivers, boolean monotoneNegative) {
        this.data = data;
        this.itemCount = itemCount;
        this.candidateMeta = DIRECT_NEGATIVE_META_SIGN | (candidateCount << 9) | driverMask
                | (nonnegativeDrivers ? NONNEGATIVE_DRIVERS_META : 0)
                | (monotoneNegative ? MONOTONE_NEGATIVE_META : 0);
        this.basePacked = basePacked;
    }

    int candidateCount() {
        if (candidateMeta >= 0) return candidateMeta;
        if (this instanceof NegativePlayer) return (candidateMeta >>> 9) & 0xF;
        return ~candidateMeta;
    }
    boolean anyNegative() {
        return candidateMeta < 0;
    }
    boolean negativeBonusesIrrelevant() {
        return this instanceof NegativePlayer && (candidateMeta & MONOTONE_NEGATIVE_META) != 0;
    }

    @Override public IEquipment get(int index) {
        if (index < 0 || index >= itemCount) throw new IndexOutOfBoundsException(index);
        return data.items[index];
    }
    @Override public int size() { return itemCount; }
    @Override public List<IEquipment> equipment() { return this; }
    @Override public int weight() {
        long state = solutionState;
        if (state == 0L) return 0;
        if (state == SPILLED_SOLUTION) {
            int[] b = solutionSpill;
            return b[0] + b[1] + b[2] + b[3] + b[4];
        }
        long packed = state & ~PACKED_SOLUTION;
        int b0 = lane(packed, 0) - data.alloc0;
        int b1 = lane(packed, 12) - data.alloc1;
        int b2 = lane(packed, 24) - data.alloc2;
        int b3 = lane(packed, 36) - data.alloc3;
        int b4 = lane(packed, 48) - data.alloc4;
        return b0 + b1 + b2 + b3 + b4;
    }
    @Override public int total(SkillPoint skill) {
        long state = solutionState;
        if (state == 0L) {
            return allocated(skill);
        }
        if (state == SPILLED_SOLUTION) {
            int[] b = solutionSpill;
            return switch (skill) {
                case STRENGTH -> data.alloc0 + b[0];
                case DEXTERITY -> data.alloc1 + b[1];
                case INTELLIGENCE -> data.alloc2 + b[2];
                case DEFENCE -> data.alloc3 + b[3];
                case AGILITY -> data.alloc4 + b[4];
            };
        }
        long packed = state & ~PACKED_SOLUTION;
        return switch (skill) {
            case STRENGTH -> lane(packed, 0);
            case DEXTERITY -> lane(packed, 12);
            case INTELLIGENCE -> lane(packed, 24);
            case DEFENCE -> lane(packed, 36);
            case AGILITY -> lane(packed, 48);
        };
    }
    @Override public int allocated(SkillPoint skill) {
        return switch (skill) {
            case STRENGTH -> data.alloc0;
            case DEXTERITY -> data.alloc1;
            case INTELLIGENCE -> data.alloc2;
            case DEFENCE -> data.alloc3;
            case AGILITY -> data.alloc4;
        };
    }
    @Override public void modify(int[] sp, boolean sum) {
        if (sp == null || sp.length != 5) throw new IllegalArgumentException("Expected exactly five skill-point values");
        int[] b = scalarBonuses();
        solutionState = SPILLED_SOLUTION;
        int sign = sum ? 1 : -1;
        b[0] += sign * sp[0]; b[1] += sign * sp[1]; b[2] += sign * sp[2]; b[3] += sign * sp[3]; b[4] += sign * sp[4];
    }
    @Override public void reset() { solutionState = 0L; }
    void setBonus(int b0, int b1, int b2, int b3, int b4) {
        int[] b = solutionSpill;
        if (b == null) solutionSpill = b = new int[5];
        b[0]=b0; b[1]=b1; b[2]=b2; b[3]=b3; b[4]=b4;
        solutionState = SPILLED_SOLUTION;
    }
    void setPackedTotal(long packedTotal) {
        solutionState = packedTotal | PACKED_SOLUTION;
    }
    private int[] scalarBonuses() {
        int[] b = solutionSpill;
        if (b == null) solutionSpill = b = new int[5];
        long state = solutionState;
        if ((state & PACKED_SOLUTION) != 0L) {
            long packed = state & ~PACKED_SOLUTION;
            b[0] = lane(packed, 0) - data.alloc0;
            b[1] = lane(packed, 12) - data.alloc1;
            b[2] = lane(packed, 24) - data.alloc2;
            b[3] = lane(packed, 36) - data.alloc3;
            b[4] = lane(packed, 48) - data.alloc4;
        } else if (state == 0L) {
            b[0]=b[1]=b[2]=b[3]=b[4]=0;
        }
        return b;
    }

    long freeOriginalMask() {
        long prefix = itemCount == 64 ? -1L : (itemCount == 0 ? 0L : (1L << itemCount) - 1L);
        return prefix & ~data.candidateOriginalMask;
    }
    static long pack5(int a,int b,int c,int d,int e) {
        return (long)(a + PACK_BIAS)
            | ((long)(b + PACK_BIAS) << 12)
            | ((long)(c + PACK_BIAS) << 24)
            | ((long)(d + PACK_BIAS) << 36)
            | ((long)(e + PACK_BIAS) << 48);
    }
    static int lane(long packed, int shift) {
        return (int)((packed >>> shift) & 0xFFFL) - PACK_BIAS;
    }

    // Eligible packed candidates have positive requirements in 1..1023.
    // The guard-bit encoding for r<=0 is equivalent to an unconditional zero requirement.
    static int capturedRequirement(long packedReqAdd, int shift) {
        int value = (int)((packedReqAdd >>> shift) & 0xFFFL);
        return (value & 0x800) != 0 ? 0 : PACK_BIAS - value;
    }

    /** Snapshot carrying scalar free-skill totals required by hardened solving. */
    static final class HardenedPlayer extends CascadeSentinelV5Player {
        final int free0, free1, free2, free3, free4;

        HardenedPlayer(Data data, int itemCount, int candidateCount, boolean anyNegative, long basePacked,
                       int free0, int free1, int free2, int free3, int free4) {
            super(data, itemCount, candidateCount, anyNegative, basePacked);
            this.free0 = free0; this.free1 = free1; this.free2 = free2; this.free3 = free3; this.free4 = free4;
        }

        int freeCount() { return itemCount - candidateCount(); }
        int freeScore() { return free0 + free1 + free2 + free3 + free4; }
    }

    /** Packed common-negative snapshot with immutable driver/sink certificate metadata. */
    static final class NegativePlayer extends CascadeSentinelV5Player {
        final long sinkDeltaPacked;
        final long fullNeedPacked;

        NegativePlayer(Data data, int itemCount, int candidateCount, int driverMask, long basePacked,
                       long sinkDeltaPacked, long fullNeedPacked, boolean nonnegativeDrivers, boolean monotoneNegative) {
            super(data, itemCount, candidateCount, driverMask, basePacked, nonnegativeDrivers, monotoneNegative);
            this.sinkDeltaPacked = sinkDeltaPacked;
            this.fullNeedPacked = fullNeedPacked;
        }

        int driverMask() {
            return candidateMeta & 0x1FF;
        }
        boolean nonnegativeDrivers() {
            return (candidateMeta & NONNEGATIVE_DRIVERS_META) != 0;
        }
    }

    static final class HardenedData {
        int[] r0, r1, r2, r3, r4;
        int[] b0, b1, b2, b3, b4;
        int[] p0, p1, p2, p3, p4;

        HardenedData(int capacity) {
            r0=new int[capacity];r1=new int[capacity];r2=new int[capacity];r3=new int[capacity];r4=new int[capacity];
            b0=new int[capacity];b1=new int[capacity];b2=new int[capacity];b3=new int[capacity];b4=new int[capacity];
            p0=new int[capacity];p1=new int[capacity];p2=new int[capacity];p3=new int[capacity];p4=new int[capacity];
        }

        void ensureCapacity(int n) {
            if (n <= r0.length) return;
            int c = Math.max(n, r0.length << 1);
            r0=Arrays.copyOf(r0,c);r1=Arrays.copyOf(r1,c);r2=Arrays.copyOf(r2,c);r3=Arrays.copyOf(r3,c);r4=Arrays.copyOf(r4,c);
            b0=Arrays.copyOf(b0,c);b1=Arrays.copyOf(b1,c);b2=Arrays.copyOf(b2,c);b3=Arrays.copyOf(b3,c);b4=Arrays.copyOf(b4,c);
            p0=Arrays.copyOf(p0,c);p1=Arrays.copyOf(p1,c);p2=Arrays.copyOf(p2,c);p3=Arrays.copyOf(p3,c);p4=Arrays.copyOf(p4,c);
        }

        void set(int i, int q0,int q1,int q2,int q3,int q4, int x0,int x1,int x2,int x3,int x4) {
            r0[i]=q0;r1[i]=q1;r2[i]=q2;r3[i]=q3;r4[i]=q4;
            b0[i]=x0;b1[i]=x1;b2[i]=x2;b3[i]=x3;b4[i]=x4;
            p0[i]=q0>0?q0+x0:NO_NEED;p1[i]=q1>0?q1+x1:NO_NEED;
            p2[i]=q2>0?q2+x2:NO_NEED;p3[i]=q3>0?q3+x3:NO_NEED;p4[i]=q4>0?q4+x4:NO_NEED;
        }
    }

    static final class Data {
        IEquipment[] items;
        int[] originalIndex;
        int[] weight;
        // Static requirement complements: positive r stores PACK_BIAS-r in its
        // 12-bit lane; r<=0 stores the guard bit, making that requirement unconditional.
        // Eligibility is tested with (total + packedReqAdd) & lane guards.
        // packedDelta stores pack5(bonuses) - PACK_BIAS_5 for direct addition.
        long[] packedReqAdd, packedDelta;
        // Packed sustainability thresholds are needed only after a negative candidate
        // appears, so create this sidecar on demand and maintain it thereafter.
        long[] packedNeed;
        HardenedData hardened;
        long candidateOriginalMask;
        int alloc0, alloc1, alloc2, alloc3, alloc4;

        Data() {
            items = new IEquipment[32];
            originalIndex = new int[9];
            weight = new int[9];
            packedReqAdd = new long[9];
            packedDelta = new long[9];
        }

        // Allocation copy-on-write duplicates only this header. Backing arrays remain
        // shared because later builder appends never overwrite a published prefix.
        Data(Data source) {
            items = source.items;
            originalIndex = source.originalIndex;
            weight = source.weight;
            packedReqAdd = source.packedReqAdd;
            packedDelta = source.packedDelta;
            packedNeed = source.packedNeed;
            hardened = source.hardened;
            candidateOriginalMask = source.candidateOriginalMask;
            alloc0 = source.alloc0; alloc1 = source.alloc1; alloc2 = source.alloc2;
            alloc3 = source.alloc3; alloc4 = source.alloc4;
        }

        void ensureItemCapacity(int n) {
            if (n <= items.length) return;
            items = Arrays.copyOf(items, Math.max(n, items.length << 1));
        }
        void ensureCandidateCapacity(int n) {
            if (n <= originalIndex.length) return;
            int c = Math.max(n, originalIndex.length << 1);
            originalIndex=Arrays.copyOf(originalIndex,c);
            weight=Arrays.copyOf(weight,c);
            packedReqAdd=Arrays.copyOf(packedReqAdd,c); packedDelta=Arrays.copyOf(packedDelta,c);
            if (packedNeed != null) packedNeed=Arrays.copyOf(packedNeed,c);
            if (hardened != null) hardened.ensureCapacity(c);
        }
        void ensureHardenedData(int candidateCount) {
            if (hardened != null) {
                hardened.ensureCapacity(candidateCount);
                return;
            }
            hardened = new HardenedData(originalIndex.length);
            for (int i = 0; i < candidateCount; i++) {
                long req = packedReqAdd[i], bonus = packedDelta[i] + PACK_BIAS_5;
                hardened.set(i,
                        capturedRequirement(req,0),capturedRequirement(req,12),capturedRequirement(req,24),
                        capturedRequirement(req,36),capturedRequirement(req,48),
                        lane(bonus,0),lane(bonus,12),lane(bonus,24),lane(bonus,36),lane(bonus,48));
            }
        }

        // Called for previously ingested candidates, never for live equipment arrays.
        long capturedNeed(int i) {
            HardenedData h = hardened;
            if (h != null) return Builder.packNeeds(h.r0[i],h.r1[i],h.r2[i],h.r3[i],h.r4[i],
                    h.b0[i],h.b1[i],h.b2[i],h.b3[i],h.b4[i]);
            long req = packedReqAdd[i], bonus = packedDelta[i] + PACK_BIAS_5;
            return Builder.packNeeds(
                    capturedRequirement(req,0),capturedRequirement(req,12),capturedRequirement(req,24),
                    capturedRequirement(req,36),capturedRequirement(req,48),
                    lane(bonus,0),lane(bonus,12),lane(bonus,24),lane(bonus,36),lane(bonus,48));
        }
    }

    public static final class Builder implements IPlayerBuilder<CascadeSentinelV5Player> {
        private Data data = new Data();
        private int itemCount, candidateCount;
        private int free0, free1, free2, free3, free4;
        private boolean anyNegative;
        private int requiredStatsMask, negativeStatsMask;
        private int neg0,neg1,neg2,neg3,neg4,pos0,pos1,pos2,pos3,pos4;
        private boolean packedValuesSafe = true;
        private boolean dataPublished;
        private IEquipment repeatZeroItem;
        // Positive-only build() finalization depends only on
        // Builder-owned scalar state. Zero-effect appends can therefore reuse the
        // last packed-safety/base derivation. Negative finalization recomputes
        // dense-nine/driver metadata from captured sidecars, never live item values.
        private boolean snapshotMetadataDirty = true;
        private boolean cleanPackedSafe;
        private long cleanPackedBase;

        @Override public IPlayerBuilder<CascadeSentinelV5Player> equipment(IEquipment... additions) {
            if (additions == null) throw new IllegalArgumentException("Equipment batch must exist");
            int n = additions.length;
            if (n == 0) return this;
            if (n == 1) {
                IEquipment item = additions[0];
                if (item == null) throw new IllegalArgumentException("Equipment item must exist");
                int[] req = item.requirements(), bon = item.bonuses();
                validateVectors(req,bon);
                addOne(item,req[0],req[1],req[2],req[3],req[4],bon[0],bon[1],bon[2],bon[3],bon[4]);
                return this;
            }
            // Capture and validate the complete batch before changing builder state.
            // The usual one-item append above needs no temporary snapshot buffer.
            int[] captured = new int[Math.multiplyExact(n,10)];
            for (int i=0;i<n;i++) {
                IEquipment item = additions[i];
                if (item == null) throw new IllegalArgumentException("Equipment item must exist");
                int[] req = item.requirements(), bon = item.bonuses();
                validateVectors(req,bon);
                System.arraycopy(req,0,captured,i*10,5);
                System.arraycopy(bon,0,captured,i*10+5,5);
            }
            for (int i=0;i<n;i++) {
                int offset = i*10;
                addOne(additions[i],captured[offset],captured[offset+1],captured[offset+2],captured[offset+3],captured[offset+4],
                        captured[offset+5],captured[offset+6],captured[offset+7],captured[offset+8],captured[offset+9]);
            }
            return this;
        }
        private static void validateVectors(int[] req,int[] bon) {
            if (req == null || req.length != 5 || bon == null || bon.length != 5)
                throw new IllegalArgumentException("Equipment requires exactly five requirements and five bonuses");
        }
        private void addOne(IEquipment item,int q0,int q1,int q2,int q3,int q4,int x0,int x1,int x2,int x3,int x4) {
            data.ensureItemCapacity(itemCount + 1);
            data.items[itemCount] = item;

            // Identity alone cannot prove a repeated item is still zero;
            // the values captured for this occurrence must also be zero.
            if (item == repeatZeroItem
                    && q0 == 0 && q1 == 0 && q2 == 0 && q3 == 0 && q4 == 0
                    && x0 == 0 && x1 == 0 && x2 == 0 && x3 == 0 && x4 == 0) {
                itemCount++;
                // Crossing 64 items changes hardened routing even though the item
                // itself is solver-neutral.
                if (itemCount == 65) snapshotMetadataDirty = true;
                return;
            }

            int w=x0+x1+x2+x3+x4;
            boolean hasReq=q0>0||q1>0||q2>0||q3>0||q4>0;
            boolean hasNeg=x0<0||x1<0||x2<0||x3<0||x4<0;
            boolean allZero=q0==0&&q1==0&&q2==0&&q3==0&&q4==0&&x0==0&&x1==0&&x2==0&&x3==0&&x4==0;
            if (!hasReq && !hasNeg) {
                free0+=x0;free1+=x1;free2+=x2;free3+=x3;free4+=x4;
                if (allZero) repeatZeroItem = item;
                else if (item == repeatZeroItem) repeatZeroItem = null;
                if (!allZero) snapshotMetadataDirty = true;
            } else {
                snapshotMetadataDirty = true;
                if (item == repeatZeroItem) repeatZeroItem = null;
                if (itemCount < 64) data.candidateOriginalMask |= 1L << itemCount;
                data.ensureCandidateCapacity(candidateCount + 1);
                int j=candidateCount++;
                // Track which captured candidate skills have positive requirements
                // or negative bonuses; disjoint masks certify monotone reachability.
                requiredStatsMask |= (q0>0?1:0)|(q1>0?2:0)|(q2>0?4:0)|(q3>0?8:0)|(q4>0?16:0);
                negativeStatsMask |= (x0<0?1:0)|(x1<0?2:0)|(x2<0?4:0)|(x3<0?8:0)|(x4<0?16:0);
                boolean candidatePackable = inPackRange(x0)&&inPackRange(x1)&&inPackRange(x2)&&inPackRange(x3)&&inPackRange(x4)
                        && needPackable(q0,x0)&&needPackable(q1,x1)&&needPackable(q2,x2)&&needPackable(q3,x3)&&needPackable(q4,x4);
                if (data.hardened == null && (!candidatePackable || candidateCount > 9)) {
                    // Earlier candidates are still exactly reconstructible from their captured packed data.
                    // The first unsafe/current candidate must be written from the values just ingested.
                    data.ensureHardenedData(j);
                }
                if (data.hardened != null) data.hardened.set(j,q0,q1,q2,q3,q4,x0,x1,x2,x3,x4);
                data.originalIndex[j]=itemCount;
                data.weight[j]=w;
                data.packedReqAdd[j]=packRequirementAdds(q0,q1,q2,q3,q4);
                data.packedDelta[j]=pack5(x0,x1,x2,x3,x4) - PACK_BIAS_5;
                if (data.packedNeed != null) {
                    data.packedNeed[j]=packNeeds(q0,q1,q2,q3,q4,x0,x1,x2,x3,x4);
                } else if (hasNeg) {
                    // The first negative candidate activates sustainability tracking;
                    // backfill packed needs for earlier positive candidates.
                    data.packedNeed = new long[data.originalIndex.length];
                    for (int i = 0; i < j; i++) data.packedNeed[i] = data.capturedNeed(i);
                    data.packedNeed[j]=packNeeds(q0,q1,q2,q3,q4,x0,x1,x2,x3,x4);
                }
                packedValuesSafe &= candidatePackable;
                neg0+=Math.min(0,x0);neg1+=Math.min(0,x1);neg2+=Math.min(0,x2);neg3+=Math.min(0,x3);neg4+=Math.min(0,x4);
                pos0+=Math.max(0,x0);pos1+=Math.max(0,x1);pos2+=Math.max(0,x2);pos3+=Math.max(0,x3);pos4+=Math.max(0,x4);
                anyNegative |= hasNeg;
            }
            itemCount++;
            if (itemCount == 65) snapshotMetadataDirty = true;
        }
        @Override public IPlayerBuilder<CascadeSentinelV5Player> allocate(SkillPoint point, int amount) {
            int current = switch (point) {
                case STRENGTH -> data.alloc0; case DEXTERITY -> data.alloc1; case INTELLIGENCE -> data.alloc2;
                case DEFENCE -> data.alloc3; case AGILITY -> data.alloc4;
            };
            if (current == amount) return this;
            if (dataPublished) {
                data = new Data(data);
                dataPublished = false;
            }
            switch (point) {
                case STRENGTH -> data.alloc0=amount; case DEXTERITY -> data.alloc1=amount; case INTELLIGENCE -> data.alloc2=amount;
                case DEFENCE -> data.alloc3=amount; case AGILITY -> data.alloc4=amount;
            }
            snapshotMetadataDirty = true;
            return this;
        }
        @Override public CascadeSentinelV5Player build() {
            if (!snapshotMetadataDirty && !anyNegative) {
                dataPublished = true;
                if (itemCount <= 64 && (candidateCount > 9 || !cleanPackedSafe)) {
                    return new HardenedPlayer(data,itemCount,candidateCount,false,cleanPackedBase,
                            free0,free1,free2,free3,free4);
                }
                return new CascadeSentinelV5Player(data,itemCount,candidateCount,false,cleanPackedBase);
            }

            int a0=data.alloc0,a1=data.alloc1,a2=data.alloc2,a3=data.alloc3,a4=data.alloc4;
            int base0=a0+free0,base1=a1+free1,base2=a2+free2,base3=a3+free3,base4=a4+free4;
            boolean safe=packedValuesSafe
                && rangeSafe(base0,neg0,pos0)&&rangeSafe(base1,neg1,pos1)&&rangeSafe(base2,neg2,pos2)
                && rangeSafe(base3,neg3,pos3)&&rangeSafe(base4,neg4,pos4);
            if (!safe) {
                // Packed range safety failed; materialize exact scalar data for hardened solving.
                data.ensureHardenedData(candidateCount);
            }
            long packedBase = safe ? pack5(base0,base1,base2,base3,base4) : 0L;
            boolean denseNineRisk = false;
            if (candidateCount == 9 && anyNegative) {
                boolean allInitial = true;
                int sum0=0,sum1=0,sum2=0,sum3=0,sum4=0;
                int need0=NO_NEED,need1=NO_NEED,need2=NO_NEED,need3=NO_NEED,need4=NO_NEED;
                HardenedData h = data.hardened;
                for(int i=0;i<9;i++){
                    int q0,q1,q2,q3,q4,x0,x1,x2,x3,x4,p0,p1,p2,p3,p4;
                    if (h != null) {
                        q0=h.r0[i];q1=h.r1[i];q2=h.r2[i];q3=h.r3[i];q4=h.r4[i];
                        x0=h.b0[i];x1=h.b1[i];x2=h.b2[i];x3=h.b3[i];x4=h.b4[i];
                        p0=h.p0[i];p1=h.p1[i];p2=h.p2[i];p3=h.p3[i];p4=h.p4[i];
                    } else {
                        long req = data.packedReqAdd[i], bon = data.packedDelta[i] + PACK_BIAS_5;
                        q0=capturedRequirement(req,0);q1=capturedRequirement(req,12);q2=capturedRequirement(req,24);
                        q3=capturedRequirement(req,36);q4=capturedRequirement(req,48);
                        x0=lane(bon,0);x1=lane(bon,12);x2=lane(bon,24);x3=lane(bon,36);x4=lane(bon,48);
                        p0=q0>0?q0+x0:NO_NEED;p1=q1>0?q1+x1:NO_NEED;p2=q2>0?q2+x2:NO_NEED;
                        p3=q3>0?q3+x3:NO_NEED;p4=q4>0?q4+x4:NO_NEED;
                    }
                    if((q0>0&&base0<q0)||(q1>0&&base1<q1)||(q2>0&&base2<q2)||(q3>0&&base3<q3)||(q4>0&&base4<q4)) allInitial=false;
                    sum0+=x0;sum1+=x1;sum2+=x2;sum3+=x3;sum4+=x4;
                    if(p0>need0)need0=p0;if(p1>need1)need1=p1;if(p2>need2)need2=p2;if(p3>need3)need3=p3;if(p4>need4)need4=p4;
                }
                if(allInitial){
                    int final0=base0+sum0,final1=base1+sum1,final2=base2+sum2,final3=base3+sum3,final4=base4+sum4;
                    denseNineRisk=(need0!=NO_NEED&&final0<need0)||(need1!=NO_NEED&&final1<need1)||(need2!=NO_NEED&&final2<need2)
                            ||(need3!=NO_NEED&&final3<need3)||(need4!=NO_NEED&&final4<need4);
                }
                if (denseNineRisk) data.ensureHardenedData(candidateCount);
            }
            if (!anyNegative) {
                cleanPackedSafe = safe;
                cleanPackedBase = packedBase;
                snapshotMetadataDirty = false;
            }
            if (itemCount <= 64 && candidateCount <= 9 && anyNegative && safe && !denseNineRisk) {
                // Drivers contribute at least one positive skill bonus; the remaining
                // common-negative candidates are sinks for the certificate.
                int driverMask = 0;
                boolean nonnegativeDrivers = true;
                for (int i = 0; i < candidateCount; i++) {
                    long bonus = data.packedDelta[i] + PACK_BIAS_5;
                    int b0=lane(bonus,0), b1=lane(bonus,12), b2=lane(bonus,24), b3=lane(bonus,36), b4=lane(bonus,48);
                    if (b0>0 || b1>0 || b2>0 || b3>0 || b4>0) {
                        driverMask |= 1 << i;
                        nonnegativeDrivers &= b0>=0 && b1>=0 && b2>=0 && b3>=0 && b4>=0;
                    }
                }
                dataPublished = true;
                return buildNegativePlayer(packedBase, driverMask, nonnegativeDrivers);
            }
            dataPublished = true;
            if (itemCount <= 64 && (candidateCount > 9 || !safe || denseNineRisk)) {
                return new HardenedPlayer(data,itemCount,candidateCount,anyNegative,packedBase,
                        free0,free1,free2,free3,free4);
            }
            return new CascadeSentinelV5Player(data,itemCount,candidateCount,anyNegative,packedBase);
        }
        private CascadeSentinelV5Player buildNegativePlayer(long packedBase, int driverMask, boolean nonnegativeDrivers) {
            // Precompute aggregate sink bonus and the maximum full-set sustainability
            // threshold for this immutable snapshot. Keep the summary on the Player,
            // not in shared Data.
            int sinks = ((1 << candidateCount) - 1) & ~driverMask;
            long sinkDeltaPacked = 0L;
            long fullNeedPacked = 0L;
            for (int i = 0; i < candidateCount; i++) {
                if ((sinks & (1 << i)) != 0) sinkDeltaPacked += data.packedDelta[i];
                fullNeedPacked = CascadeSentinelV5NegativeCertificate.max5(fullNeedPacked, data.packedNeed[i]);
            }
            return new NegativePlayer(data,itemCount,candidateCount,driverMask,packedBase,sinkDeltaPacked,fullNeedPacked,nonnegativeDrivers,
                    (requiredStatsMask & negativeStatsMask) == 0);
        }
        private static boolean inPackRange(int v){return v>=-PACK_BIAS && v<PACK_BIAS;}
        private static boolean needPackable(int r,int b){return r<=0 || (inPackRange(r)&&inPackRange(r+b));}
        private static boolean rangeSafe(int base,int neg,int pos){return inPackRange(base+neg)&&inPackRange(base+pos);}
        private static long laneReqAdd(int r){return r>0 ? PACK_BIAS-r : 0x800L;}
        private static long laneNeed(int r,int b){return r>0 ? r+b+PACK_BIAS : 0L;}
        private static long packRequirementAdds(int r0,int r1,int r2,int r3,int r4){
            return laneReqAdd(r0)|(laneReqAdd(r1)<<12)|(laneReqAdd(r2)<<24)|(laneReqAdd(r3)<<36)|(laneReqAdd(r4)<<48);
        }
        private static long packNeeds(int r0,int r1,int r2,int r3,int r4,int b0,int b1,int b2,int b3,int b4){
            return laneNeed(r0,b0)|(laneNeed(r1,b1)<<12)|(laneNeed(r2,b2)<<24)|(laneNeed(r3,b3)<<36)|(laneNeed(r4,b4)<<48);
        }
    }
}
