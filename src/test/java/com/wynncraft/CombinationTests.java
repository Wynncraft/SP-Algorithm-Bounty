package com.wynncraft;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.core.CombinationTest;
import com.wynncraft.enums.Equipment;
import com.wynncraft.enums.SkillPoint;
import org.junit.jupiter.api.Tag;

import static com.wynncraft.core.EquipmentAssertions.*;

@Tag("upstream")
class CombinationTests {

    @CombinationTest
    public void invalid_requirement_simple(IAlgorithm algorithm, IPlayerBuilder builder) {
        // Equipment that has requirements without the having the requirement
        {
            builder.equipment(Equipment.GALES_FORCE);
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 0);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 59);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 60);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 61);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 84);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 40, 0, 0, 45, 0);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 45);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 37, 70, 0, 0, 0);
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
                Equipment.DIAMOND_STEAM_RING,
                Equipment.DIAMOND_STEAM_RING
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 0, 0, 0, 0, 95);
        assertValid(result);
        assertInvalid(result,
            Equipment.DIAMOND_STEAM_RING,
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 75, 42, 55, 52, -3);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 43, 0, 45, 0, 0);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 32, 70, 90, 0, 0);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 67, 68, -35, 68, 60);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 75, 42, -3, 52, -3);
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
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertSkillPoints(player, 60, 0, 0, 0, 0);
        assertValid(result, Equipment.ACIDOSIS);
        assertInvalid(result, Equipment.ACHROMATIC_GLOOM);
    }

}
