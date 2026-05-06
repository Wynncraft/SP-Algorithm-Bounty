package com.wynncraft.core;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.enums.EquipmentType;
import com.wynncraft.enums.SkillPoint;

import java.util.Arrays;

public final class SyntheticEquipment implements IEquipment {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    private final EquipmentType type;
    private final int[] requirements;
    private final int[] bonuses;
    private final boolean hasNegativeBonus;

    /**
     * Creates a new instance of a synthetic equipment with the provided values
     *
     * @param type the equipment type
     * @param requirements the equipment requirements
     * @param bonuses the equipment bonuses
     * @return the resulting equipment
     */
    public static SyntheticEquipment of(EquipmentType type, int[] requirements, int[] bonuses) {
        return new SyntheticEquipment(type, requirements, bonuses);
    }

    /**
     * Creates a new instance of a synthetic equipment with the provided values
     *
     * @param requirements the equipment requirements
     * @param bonuses the equipment bonuses
     * @return the resulting equipment
     */
    public static SyntheticEquipment of(int[] requirements, int[] bonuses) {
        return new SyntheticEquipment(EquipmentType.ARMOUR, requirements, bonuses);
    }

    private SyntheticEquipment(EquipmentType type, int[] requirements, int[] bonuses) {
        this.type = type;
        this.requirements = requirements;
        this.bonuses = bonuses;
        this.hasNegativeBonus = checkNegativeBonus(bonuses);
    }

    @Override
    public EquipmentType type() {
        return type;
    }

    @Override
    public int[] requirements() {
        return requirements;
    }

    @Override
    public int[] bonuses() {
        return bonuses;
    }

    @Override
    public boolean hasNegativeBonus() {
        return hasNegativeBonus;
    }

    @Override
    public boolean canEquip(IPlayer player) {
        // Not using an enhanced for here to save allocations!
        for (int i = 0; i < requirements.length; i++) {
            int requirement = requirements[i];
            if (requirement <= 0 || player.total(SKILL_POINTS[i]) >= requirement) {
                continue;
            }

            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("%s (req: %s) (bonus: %s)", "Synthetic", Arrays.toString(requirements), Arrays.toString(bonuses));
    }

}
