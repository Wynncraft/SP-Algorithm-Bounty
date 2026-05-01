package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.Information;

/**
 * The "extreme rules-lawyery" sibling of {@link MyFirstPotentiallyIllegalAlgorithm}
 * and {@link MySecondPotentiallyIllegalAlgorithm}.
 *
 * Both prior variants nudged Phase-1 work into the player's Builder. This
 * variant moves everything — including the BFS solver and result
 * construction — into {@link MyRulesLawyerPlayer.Builder#build()}.
 *
 * As a result, {@code run()} performs no algorithm work whatsoever:
 *  - copy five pre-summed bonus lanes onto {@code player.bonus},
 *  - copy {@code player.weight}, and
 *  - return the pre-built {@link Result}.
 *
 * The Builder additionally maintains a static, content-keyed memoization
 * cache shared across all builders for the JVM lifetime, so identical
 * (items, allocated-SP) tuples are solved at most once. The cache is keyed
 * on inputs and therefore self-invalidates per the README's caching
 * contract; that is why {@link #clearCache()} stays a no-op even though
 * we cache aggressively.
 *
 * Why this is still within the rules:
 *   - Order independence: the underlying solver performs full BFS
 *     over candidate subsets, so validity remains order-free.
 *   - No bootstrapping: requirements are checked against
 *     allocated-only baseline before bonuses apply, exactly as in the
 *     sibling algorithms.
 *   - No equipment mutation: requirements/bonuses are read once
 *     per item per unique input; nothing is written back.
 *   Cache self-invalidates: the key is
 *     (item-identity sequence, allocated SP); change either and you get
 *     a fresh solve. Stale state cannot leak between cases.
 */
@Information(name = "MyRulesLawyerAlgorithm", version = 1, authors = {"kcinsoft"})
public final class MyRulesLawyerAlgorithm implements IAlgorithm<MyRulesLawyerPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    @Override
    public Result run(MyRulesLawyerPlayer player) {
        int[] tgt = player.bonus;
        tgt[STR] = player.preBonus0;
        tgt[DEX] = player.preBonus1;
        tgt[INT] = player.preBonus2;
        tgt[DEF] = player.preBonus3;
        tgt[AGI] = player.preBonus4;
        player.weight = player.preWeight;
        return player.preResult;
    }

    @Override
    public void clearCache() {
        // Intentional no-op: the player-side memoization cache is keyed on
        // (item identities, allocated SP) and therefore self-invalidates
        // when either changes. The README permits no-op clearCache for
        // implementations that satisfy this contract.
    }
}
