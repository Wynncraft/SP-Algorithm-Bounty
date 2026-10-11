package com.wynncraft.algorithms;

/** Driver/sink feasibility certificate for packed common-negative snapshots. */
final class CascadeSentinelCachedV5NegativeCertificate {
    private static final long GUARD_BITS = 0x0800_8008_0080_0800L;

    private CascadeSentinelCachedV5NegativeCertificate() { }

    /**
     * Returns 1 when all candidates are proven feasible, 2 when no candidate can
     * be the first legal transition, and 0 when the certificate is inconclusive.
     */
    static int tryFull(
            CascadeSentinelCachedV5Player player,
            CascadeSentinelCachedV5Player.Data d,
            int fullMask,
            long base) {
        CascadeSentinelCachedV5Player.NegativePlayer negative = (CascadeSentinelCachedV5Player.NegativePlayer) player;
        // driverMask contains only candidate indices, so no fullMask clamp is needed.
        final int drivers = negative.driverMask();

        int remainingDrivers = drivers;
        long total = base;
        long threshold = 0L;
        boolean progress;
        if (negative.nonnegativeDrivers()) {
            // Componentwise nonnegative drivers cannot invalidate previous drivers.
            do {
                progress = false;
                for (int bits = remainingDrivers; bits != 0; bits &= bits - 1) {
                    int i = Integer.numberOfTrailingZeros(bits);
                    if (!meetsStaticReq(total, d.packedReqAdd[i])) continue;
                    remainingDrivers ^= 1 << i;
                    total += d.packedBonus[i] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    progress = true;
                }
            } while (progress && remainingDrivers != 0);
        } else {
            do {
                progress = false;
                for (int bits = remainingDrivers; bits != 0; bits &= bits - 1) {
                    int i = Integer.numberOfTrailingZeros(bits);
                    if (!meetsStaticReq(total, d.packedReqAdd[i])) continue;
                    long nextTotal = total + d.packedBonus[i] - CascadeSentinelCachedV5Player.PACK_BIAS_5;
                    if (!ge5(nextTotal, threshold)) continue;
                    remainingDrivers ^= 1 << i;
                    total = nextTotal;
                    threshold = max5(threshold, d.packedNeed[i]);
                    progress = true;
                }
            } while (progress && remainingDrivers != 0);
        }

        if (remainingDrivers != 0) {
            if (remainingDrivers == drivers) {
                final int sinks = fullMask & ~drivers;
                for (int bits = sinks; bits != 0; bits &= bits - 1) {
                    int i = Integer.numberOfTrailingZeros(bits);
                    if (meetsStaticReq(base, d.packedReqAdd[i])) return 0;
                }
                player.setPackedTotal(base);
                return 2;
            }
            return 0;
        }

        // Sinks have no positive component. If the final total sustains the
        // full set, each sink can be appended after the drivers without breaking
        // earlier items. Include every candidate in the final threshold.
        long finalThreshold = negative.fullNeedPacked;
        long finalTotal = total + negative.sinkDeltaPacked;
        if (!ge5(finalTotal, finalThreshold)) return 0;
        player.setPackedTotal(finalTotal);
        return 1;
    }

    private static boolean meetsStaticReq(long values, long reqAdd) {
        return ((values + reqAdd) & GUARD_BITS) == GUARD_BITS;
    }

    private static boolean ge5(long values, long limits) {
        return (((values | GUARD_BITS) - limits) & GUARD_BITS) == GUARD_BITS;
    }

    static long max5(long a, long b) {
        long greater = ((a | GUARD_BITS) - b) & GUARD_BITS;
        long laneBits = greater >>> 11;
        long select = greater | (greater - laneBits);
        return (a & select) | (b & ~select);
    }
}
