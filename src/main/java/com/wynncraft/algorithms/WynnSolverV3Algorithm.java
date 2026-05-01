package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * V3 builds on V2 with three additional ideas inspired by the leaderboard cluster:
 *
 *  - Greedy fixed-point fast path. When the build has no negative-bonus items
 *    anywhere, monotonicity guarantees greedy activation is provably optimal:
 *    any feasible item stays feasible as sp grows, cascade is trivially safe,
 *    and max-subset is achieved by activating every fittable item until fixed
 *    point. This bypasses backtracking entirely.
 *
 *  - reqMask per item. Most items have reqs on 0-2 attrs out of 5; storing a
 *    5-bit mask of which attrs are constrained lets us iterate only those
 *    attrs via TZCNT bit-walk, skipping the unconditional 5-attr scan.
 *
 *  - Bit-iteration over the remaining set. The backtrack maintains an int
 *    bitmask of unactivated constrained items and walks it via `m & -m` /
 *    `m &= m - 1`, instead of scanning a fixed-size rem[] and skipping
 *    already-activated entries.
 *
 * The cascade-feasibility logic is unchanged from V2: greedy free pre-pass,
 * exclude-self cascade re-check fires only when an item subtracts SP, and only
 * on the attrs it subtracted. Scratch arrays are instance-resident.
 */
@Information(name = "WynnSolver", version = 3, authors = {"Alex-Guha"})
public class WynnSolverV3Algorithm implements IAlgorithm<WynnPlayer> {

    private static final int SP_COUNT = 5;
    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int INIT_CAPACITY = 16;

    private IEquipment[] items = new IEquipment[INIT_CAPACITY];
    private int[][] reqs = new int[INIT_CAPACITY][];
    private int[][] bonuses = new int[INIT_CAPACITY][];
    private int[] reqMask = new int[INIT_CAPACITY];
    private int[] negMask = new int[INIT_CAPACITY];
    private boolean[] activated = new boolean[INIT_CAPACITY];
    private boolean[] best = new boolean[INIT_CAPACITY];
    private final int[] sp = new int[SP_COUNT];
    private final int[] bestStats = new int[2];

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int n = equipment.size();

        if (items.length < n) {
            int cap = Math.max(n, items.length * 2);
            items = new IEquipment[cap];
            reqs = new int[cap][];
            bonuses = new int[cap][];
            reqMask = new int[cap];
            negMask = new int[cap];
            activated = new boolean[cap];
            best = new boolean[cap];
        }

        IEquipment[] items = this.items;
        int[][] reqs = this.reqs;
        int[][] bonuses = this.bonuses;
        int[] reqMask = this.reqMask;
        int[] negMask = this.negMask;
        boolean[] activated = this.activated;
        boolean[] best = this.best;
        int[] sp = this.sp;

        int globalNegMask = 0;
        for (int i = 0; i < n; i++) {
            IEquipment it = equipment.get(i);
            items[i] = it;
            int[] r = it.requirements();
            int[] b = it.bonuses();
            reqs[i] = r;
            bonuses[i] = b;
            int rm = 0, nm = 0;
            for (int s = 0; s < SP_COUNT; s++) {
                if (r[s] > 0) rm |= 1 << s;
                if (b[s] < 0) nm |= 1 << s;
            }
            reqMask[i] = rm;
            negMask[i] = nm;
            activated[i] = false;
            globalNegMask |= nm;
        }

        for (int s = 0; s < SP_COUNT; s++) sp[s] = player.allocated(SKILL_POINTS[s]);

        // Phase 1: free items (no req, no neg) — apply unconditionally.
        // Build a bitmask of remaining (constrained) item indices in one pass.
        int remaining = 0;
        for (int i = 0; i < n; i++) {
            if (reqMask[i] == 0 && negMask[i] == 0) {
                activated[i] = true;
                int[] b = bonuses[i];
                sp[0] += b[0]; sp[1] += b[1]; sp[2] += b[2]; sp[3] += b[3]; sp[4] += b[4];
            } else {
                remaining |= 1 << i;
            }
        }

        if (remaining == 0) {
            return finishResult(items, activated, player, n);
        }

