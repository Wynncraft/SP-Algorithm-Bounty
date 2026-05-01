package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * V2 keeps V1's cascade-activation backtracking but cuts its per-step costs:
 *
 *   - Cache `requirements()` / `bonuses()` once at the top of run() so the
 *     interface call overhead doesn't repeat at every recursion level.
 *   - hasNeg flag per item: cascade exclude-self re-walk only fires when the
 *     just-activated item has a negative bonus, and only on negative attrs.
 *   - sp updated in place with add/undo (no clones per branch).
 *   - Scratch arrays are instance-resident and resized lazily, so steady-state
 *     run() does no scratch allocation. (Result Lists must still be allocated
 *     because they're returned to the caller.)
 */
@Information(name = "WynnSolver", version = 2, authors = {"Alex-Guha"})
public class WynnSolverV2Algorithm implements IAlgorithm<WynnPlayer> {

    private static final int SP_COUNT = 5;
    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int INIT_CAPACITY = 16;

    private IEquipment[] items = new IEquipment[INIT_CAPACITY];
    private int[][] reqs = new int[INIT_CAPACITY][];
    private int[][] bonuses = new int[INIT_CAPACITY][];
    private boolean[] hasReq = new boolean[INIT_CAPACITY];
    private boolean[] hasNeg = new boolean[INIT_CAPACITY];
    private boolean[] activated = new boolean[INIT_CAPACITY];
    private boolean[] best = new boolean[INIT_CAPACITY];
    private int[] rem = new int[INIT_CAPACITY];
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
            hasReq = new boolean[cap];
            hasNeg = new boolean[cap];
            activated = new boolean[cap];
            best = new boolean[cap];
            rem = new int[cap];
        }

        IEquipment[] items = this.items;
        int[][] reqs = this.reqs;
        int[][] bonuses = this.bonuses;
        boolean[] hasReq = this.hasReq;
        boolean[] hasNeg = this.hasNeg;
        boolean[] activated = this.activated;
        boolean[] best = this.best;
        int[] rem = this.rem;
        int[] sp = this.sp;

        for (int i = 0; i < n; i++) {
            IEquipment it = equipment.get(i);
            items[i] = it;
            int[] r = it.requirements();
            int[] b = it.bonuses();
            reqs[i] = r;
            bonuses[i] = b;
            boolean hr = false, hn = false;
            for (int s = 0; s < SP_COUNT; s++) {
                if (r[s] > 0) hr = true;
                if (b[s] < 0) hn = true;
            }
            hasReq[i] = hr;
            hasNeg[i] = hn;
            activated[i] = false;
        }

        for (int s = 0; s < SP_COUNT; s++) sp[s] = player.allocated(SKILL_POINTS[s]);

        int k = 0;
        int baseCount = 0;
        int baseWeight = 0;
        for (int i = 0; i < n; i++) {
            if (!hasReq[i] && !hasNeg[i]) {
                activated[i] = true;
                int[] b = bonuses[i];
                int w = 0;
                for (int s = 0; s < SP_COUNT; s++) { sp[s] += b[s]; w += b[s]; }
                baseCount++;
                baseWeight += w;
            } else {
                rem[k++] = i;
            }
        }

        if (k == 0) {
            System.arraycopy(activated, 0, best, 0, n);
            return finishResult(items, best, player, n);
        }

        System.arraycopy(activated, 0, best, 0, n);
        bestStats[0] = baseCount;
        bestStats[1] = baseWeight;

        bt(reqs, bonuses, hasNeg, rem, k, sp, activated, baseCount, baseWeight, bestStats, best, n);

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

    private static void bt(int[][] reqs, int[][] bonuses, boolean[] hasNeg,
                           int[] rem, int k, int[] sp, boolean[] activated,
                           int count, int weight, int[] best, boolean[] bestActivated, int n) {
        if (count > best[0] || (count == best[0] && weight > best[1])) {
            best[0] = count;
            best[1] = weight;
            System.arraycopy(activated, 0, bestActivated, 0, n);
        }
        if (best[0] == n) return;

        int rest = 0;
        for (int j = 0; j < k; j++) if (!activated[rem[j]]) rest++;
        if (count + rest <= best[0]) return;

        for (int j = 0; j < k; j++) {
            int i = rem[j];
            if (activated[i]) continue;

            int[] r = reqs[i];
            // r[s] > 0 guard: sp[s] can be negative, and req of 0 must still pass.
            if ((r[0] > 0 && sp[0] < r[0]) ||
                (r[1] > 0 && sp[1] < r[1]) ||
                (r[2] > 0 && sp[2] < r[2]) ||
                (r[3] > 0 && sp[3] < r[3]) ||
                (r[4] > 0 && sp[4] < r[4])) {
                continue;
            }

            int[] b = bonuses[i];
            sp[0] += b[0]; sp[1] += b[1]; sp[2] += b[2]; sp[3] += b[3]; sp[4] += b[4];

            // Cascade re-check only if this item subtracted SP somewhere.
            boolean cascadeOk = true;
            if (hasNeg[i]) {
                outer:
                for (int s = 0; s < SP_COUNT; s++) {
                    if (b[s] >= 0) continue;
                    int spS = sp[s];
                    for (int x = 0; x < n; x++) {
                        if (!activated[x]) continue;
                        int[] rx = reqs[x];
                        if (rx[s] > 0 && rx[s] + bonuses[x][s] > spS) {
                            cascadeOk = false;
                            break outer;
                        }
                    }
                }
            }

            if (cascadeOk) {
                activated[i] = true;
                int wb = b[0] + b[1] + b[2] + b[3] + b[4];
                bt(reqs, bonuses, hasNeg, rem, k, sp, activated, count + 1, weight + wb, best, bestActivated, n);
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
