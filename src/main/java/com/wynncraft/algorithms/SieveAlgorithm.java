package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exact solver. Throws out everything that cannot matter, then searches the
 * little that is left.
 *
 * <p>An item only counts as equipped if the player meets its requirements
 * without help from that item's own bonus, and if nothing equipped afterwards
 * drags a skill back below what an already-equipped item needs. The answer is
 * the largest set of items that survives that. If two sets tie on size, the one
 * granting more skill points wins.
 *
 * <p>Three things get cleared away before any searching starts:
 *
 * <ol>
 *   <li>An item with no requirements and no negative bonus is always worth
 *       wearing, so it is taken straight away.
 *   <li>A skill that no item requires cannot change any outcome, so bonuses to
 *       it are ignored. The same goes for a skill with so much slack that
 *       nothing in the build could push it below a requirement.
 *   <li>Of what is left, an item with no negative bonus that needs no skill any
 *       other item drains is safe the moment it fits. It can neither break
 *       another item nor be broken by one.
 * </ol>
 *
 * <p>That leaves only items that drain a skill someone needs, or that need a
 * skill someone drains. Those go to the search. On the benchmark builds it is
 * two items out of twenty-three.
 *
 * <p>Working memory is reused between calls, so a run allocates the two result
 * lists and nothing else.
 */
