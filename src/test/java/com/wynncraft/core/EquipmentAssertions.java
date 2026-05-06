package com.wynncraft.core;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.enums.SkillPoint;

import static org.assertj.core.api.Assertions.assertThat;

public class EquipmentAssertions {

    /**
     * Asserts all the provided equipment are inside the expected list
     *
     * @param result the result to test for
     * @param expected the expected valid equipment
     */
    public static void assertValid(IAlgorithm.Result result, IEquipment... expected) {
        assertThat(expected)
            .containsExactlyInAnyOrderElementsOf(result.valid());
    }

    /**
     * Asserts all provided equipment are inside the expected list
     *
     * @param result the result to test for
     * @param expected the expected invalid equipment
     */
    public static void assertInvalid(IAlgorithm.Result result, IEquipment... expected) {
        assertThat(expected)
            .containsExactlyInAnyOrderElementsOf(result.invalid());
    }

    /**
     * Asserts the final skill point count for the player
     *
     * @param player the player to assert for
     * @param strength the strength value
     * @param dexterity the dexterity value
     * @param intelligence the intelligence value
     * @param defence the defence value
     * @param agility the agility value
     */
    public static void assertSkillPoints(IPlayer player, int strength, int dexterity, int intelligence, int defence, int agility) {
        assertThat(player.total(SkillPoint.STRENGTH))
            .describedAs("Strength")
            .isEqualTo(strength);
        assertThat(player.total(SkillPoint.DEXTERITY))
            .describedAs("Dexterity")
            .isEqualTo(dexterity);
        assertThat(player.total(SkillPoint.INTELLIGENCE))
            .describedAs("Intelligence")
            .isEqualTo(intelligence);
        assertThat(player.total(SkillPoint.DEFENCE))
            .describedAs("Defence")
            .isEqualTo(defence);
        assertThat(player.total(SkillPoint.AGILITY))
            .describedAs("Agility")
            .isEqualTo(agility);
    }

}
