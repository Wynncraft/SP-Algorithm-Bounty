package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Exact adaptive skill-point solver with exact reuse for fully solved positive snapshots.
 *
 * <p>Snapshots admitted to the packed common path use specialized handling for
 * at most 64 items and nine candidates. Positive-only snapshots use monotone
 * closure. The same closure also applies when every negative bonus is confined
 * to skills with no positive candidate requirement. Other negative snapshots
 * try a driver/sink certificate, then a constructive full-set pass; unresolved
 * cases enter the 512-state exact subset search.</p>
 *
 * <p>Snapshots outside that domain, including numerically unsafe and dense-nine
 * cases, route to scalar hardened solvers. Builder work is limited to metadata
 * preparation; solve results are produced by {@code run()}.</p>
 *
 * <p>Exact repeated positive eight-candidate snapshots may reuse the solved packed total
 * directly. Later positive snapshots from the same append-only builder sequence
 * may resume from a proven full prefix when provenance and lane-wise base
 * monotonicity checks pass. {@code clearCache()} discards all reusable state;
 * negative and hardened solves do not publish cache entries.</p>
 */
@Information(name = "Cascade Sentinel Cached", version = 5, authors = {"Luuk"})
public final class CascadeSentinelCachedV5Algorithm implements IAlgorithm<CascadeSentinelCachedV5Player> {
    private static final int FAST_BITS = 9;
    private static final int FAST_STATES = 1 << FAST_BITS;
    private static final long LANE_GUARDS = 0x0800_8008_0080_0800L;

    private final CascadeSentinelCachedV5SmallHardenedSolver smallHardened = new CascadeSentinelCachedV5SmallHardenedSolver();
    private final CascadeSentinelCachedV5LargeHardenedSolver largeHardened = new CascadeSentinelCachedV5LargeHardenedSolver();
    private final int[] reachedGeneration = new int[FAST_STATES];
    private final long[] stateTotal = new long[FAST_STATES];
    private final long[] stateThreshold = new long[FAST_STATES];
    private final int[] stateWeight = new int[FAST_STATES];
    private int generation;
    private CascadeSentinelCachedV5Player cachedPlayer;
    private long cachedSolvedTotal;

