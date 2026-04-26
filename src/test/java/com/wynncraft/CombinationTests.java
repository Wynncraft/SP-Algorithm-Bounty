package com.wynncraft;

import com.wynncraft.combination.BeforeCombination;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.combination.CombinationTest;
import com.wynncraft.enums.Equipment;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.*;

class CombinationTests {

    @BeforeCombination
    public void loadItems() {
        // Loads all items into the cache so they are
        // available before the tests start, this call
        // is completely useless, its just so the compiler
        // doesn't delete the unused code lol
        Equipment galesForce = Equipment.GALES_FORCE;
        System.out.println("Intializing combination tests, pre-loading item data");
        System.out.println("Loaded Gale's: " + Arrays.toString(galesForce.requirements()));
    }

    @CombinationTest
    public void invalid_requirement_simple(IAlgorithm algorithm, IPlayerBuilder builder) {
        // Equipment that has requirements without the having the requirement
        {
            builder.equipment(Equipment.GALES_FORCE);
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result, Equipment.GALES_FORCE);
    }

    @CombinationTest
    public void under_requirement_simple(IAlgorithm algorithm, IPlayerBuilder builder) {
        // Equipment that has requirements while ALMOST having the exactly requirement
        {
            builder.allocate(SkillPoint.AGILITY, 59);
            builder.equipment(Equipment.GALES_FORCE);
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result, Equipment.GALES_FORCE);
    }

    @CombinationTest
    public void valid_requirement_simple(IAlgorithm algorithm, IPlayerBuilder builder) {
        // Equipment that has requirements while having exactly the requirement
        {
            builder.allocate(SkillPoint.AGILITY, 60);
            builder.equipment(Equipment.GALES_FORCE);
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result, Equipment.GALES_FORCE);
        assertInvalid(result);
    }

    @CombinationTest
    public void over_requirement_simple(IAlgorithm algorithm, IPlayerBuilder builder) {
        // Equipment that has requirements while having exactly the requirement
        {
            builder.allocate(SkillPoint.AGILITY, 61);
            builder.equipment(Equipment.GALES_FORCE);
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result, Equipment.GALES_FORCE);
        assertInvalid(result);
    }