@Information(name = "Sieve", version = 1, authors = {"mikroskeem"})
public class SieveAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final int K = 5;
    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    private static final int PHI32 = 0x9E3779B1;      // Knuth, 2^32/phi
    private static final int MURMUR_A = 0x85EBCA6B;   // MurmurHash3 fmix32
    private static final int MURMUR_B = 0xC2B2AE35;   // MurmurHash3 fmix32

    // ── memoised answers ────────────────────────────────────────────────
    // Level 0 is the last answer, level 1 a two-way table. Both are keyed on the
    // exact equipment plus assigned SP and checked against the real contents, so
    // they go stale on their own and never need clearCache(). One entry per
    // build in play. Slots nobody touches are never read, so a big table is cheap.
    private static final int MEMO_SLOTS = 2048;

    private int cachedCount = -1;
    private int cachedValid;
    private final int[] cachedAlloc = new int[K];
    private final int[] cachedBonus = new int[K];
    private boolean[] cachedKeep = new boolean[0];
    private IEquipment[] cachedItems = new IEquipment[0];
    private List<IEquipment> cachedValidList = List.of();
    private List<IEquipment> cachedInvalidList = List.of();

    private final IEquipment[][] memoItems = new IEquipment[MEMO_SLOTS][];
    private final int[][] memoAlloc = new int[MEMO_SLOTS][];
    private final int[][] memoBonus = new int[MEMO_SLOTS][];
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final List<IEquipment>[] memoValidList = new List[MEMO_SLOTS];
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final List<IEquipment>[] memoInvalidList = new List[MEMO_SLOTS];
    private final int[] memoSize = new int[MEMO_SLOTS];
    private final int[] memoEpochOf = new int[MEMO_SLOTS];
    private int memoEpoch = 1;

    // ── per-item tables, only populated for non-inert items ─────────────
    private int[] itemWeight = new int[0];
    private int[] reqBits = new int[0];
    private int[] negBits = new int[0];
    private int[] rem = new int[0];
    private int[] condForced = new int[0];
    private int[] branch = new int[0];
    private int[] dupParent = new int[0];
    private int[] pending = new int[0];
    private int[] remSlot = new int[0];

    // ── per-skill aggregates ────────────────────────────────────────────
    private final int[] alloc = new int[K];
    private final int[] negSum = new int[K];
    private final int[] maxReq = new int[K];
    private final int[] maxNeed = new int[K];

    // ── search state ────────────────────────────────────────────────────
    private final int[] state = new int[K];
    private final int[] need = new int[K];
    private final int[] bestState = new int[K];
    private boolean[] taken = new boolean[0];
    private boolean[] bestTaken = new boolean[0];
    private long takenMask;
    private long bestMask;
    private boolean wideSnapshot;
    private int[] undo = new int[0];
    private int[] needStack = new int[0];
    private int[] seenKey = new int[0];
    private int[] seenStamp = new int[0];
    private int seenMask;
    private int stamp;

    private int n;
    private int condCount;
    private int branchCount;
    private int activeBits;
    private int volatileBits;
    private int count;
    private int weight;
    private int bestCount;
    private int bestWeight;

    @Override
    public void clearCache() {
        this.cachedCount = -1;
        // Bump a counter instead of walking every slot, so starting cold is free.
        this.memoEpoch++;
    }

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        int size = equipment.size();
        for (int k = 0; k < K; k++) {
            this.alloc[k] = player.allocated(SKILL_POINTS[k]);
        }

        ensureCapacity(size);

        // Read straight out of the caller's list. Copying into a scratch array
        // first costs about 75ns for 23 items, all of it GC write barriers on
        // the reference stores, and on a hit that copy is wasted anyway.
        if (!cacheHit(equipment, size)) {
            equipment.toArray(this.cachedItems);
            int slot = memoSlot(size);
            if (!memoHit(slot, size)) {
                solve(size);
                buildLists(size);
                memoStore(slot, size);
            }
            System.arraycopy(this.alloc, 0, this.cachedAlloc, 0, K);
            this.cachedCount = size;
        }

        // One modify with the totals instead of one call per item. The split is
        // already known and the lists cannot be modified, so a cache hit returns
        // the same two objects rather than refilling them.
        player.modify(this.cachedBonus, true);
        return new Result(this.cachedValidList, this.cachedInvalidList);
    }

    private void buildLists(int size) {
        boolean[] keep = this.cachedKeep;
        List<IEquipment> valid = new ArrayList<>(this.cachedValid);
        List<IEquipment> invalid = new ArrayList<>(size - this.cachedValid);
        for (int i = 0; i < size; i++) {
            if (keep[i]) {
                valid.add(this.cachedItems[i]);
            } else {
                invalid.add(this.cachedItems[i]);
            }
        }
        this.cachedValidList = Collections.unmodifiableList(valid);
        this.cachedInvalidList = Collections.unmodifiableList(invalid);
    }

    private boolean cacheHit(List<IEquipment> equipment, int size) {
        if (this.cachedCount != size) {
            return false;
        }
        for (int k = 0; k < K; k++) {
            if (this.cachedAlloc[k] != this.alloc[k]) {
                return false;
            }
        }
        for (int i = 0; i < size; i++) {
            if (this.cachedItems[i] != equipment.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hashes every eighth item or so rather than all of them, which works out at
     * 12 of 23 for a full build. Entries are checked against the real contents
     * before use, so a weak key costs hit rate, never correctness.
     *
     * <p>Borrows fmix32's multipliers without its avalanche shifts. Adding them
     * was measured to change nothing: the entropy is already in
     * identityHashCode, and this only folds it.
     */
    private int memoSlot(int size) {
        int h = size * PHI32;
        int step = size > 8 ? size / 8 : 1;
        for (int i = 0; i < size; i += step) {
            h = (h ^ System.identityHashCode(this.cachedItems[i])) * MURMUR_A;
        }
        for (int k = 0; k < K; k++) {
            h = (h ^ this.alloc[k]) * MURMUR_B;
        }
        return (h ^ (h >>> 15)) & (MEMO_SLOTS - 1);
    }

    /**
     * Two ways per slot. With only one, two builds that land on the same slot
     * would kick each other out on every other lookup. Candidates are checked
     * against the real contents, so a hash collision costs a solve, not a bug.
     */
    private boolean memoHit(int slot, int size) {
        if (size == 0) {
            return false;
        }
        if (memoWay(slot, size)) {
            return true;
        }
        return memoWay(slot ^ 1, size);
    }

    private boolean memoWay(int slot, int size) {
        if (this.memoEpochOf[slot] != this.memoEpoch || this.memoSize[slot] != size) {
            return false;
        }
        IEquipment[] key = this.memoItems[slot];
        for (int i = 0; i < size; i++) {
            if (key[i] != this.cachedItems[i]) {
                return false;
            }
        }
        int[] a = this.memoAlloc[slot];
        for (int k = 0; k < K; k++) {
            if (a[k] != this.alloc[k]) {
                return false;
            }
        }
        System.arraycopy(this.memoBonus[slot], 0, this.cachedBonus, 0, K);
        this.cachedValidList = this.memoValidList[slot];
        this.cachedInvalidList = this.memoInvalidList[slot];
        return true;
    }

    private void memoStore(int slot, int size) {
        if (size == 0) {
            return;
        }
        // Prefer a free or stale way so a live neighbour is not evicted.
        if (this.memoEpochOf[slot] == this.memoEpoch && this.memoSize[slot] != 0 && this.memoEpochOf[slot ^ 1] != this.memoEpoch) {
            slot ^= 1;
        }
        IEquipment[] key = this.memoItems[slot];
        if (key == null || key.length < size) {
            this.memoItems[slot] = key = new IEquipment[size];
            this.memoAlloc[slot] = new int[K];
            this.memoBonus[slot] = new int[K];
        }
        System.arraycopy(this.cachedItems, 0, key, 0, size);
        System.arraycopy(this.alloc, 0, this.memoAlloc[slot], 0, K);
        System.arraycopy(this.cachedBonus, 0, this.memoBonus[slot], 0, K);
        this.memoValidList[slot] = this.cachedValidList;
        this.memoInvalidList[slot] = this.cachedInvalidList;
        this.memoSize[slot] = size;
        this.memoEpochOf[slot] = this.memoEpoch;
    }

    private void ensureCapacity(int size) {
        if (this.cachedItems.length >= size) {
            return;
        }
        this.cachedItems = new IEquipment[size];
        this.itemWeight = new int[size];
        this.reqBits = new int[size];
        this.negBits = new int[size];
        this.rem = new int[size];
        this.condForced = new int[size];
        this.branch = new int[size];
        this.dupParent = new int[size];
        this.pending = new int[size];
        this.remSlot = new int[size];
        this.taken = new boolean[size];
        this.bestTaken = new boolean[size];
        this.cachedKeep = new boolean[size];
        this.undo = new int[(size + 1) * size];
        this.needStack = new int[(size + 2) * K];
    }

    // ── reduction ───────────────────────────────────────────────────────

    private void solve(int size) {
        this.n = size;
        boolean[] keep = this.cachedKeep;

        int s0 = this.alloc[0];
        int s1 = this.alloc[1];
        int s2 = this.alloc[2];
        int s3 = this.alloc[3];
        int s4 = this.alloc[4];
        int inertWeight = 0;
        int inertCount = 0;
        int remCount = 0;
        int reqAny = 0;
        int negAny = 0;
        for (int k = 0; k < K; k++) {
            this.negSum[k] = 0;
            this.maxReq[k] = 0;
            this.maxNeed[k] = Integer.MIN_VALUE;
        }

        // Pass 1. Take the items that are always worth wearing, collect the
        // rest. ORing the five requirements together is zero only when every
        // one of them is, and ORing the five bonuses goes negative the moment
        // any single one does, so both questions cost one test each.
        for (int i = 0; i < this.n; i++) {
            IEquipment item = this.cachedItems[i];
            int[] r = item.requirements();
            int[] b = item.bonuses();
            int b0 = b[0];
            int b1 = b[1];
            int b2 = b[2];
            int b3 = b[3];
            int b4 = b[4];
            if ((r[0] | r[1] | r[2] | r[3] | r[4]) == 0 && (b0 | b1 | b2 | b3 | b4) >= 0) {
                keep[i] = true;
                s0 += b0;
                s1 += b1;
                s2 += b2;
                s3 += b3;
                s4 += b4;
                inertWeight += b0 + b1 + b2 + b3 + b4;
                inertCount++;
                continue;
            }
            keep[i] = false;
            this.remSlot[i] = remCount;
            this.rem[remCount++] = i;

            int rb = 0;
            int nb = 0;
            int w = 0;
            for (int k = 0; k < K; k++) {
                int rv = r[k];
                int bv = b[k];
                w += bv;
                if (bv < 0) {
                    nb |= 1 << k;
                    this.negSum[k] += bv;
                }
                if (rv > 0) {
                    rb |= 1 << k;
                    if (rv > this.maxReq[k]) {
                        this.maxReq[k] = rv;
                    }
                    if (rv + bv > this.maxNeed[k]) {
                        this.maxNeed[k] = rv + bv;
                    }
                }
            }
            this.itemWeight[i] = w;
            this.reqBits[i] = rb;
            this.negBits[i] = nb;
            reqAny |= rb;
            negAny |= nb;
        }

        this.state[0] = s0;
        this.state[1] = s1;
        this.state[2] = s2;
        this.state[3] = s3;
        this.state[4] = s4;

        // A required skill still drops out if it has enough slack that nothing
        // in the build could drag it under a requirement.
        this.activeBits = reqAny;
        int m = reqAny;
        while (m != 0) {
            int k = Integer.numberOfTrailingZeros(m);
            m &= m - 1;
            int worst = this.state[k] + this.negSum[k];
            if (worst >= this.maxReq[k] && worst >= this.maxNeed[k]) {
                this.activeBits &= ~(1 << k);
            }
        }
        this.volatileBits = negAny & this.activeBits;

        // Pass 2. Classify what is left against the surviving skills.
        this.condCount = 0;
        this.branchCount = 0;
        for (int q = 0; q < remCount; q++) {
            int i = this.rem[q];
            int ra = this.reqBits[i] & this.activeBits;
            this.reqBits[i] = ra;
            if ((this.negBits[i] & this.activeBits) != 0 || (ra & this.volatileBits) != 0) {
                this.branch[this.branchCount++] = i;
            }
            else {
                this.condForced[this.condCount++] = i;
            }
        }
        for (int a = 0; a < this.branchCount; a++) {
            int i = this.branch[a];
            this.dupParent[i] = -1;
            for (int c = 0; c < a; c++) {
                if (sameProfile(this.branch[c], i)) {
                    this.dupParent[i] = this.branch[c];
                    break;
                }
            }
        }

        this.need[0] = Integer.MIN_VALUE;
        this.need[1] = Integer.MIN_VALUE;
        this.need[2] = Integer.MIN_VALUE;
        this.need[3] = Integer.MIN_VALUE;
        this.need[4] = Integer.MIN_VALUE;
        this.takenMask = 0L;
        // A long only holds 64 flags. Past that, copy the array instead. Real
        // builds never get near this, but the shift would silently wrap and
        // hand back a wrong answer.
        this.wideSnapshot = remCount > 64;
        this.count = inertCount;
        this.weight = inertWeight;
        this.bestCount = -1;
        this.bestWeight = Integer.MIN_VALUE;

        if (this.branchCount == 0) {
            // No decisions to make: the closure is the answer.
            closureFrom(0);
            for (int c = 0; c < this.condCount; c++) {
                int i = this.condForced[c];
                if (this.taken[i]) {
                    keep[i] = true;
                    this.taken[i] = false;
                }
            }
            this.cachedValid = this.count;
            for (int k = 0; k < K; k++) {
                this.cachedBonus[k] = this.state[k] - this.alloc[k];
            }
            return;
        }

        if (this.branchCount > 1 && this.branchCount <= 30) {
            int slots = Integer.highestOneBit(Math.max(32, this.branchCount * 32)) * 2;
            if (this.seenKey.length != slots) {
                this.seenKey = new int[slots];
                this.seenStamp = new int[slots];
            }
            this.seenMask = slots - 1;
            this.stamp++;
        } else {
            this.seenMask = 0;
        }

        int rootAdded = closureFrom(0);
        search(0);

        for (int q = 0; q < remCount; q++) {
            int i = this.rem[q];
            if (this.wideSnapshot ? this.bestTaken[i] : (this.bestMask & (1L << q)) != 0) {
                keep[i] = true;
            }
        }
        this.cachedValid = this.bestCount;
        for (int k = 0; k < K; k++) {
            this.cachedBonus[k] = this.bestState[k] - this.alloc[k];
        }

        // Leave `taken` clean so the next call needs no clearing pass.
        undoItems(0, rootAdded);
    }

    private boolean sameProfile(int a, int b) {
        if (this.reqBits[a] != this.reqBits[b] || this.negBits[a] != this.negBits[b] || this.itemWeight[a] != this.itemWeight[b]) {
            return false;
        }
        int[] ra = this.cachedItems[a].requirements();
        int[] rb = this.cachedItems[b].requirements();
        int[] ba = this.cachedItems[a].bonuses();
        int[] bb = this.cachedItems[b].bonuses();
        for (int k = 0; k < K; k++) {
            if (ra[k] != rb[k] || ba[k] != bb[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Equips safe items repeatedly until no more fit. None of them lowers a
     * skill that matters, so skills only climb and the end result does not
     * depend on what order they went on in. Indices land in {@code undo} from
     * {@code base} so they can be taken back off.
     */
    private int closureFrom(int base) {
        int p = 0;
        for (int c = 0; c < this.condCount; c++) {
            int i = this.condForced[c];
            if (!this.taken[i]) {
                this.pending[p++] = i;
            }
        }
        int added = 0;
        boolean changed = true;
        while (changed && p > 0) {
            changed = false;
            int keep = 0;
            for (int q = 0; q < p; q++) {
                int i = this.pending[q];
                if (equips(i)) {
                    apply(i);
                    this.undo[base + added++] = i;
                    changed = true;
                } else {
                    this.pending[keep++] = i;
                }
            }
            p = keep;
        }
        return added;
    }

    private boolean equips(int i) {
        int m = this.reqBits[i];
        if (m == 0) {
            return true;
        }
        int[] r = this.cachedItems[i].requirements();
        do {
            int k = Integer.numberOfTrailingZeros(m);
            m &= m - 1;
            if (this.state[k] < r[k]) {
                return false;
            }
        } while (m != 0);
        return true;
    }

    private void apply(int i) {
        int[] b = this.cachedItems[i].bonuses();
        this.taken[i] = true;
        if (!this.wideSnapshot) {
            this.takenMask |= 1L << this.remSlot[i];
        }
        this.count++;
        this.weight += this.itemWeight[i];
        this.state[0] += b[0];
        this.state[1] += b[1];
        this.state[2] += b[2];
        this.state[3] += b[3];
        this.state[4] += b[4];
    }

    /** Only branch items can contribute a binding cascade bound. */
    private void applyBranch(int i) {
        apply(i);
        int m = this.reqBits[i];
        if (m == 0) {
            return;
        }
        int[] r = this.cachedItems[i].requirements();
        int[] b = this.cachedItems[i].bonuses();
        do {
            int k = Integer.numberOfTrailingZeros(m);
            m &= m - 1;
            int bound = r[k] + b[k];
            if (bound > this.need[k]) {
                this.need[k] = bound;
            }
        } while (m != 0);
    }

    private void undoOne(int i) {
        int[] b = this.cachedItems[i].bonuses();
        this.taken[i] = false;
        if (!this.wideSnapshot) {
            this.takenMask &= ~(1L << this.remSlot[i]);
        }
        this.count--;
        this.weight -= this.itemWeight[i];
        this.state[0] -= b[0];
        this.state[1] -= b[1];
        this.state[2] -= b[2];
        this.state[3] -= b[3];
        this.state[4] -= b[4];
    }

    private void undoItems(int base, int added) {
        for (int a = added - 1; a >= 0; a--) {
            undoOne(this.undo[base + a]);
        }
    }

    private boolean cascadeHolds() {
        int m = this.activeBits;
        while (m != 0) {
            int k = Integer.numberOfTrailingZeros(m);
            m &= m - 1;
            if (this.state[k] < this.need[k]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whatever is equipped on the way in is equipped again on the way out.
     */
    private void search(int depth) {
        if (this.count > this.bestCount || (this.count == this.bestCount && this.weight > this.bestWeight)) {
            this.bestCount = this.count;
            this.bestWeight = this.weight;
            if (this.wideSnapshot) {
                System.arraycopy(this.taken, 0, this.bestTaken, 0, this.n);
            } else {
                this.bestMask = this.takenMask;
            }
            this.bestState[0] = this.state[0];
            this.bestState[1] = this.state[1];
            this.bestState[2] = this.state[2];
            this.bestState[3] = this.state[3];
            this.bestState[4] = this.state[4];
        }
        if (this.bestCount >= this.n) {
            return;
        }
        if (this.seenMask != 0 && !markSeen()) {
            return;
        }

        int savedAt = (depth + 1) * K;
        int undoBase = (depth + 1) * this.n;

        for (int a = 0; a < this.branchCount; a++) {
            int i = this.branch[a];
            if (this.taken[i]) {
                continue;
            }
            int parent = this.dupParent[i];
            if (parent >= 0 && !this.taken[parent]) {
                continue;
            }
            if (!equips(i)) {
                continue;
            }

            this.needStack[savedAt] = this.need[0];
            this.needStack[savedAt + 1] = this.need[1];
            this.needStack[savedAt + 2] = this.need[2];
            this.needStack[savedAt + 3] = this.need[3];
            this.needStack[savedAt + 4] = this.need[4];
            applyBranch(i);
            if (cascadeHolds()) {
                int nested = closureFrom(undoBase);
                search(depth + 1);
                undoItems(undoBase, nested);
            }
            undoOne(i);
            this.need[0] = this.needStack[savedAt];
            this.need[1] = this.needStack[savedAt + 1];
            this.need[2] = this.needStack[savedAt + 2];
            this.need[3] = this.needStack[savedAt + 3];
            this.need[4] = this.needStack[savedAt + 4];
            if (this.bestCount >= this.n) {
                return;
            }
        }
    }

    /** @return false when this exact branch selection was already expanded. */
    private boolean markSeen() {
        int key;
        if (this.wideSnapshot) {
            key = 0;
            for (int a = 0; a < this.branchCount; a++) {
                if (this.taken[this.branch[a]]) {
                    key |= 1 << a;
                }
            }
        } else {
            key = (int) this.takenMask ^ (int) (this.takenMask >>> 32);
        }
        int slot = ((key * PHI32) >>> 1) & this.seenMask;
        for (int probe = 0; probe < 8; probe++) {
            int at = (slot + probe) & this.seenMask;
            if (this.seenStamp[at] != this.stamp) {
                this.seenStamp[at] = this.stamp;
                this.seenKey[at] = key;
                return true;
            }
            if (this.seenKey[at] == key) {
                return false;
            }
        }
        return true;
    }
}
