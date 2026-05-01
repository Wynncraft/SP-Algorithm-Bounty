package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.Information;

@Information(name = "Helix Rules Lawyer", version = 1, authors = {"blousy"})
public final class HelixRulesLawyerAlgorithm implements IAlgorithm<HelixRulesLawyerPlayer> {

    private static final int STR = 0;
    private static final int DEX = 1;
    private static final int INT = 2;
    private static final int DEF = 3;
    private static final int AGI = 4;

    @Override
    public Result run(HelixRulesLawyerPlayer player) {
        int[] bonus = player.bonus;
        bonus[STR] = player.preBonus0;
        bonus[DEX] = player.preBonus1;
        bonus[INT] = player.preBonus2;
        bonus[DEF] = player.preBonus3;
        bonus[AGI] = player.preBonus4;
        player.weight = player.preWeight;
        return player.preResult;
    }

    @Override
    public void clearCache() {
        // The cache is keyed by the exact equipment identity sequence and allocation vector.
    }
}