    @Override
    public Result run(CascadeSentinelCachedV5Player player) {
        final int k = player.candidateCount();
        if (player.itemCount > 64 || k > FAST_BITS) return largeHardened.run(player);
        // Within <=64 items and <=9 candidates, HardenedPlayer marks snapshots
        // that require scalar handling because packed range safety fails or the dense-nine guard triggers.
        if (player instanceof CascadeSentinelCachedV5Player.HardenedPlayer) return smallHardened.run(player);

        if (k == 0) {
            player.setPackedTotal(player.basePacked);
            return new Result(player, Collections.emptyList());
        }

        final CascadeSentinelCachedV5Player.Data d = player.data;
        final int fullMask = (1 << k) - 1;

        // Positive-only systems are monotone: the closure is the unique maximum
        // reachable set, so no subset search is needed.
        if (!player.anyNegative()) {
            if (k == 8 && tryReuseFullK8(player)) {
                return new Result(player, Collections.emptyList());
            }
            int active = 0;
            int remaining = fullMask;
            long total = player.basePacked;
            CascadeSentinelCachedV5Player cached = cachedPlayer;
            if (cached != null
                    && player.data == cached.data
                    && player.itemCount > cached.itemCount
                    && player.data.sequenceItemCount == player.itemCount) {
                int cachedK = cached.candidateCount();
                if (cachedK <= k && ge5(player.basePacked, cached.basePacked)) {
                    // The cached player is a fully solved prefix of the same append-only Data.
                    // A lane-wise non-decreasing base means only free non-negative bonuses
                    // were added outside that prefix. Packed range safety makes the base
                    // subtraction borrow-free and the following addition carry-free per lane.
                    active = (1 << cachedK) - 1;
                    remaining &= ~active;
                    total = cachedSolvedTotal + (player.basePacked - cached.basePacked);
                }
            }
            boolean progress;
            do {
                progress = false;
                if ((remaining & 256) != 0 && meetsStaticReq(total, d.packedReqAdd[8])) {
                    active |= 256; remaining ^= 256;
                    total += d.packedBonus[8] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 128) != 0 && meetsStaticReq(total, d.packedReqAdd[7])) {
                    active |= 128; remaining ^= 128;
                    total += d.packedBonus[7] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 64) != 0 && meetsStaticReq(total, d.packedReqAdd[6])) {
                    active |= 64; remaining ^= 64;
                    total += d.packedBonus[6] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 32) != 0 && meetsStaticReq(total, d.packedReqAdd[5])) {
                    active |= 32; remaining ^= 32;
                    total += d.packedBonus[5] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 16) != 0 && meetsStaticReq(total, d.packedReqAdd[4])) {
                    active |= 16; remaining ^= 16;
                    total += d.packedBonus[4] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 8) != 0 && meetsStaticReq(total, d.packedReqAdd[3])) {
                    active |= 8; remaining ^= 8;
                    total += d.packedBonus[3] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 4) != 0 && meetsStaticReq(total, d.packedReqAdd[2])) {
                    active |= 4; remaining ^= 4;
                    total += d.packedBonus[2] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 2) != 0 && meetsStaticReq(total, d.packedReqAdd[1])) {
                    active |= 2; remaining ^= 2;
                    total += d.packedBonus[1] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
                if ((remaining & 1) != 0 && meetsStaticReq(total, d.packedReqAdd[0])) {
                    active |= 1; remaining ^= 1;
                    total += d.packedBonus[0] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
            } while (progress && remaining != 0);
            player.setPackedTotal(total);
            if (remaining == 0) {
                cacheFullPositive(player, total);
                return new Result(player, Collections.emptyList());
            }
            return result(player, active);
        }

        if (player.negativeBonusesIrrelevant()) return runMonotoneNegative(player, d, fullMask);

        // The driver/sink certificate can prove full feasibility or prove that no
        // candidate can start. Inconclusive cases fall through to constructive/exact solving.
        final long negativeBase = player.basePacked;
        int r2Certificate = CascadeSentinelCachedV5NegativeCertificate.tryFull(player, d, fullMask, negativeBase);
        if (r2Certificate == 1) return new Result(player, Collections.emptyList());
        if (r2Certificate == 2) return result(player, 0);

        // Constructive certificate. If this reaches every candidate, optimality
        // is immediate and we avoid the exact state table entirely.
        int greedyMask = 0;
        int remaining = fullMask;
        long greedyTotal = negativeBase;
        long greedyThreshold = 0L;
        boolean progress;
        do {
            progress = false;
            for (int bits = remaining; bits != 0; bits &= bits - 1) {
                int i = Integer.numberOfTrailingZeros(bits);
                if (!meetsStaticReq(greedyTotal, d.packedReqAdd[i])) continue;
                long nextTotal = greedyTotal + d.packedBonus[i] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                if (!ge5(nextTotal, greedyThreshold)) continue;
                int bit = 1 << i;
                greedyMask |= bit;
                remaining ^= bit;
                greedyTotal = nextTotal;
                greedyThreshold = max5(greedyThreshold, d.packedNeed[i]);
                progress = true;
            }
        } while (progress && remaining != 0);

        if (remaining == 0) {
            player.setPackedTotal(greedyTotal);
            return new Result(player, Collections.emptyList());
        }
        if (greedyMask == 0) {
            // If no candidate can be the first legal transition, no legal equip
            // sequence can start. The unconditional free items are optimal.
            player.setPackedTotal(negativeBase);
            return result(player, 0);
        }

        int greedyWeight = 0;
        for (int bits = greedyMask; bits != 0; bits &= bits - 1) {
            greedyWeight += d.weight[Integer.numberOfTrailingZeros(bits)];
        }

        // Exact subset reachability for the small unresolved case.
        int stamp = ++generation;
        if (stamp == 0) {
            java.util.Arrays.fill(reachedGeneration, 0);
            stamp = ++generation;
        }
        reachedGeneration[0] = stamp;
        stateTotal[0] = negativeBase;
        stateThreshold[0] = 0L;
        stateWeight[0] = 0;

        int bestMask = greedyMask;
        int bestCount = Integer.bitCount(greedyMask);
        int bestWeight = greedyWeight;
        long bestTotal = greedyTotal;

        for (int mask = 0; mask <= fullMask; mask++) {
            if (reachedGeneration[mask] != stamp) continue;
            long total = stateTotal[mask];
            long threshold = stateThreshold[mask];
            int weight = stateWeight[mask];
            int absent = fullMask & ~mask;
            for (int bits = absent; bits != 0; bits &= bits - 1) {
                int i = Integer.numberOfTrailingZeros(bits);
                if (!meetsStaticReq(total, d.packedReqAdd[i])) continue;
                long nextTotal = total + d.packedBonus[i] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                if (!ge5(nextTotal, threshold)) continue;
                int nextMask = mask | (1 << i);
                if (reachedGeneration[nextMask] == stamp) continue;

                int nextWeight = weight + d.weight[i];
                reachedGeneration[nextMask] = stamp;
                stateTotal[nextMask] = nextTotal;
                stateThreshold[nextMask] = max5(threshold, d.packedNeed[i]);
                stateWeight[nextMask] = nextWeight;

                int nextCount = Integer.bitCount(nextMask);
                if (nextCount > bestCount || (nextCount == bestCount && nextWeight > bestWeight)) {
                    bestMask = nextMask;
                    bestCount = nextCount;
                    bestWeight = nextWeight;
                    bestTotal = nextTotal;
                    if (nextMask == fullMask) {
                        player.setPackedTotal(nextTotal);
                        return new Result(player, Collections.emptyList());
                    }
                }
            }
        }

        player.setPackedTotal(bestTotal);
        return result(player, bestMask);
    }

    // The snapshot proves that required skills receive no negative bonuses.
    // Bonuses on unrequired skills still contribute to totals and tie weight.
    // Reachability is monotone on every required skill, so closure is exact.
    // This negative-only route neither reads nor publishes the positive cache.
    private static Result runMonotoneNegative(CascadeSentinelCachedV5Player player, CascadeSentinelCachedV5Player.Data d, int fullMask) {
            int remaining = fullMask;
            long total = player.basePacked;
            int remainingBeforePass;
            do {
                remainingBeforePass = remaining;
                if ((remaining & 256) != 0 && meetsStaticReq(total, d.packedReqAdd[8])) {
                    remaining ^= 256;
                    total += d.packedBonus[8] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 128) != 0 && meetsStaticReq(total, d.packedReqAdd[7])) {
                    remaining ^= 128;
                    total += d.packedBonus[7] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 64) != 0 && meetsStaticReq(total, d.packedReqAdd[6])) {
                    remaining ^= 64;
                    total += d.packedBonus[6] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 32) != 0 && meetsStaticReq(total, d.packedReqAdd[5])) {
                    remaining ^= 32;
                    total += d.packedBonus[5] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 16) != 0 && meetsStaticReq(total, d.packedReqAdd[4])) {
                    remaining ^= 16;
                    total += d.packedBonus[4] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 8) != 0 && meetsStaticReq(total, d.packedReqAdd[3])) {
                    remaining ^= 8;
                    total += d.packedBonus[3] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 4) != 0 && meetsStaticReq(total, d.packedReqAdd[2])) {
                    remaining ^= 4;
                    total += d.packedBonus[2] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 2) != 0 && meetsStaticReq(total, d.packedReqAdd[1])) {
                    remaining ^= 2;
                    total += d.packedBonus[1] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
                if ((remaining & 1) != 0 && meetsStaticReq(total, d.packedReqAdd[0])) {
                    remaining ^= 1;
                    total += d.packedBonus[0] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                }
            } while (remaining != 0 && remaining != remainingBeforePass);
            player.setPackedTotal(total);
            if (remaining == 0) return new Result(player, Collections.emptyList());
            return result(player, fullMask ^ remaining);
    }

    @Override
    public void clearCache() {
        cachedPlayer = null;
        smallHardened.clearCache();
        largeHardened.clearCache();
    }

    private boolean tryReuseFullK8(CascadeSentinelCachedV5Player player) {
        // Exact reuse requires the same captured candidate data and packed base state.
        CascadeSentinelCachedV5Player cached = cachedPlayer;
        if (cached == null
                || cached.data != player.data
                || cached.candidateMeta != player.candidateMeta
                || cached.basePacked != player.basePacked) {
            return false;
        }
        player.setPackedTotal(cachedSolvedTotal);
        return true;
    }

    private void cacheFullPositive(CascadeSentinelCachedV5Player player, long solvedTotal) {
        cachedPlayer = player;
        cachedSolvedTotal = solvedTotal;
    }

    private static boolean meetsStaticReq(long values, long reqAdd) {
        return ((values + reqAdd) & LANE_GUARDS) == LANE_GUARDS;
    }

    private static boolean ge5(long values, long limits) {
        return (((values | LANE_GUARDS) - limits) & LANE_GUARDS) == LANE_GUARDS;
    }

    private static long max5(long a, long b) {
        long greater = ((a | LANE_GUARDS) - b) & LANE_GUARDS;
        long laneBits = greater >>> 11;
        long select = greater | (greater - laneBits);
        return (a & select) | (b & ~select);
    }

    private static Result result(CascadeSentinelCachedV5Player player, int candidateMask) {
        long selected = player.freeOriginalMask();
        for (int bits = candidateMask; bits != 0; bits &= bits - 1) {
            int candidate = Integer.numberOfTrailingZeros(bits);
            selected |= 1L << player.data.originalIndex[candidate];
        }
        int count = Long.bitCount(selected);
        if (count == player.itemCount) return new Result(player, Collections.emptyList());
        if (count == 0) return new Result(Collections.emptyList(), player);
        long all = player.itemCount == 64 ? -1L : (1L << player.itemCount) - 1L;
        return new Result(
            new MaskList(player.data.items, selected, count),
            new MaskList(player.data.items, all & ~selected, player.itemCount - count)
        );
    }

    private static final class MaskList extends AbstractList<IEquipment> {
        private final IEquipment[] items;
        private final long mask;
        private final int size;
        MaskList(IEquipment[] items, long mask, int size) {
            this.items = items; this.mask = mask; this.size = size;
        }
        @Override public IEquipment get(int index) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
            long bits = mask;
            for (int i = 0; i < index; i++) bits &= bits - 1;
            return items[Long.numberOfTrailingZeros(bits)];
        }
        @Override public int size() { return size; }
    }
}
