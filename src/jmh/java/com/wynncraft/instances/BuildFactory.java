package com.wynncraft.instances;

import com.wynncraft.AlgorithmRegistry;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

public record BuildFactory(IEquipment[] equipment, int[] assignedSkillpoints) {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    public BuildFactory {
        assert assignedSkillpoints.length == SKILL_POINTS.length;
    }

    /**
     * Creates a player for this build specification
     *
     * @param entry the algorithm entry
     * @return the resulting player build
     */
    public IPlayer create(AlgorithmRegistry.Entry entry) {
        IPlayerBuilder builder = entry.builder();
        {
            apply(builder);
        }
        return builder.build();
    }

    /**
     * Applies this build to the provided player build
     * @param builder the builder to include it
     */
    public void apply(IPlayerBuilder builder) {
        for (int i = 0; i < SKILL_POINTS.length; i++) {
            int amount = assignedSkillpoints[i];
            if (amount == 0) {
                continue;
            }

            builder.allocate(SKILL_POINTS[i], amount);
        }

        builder.equipment(equipment);
    }

}
