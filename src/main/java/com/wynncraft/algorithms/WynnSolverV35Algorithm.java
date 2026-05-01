package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;

import java.util.ArrayList;
import java.util.List;

/**
 * V3.5 — V3 with a custom IPlayer (WynnSolverPlayer) that exposes the
 * allocated[] and bonus[] arrays directly. Behavior is identical to V3
 * by construction; only the IPlayer access pattern changes:
 *
 *  - Base SP read in one int[] field load instead of 5x interface
 *    `allocated(SkillPoint)` calls (each costs an ordinal() lookup +
 *    array index).
 *
 *  - finishResult writes the aggregated bonus vector to the player in
 *    one pass instead of calling player.modify() per kept item (which
 *    re-iterates 5 entries and re-touches `weight` each call).
 *
 * The README explicitly allows custom IPlayer implementations.
 */
@Information(name = "WynnSolver", version = 35, authors = {"Alex-Guha"})
public class WynnSolverV35Algorithm implements IAlgorithm<WynnSolverPlayer> {

    private static final int SP_COUNT = 5;
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
    public Result run(WynnSolverPlayer player) {
        List<IEquipment> equipment = player.equipment;
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

        int[] alloc = player.allocated;
        sp[0] = alloc[0]; sp[1] = alloc[1]; sp[2] = alloc[2]; sp[3] = alloc[3]; sp[4] = alloc[4];

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
            return finishResult(items, bonuses, activated, player, n);
        }

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
            return finishResult(items, bonuses, activated, player, n);
        }

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

        return finishResult(items, bonuses, best, player, n);
    }

    private static Result finishResult(IEquipment[] items, int[][] bonuses, boolean[] keep,
                                       WynnSolverPlayer player, int n) {
        List<IEquipment> valid = new ArrayList<>(n);
        List<IEquipment> invalid = new ArrayList<>(n);
        int b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0;
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                valid.add(items[i]);
                int[] b = bonuses[i];
                b0 += b[0]; b1 += b[1]; b2 += b[2]; b3 += b[3]; b4 += b[4];
            } else {
                invalid.add(items[i]);
            }
        }
        int[] pb = player.bonus;
        pb[0] += b0; pb[1] += b1; pb[2] += b2; pb[3] += b3; pb[4] += b4;
        player.weight += b0 + b1 + b2 + b3 + b4;
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
    public void clearCache() { }
}