        // Fast path: no item anywhere subtracts SP. Greedy fixed-point is provably
        // optimal — sp only grows, so any item that ever becomes feasible stays
        // feasible, and cascade is trivially preserved.
        if (globalNegMask == 0) {
            int rem = remaining;
            boolean changed = true;
            while (changed) {
                changed = false;
                int m = rem;
                while (m != 0) {
                    int i = Integer.numberOfTrailingZeros(m);
                    int bit = 1 << i;
                    m &= m - 1;
                    int[] r = reqs[i];
                    int rm = reqMask[i];
                    boolean fits = true;
                    while (rm != 0) {
                        int s = Integer.numberOfTrailingZeros(rm);
                        if (sp[s] < r[s]) { fits = false; break; }
                        rm &= rm - 1;
                    }
                    if (!fits) continue;
                    int[] b = bonuses[i];
                    sp[0] += b[0]; sp[1] += b[1]; sp[2] += b[2]; sp[3] += b[3]; sp[4] += b[4];
                    activated[i] = true;
                    rem &= ~bit;
                    changed = true;
                }
            }
            return finishResult(items, activated, player, n);
        }

        // General path: backtrack over the constrained set, with bit-iteration
        // over `remaining` and per-item reqMask-driven check.
        System.arraycopy(activated, 0, best, 0, n);
        int baseCount = Integer.bitCount(((1 << n) - 1) & ~remaining);
        int baseWeight = 0;
        for (int i = 0; i < n; i++) {
            if (activated[i]) {
                int[] b = bonuses[i];
                baseWeight += b[0] + b[1] + b[2] + b[3] + b[4];
            }
        }
        bestStats[0] = baseCount;
        bestStats[1] = baseWeight;

        bt(reqs, bonuses, reqMask, negMask, remaining, sp, activated,
                baseCount, baseWeight, bestStats, best, n);

        return finishResult(items, best, player, n);
    }

    private static Result finishResult(IEquipment[] items, boolean[] keep, WynnPlayer player, int n) {
        List<IEquipment> valid = new ArrayList<>(n);
        List<IEquipment> invalid = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                valid.add(items[i]);
                player.modify(items[i].bonuses(), true);
            } else {
                invalid.add(items[i]);
            }
        }
        return new Result(valid, invalid);
    }

    private static void bt(int[][] reqs, int[][] bonuses, int[] reqMask, int[] negMask,
                           int remaining, int[] sp, boolean[] activated,
                           int count, int weight, int[] best, boolean[] bestActivated, int n) {
        if (count > best[0] || (count == best[0] && weight > best[1])) {
            best[0] = count;
            best[1] = weight;
            System.arraycopy(activated, 0, bestActivated, 0, n);
        }
        if (best[0] == n) return;

        int rest = Integer.bitCount(remaining);
        if (count + rest <= best[0]) return;

        int m = remaining;
        while (m != 0) {
            int i = Integer.numberOfTrailingZeros(m);
            int bit = 1 << i;
            m &= m - 1;

            int[] r = reqs[i];
            int rm = reqMask[i];
            boolean fits = true;
            while (rm != 0) {
                int s = Integer.numberOfTrailingZeros(rm);
                if (sp[s] < r[s]) { fits = false; break; }
                rm &= rm - 1;
            }
            if (!fits) continue;

            int[] b = bonuses[i];
            sp[0] += b[0]; sp[1] += b[1]; sp[2] += b[2]; sp[3] += b[3]; sp[4] += b[4];

            // Cascade re-check only if this item subtracted SP, and only on those attrs.
            boolean cascadeOk = true;
            int nm = negMask[i];
            if (nm != 0) {
                int nmIter = nm;
                outer:
                while (nmIter != 0) {
                    int s = Integer.numberOfTrailingZeros(nmIter);
                    nmIter &= nmIter - 1;
                    int spS = sp[s];
                    for (int x = 0; x < n; x++) {
                        if (!activated[x]) continue;
                        if ((reqMask[x] & (1 << s)) == 0) continue;
                        int[] rx = reqs[x];
                        if (rx[s] + bonuses[x][s] > spS) {
                            cascadeOk = false;
                            break outer;
                        }
                    }
                }
            }

            if (cascadeOk) {
                activated[i] = true;
                int wb = b[0] + b[1] + b[2] + b[3] + b[4];
                bt(reqs, bonuses, reqMask, negMask, remaining & ~bit, sp, activated,
                        count + 1, weight + wb, best, bestActivated, n);
                activated[i] = false;
                if (best[0] == n) {
                    sp[0] -= b[0]; sp[1] -= b[1]; sp[2] -= b[2]; sp[3] -= b[3]; sp[4] -= b[4];
                    return;
                }
            }

            sp[0] -= b[0]; sp[1] -= b[1]; sp[2] -= b[2]; sp[3] -= b[3]; sp[4] -= b[4];
        }
    }

    @Override
    public void clearCache() {
        // Scratch is fully overwritten on each run(); nothing to invalidate.
    }
}