    @CombinationTest
    public void bootstrap_1(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.AGILITY, 84);
            builder.equipment(
                Equipment.DIAMOND_STEAM_RING,
                Equipment.DIAMOND_STEAM_RING,
                Equipment.DIAMOND_STEAM_BRACELET,
                Equipment.DIAMOND_STEAM_NECKLACE
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.DIAMOND_STEAM_RING,
            Equipment.DIAMOND_STEAM_RING,
            Equipment.DIAMOND_STEAM_BRACELET,
            Equipment.DIAMOND_STEAM_NECKLACE
        );
    }

    @CombinationTest
    public void bootstrap_2(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 40);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.equipment(
                Equipment.CRYSTAL_COIL,
                Equipment.PHAGE_PINS
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.CRYSTAL_COIL,
            Equipment.PHAGE_PINS
        );
    }

    @CombinationTest
    public void bootstrap_3(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.AGILITY, 45);
            builder.equipment(
                Equipment.SOARFAE,
                Equipment.ALBEDO
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.SOARFAE,
            Equipment.ALBEDO
        );
    }

    @CombinationTest
    public void bootstrap_4(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 37);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.SHATTERGLASS
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.BRAINWASH,
            Equipment.SHATTERGLASS
        );
    }

    @CombinationTest
    public void bootstrap_self(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.AGILITY, 95);
            builder.equipment(
                Equipment.DIAMOND_STEAM_RING
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.DIAMOND_STEAM_RING
        );
    }

    @CombinationTest
    public void greedy_trap_1(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 45);
            builder.allocate(SkillPoint.DEXTERITY, 45);
            builder.allocate(SkillPoint.INTELLIGENCE, 48);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.equipment(
                Equipment.GRANITIC_METTLE,
                Equipment.RATION,
                Equipment.REPURPOSED_VESSELS
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result,
            Equipment.GRANITIC_METTLE,
            Equipment.RATION,
            Equipment.REPURPOSED_VESSELS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void greedy_trap_2(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 43);
            builder.allocate(SkillPoint.INTELLIGENCE, 45);
            builder.equipment(
                Equipment.SEISMOSOUL,
                Equipment.SHATTERGLASS
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.SEISMOSOUL,
            Equipment.SHATTERGLASS
        );
    }

    @CombinationTest
    public void greedy_trap_3(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 32);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.allocate(SkillPoint.INTELLIGENCE, 90);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.SAPPHIRE,
                Equipment.SHATTERGLASS
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result);
        assertInvalid(result,
            Equipment.BRAINWASH,
            Equipment.SAPPHIRE,
            Equipment.SHATTERGLASS
        );
    }

    @CombinationTest
    public void full_check_1(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 48);
            builder.allocate(SkillPoint.DEXTERITY, 47);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.allocate(SkillPoint.AGILITY, 60);
            builder.equipment(
                Equipment.DARKSTEEL_FULL_HELM,
                Equipment.DARKSTEEL_CENTRIFUGE,
                Equipment.EARTH_BREAKER,
                Equipment.DAWNBREAK,
                Equipment.BINDING_BRACE,
                Equipment.DAREDEVIL
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result,
            Equipment.DARKSTEEL_FULL_HELM,
            Equipment.DARKSTEEL_CENTRIFUGE,
            Equipment.EARTH_BREAKER,
            Equipment.DAWNBREAK,
            Equipment.BINDING_BRACE,
            Equipment.DAREDEVIL
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void negative_enables_positive(IAlgorithm algorithm, IPlayerBuilder builder) {
        // Repurposed Vessels grants +30 STR with negatives elsewhere; in
        // isolation its DEX/DEF requirements are met by allocation, and
        // its post-application state still keeps Vessels' own isValid
        // satisfied (DEX 42 ≥ 45-3, DEF 42 ≥ 45-3). Granitic Mettle
        // requires STR=75 which the player does not have until Vessels'
        // +30 STR bonus is applied; Mettle in turn adds +10 DEF which
        // strengthens Vessels' isValid further. Both items are valid.
        {
            builder.allocate(SkillPoint.STRENGTH, 45);
            builder.allocate(SkillPoint.DEXTERITY, 45);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.equipment(
                Equipment.GRANITIC_METTLE,
                Equipment.REPURPOSED_VESSELS
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result,
            Equipment.GRANITIC_METTLE,
            Equipment.REPURPOSED_VESSELS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void cascade_blocks_self_decay(IAlgorithm algorithm, IPlayerBuilder builder) {
        // ACIDOSIS requires STR=60 and gives no bonus; player has STR=60
        // exactly. ACHROMATIC_GLOOM has no requirements but applies -3 to
        // every skill. Adding GLOOM after ACIDOSIS pushes STR to 57, which
        // breaks ACIDOSIS' isValid (state 57 < req 60 + bonus 0). The
        // cascade rule therefore rejects equipping both. Best valid set is
        // {ACIDOSIS} on a weight tiebreak (5 × 0 vs 5 × -3 = -15).
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.equipment(
                Equipment.ACIDOSIS,
                Equipment.ACHROMATIC_GLOOM
            );
        }
        IAlgorithm.Result result = algorithm.run(builder.build());
        assertValid(result, Equipment.ACIDOSIS);
        assertInvalid(result, Equipment.ACHROMATIC_GLOOM);
    }

    /**
     * Asserts all the provided equipment are inside the expected list
     *
     * @param result the result to test for
     * @param expected the expected valid equipment
     */
    private static void assertValid(IAlgorithm.Result result, IEquipment... expected) {
        assertThat(expected)
            .containsExactlyInAnyOrderElementsOf(result.valid());
    }

    /**
     * Asserts all provided equipment are inside the expected list
     *
     * @param result the result to test for
     * @param expected the expected invalid equipment
     */
    private static void assertInvalid(IAlgorithm.Result result, IEquipment... expected) {
        assertThat(expected)
            .containsExactlyInAnyOrderElementsOf(result.invalid());
    }


}
