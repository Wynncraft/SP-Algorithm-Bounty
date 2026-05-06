package com.wynncraft;

import com.wynncraft.core.CombinationTest;
import com.wynncraft.core.SyntheticEquipment;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;
import org.junit.jupiter.api.Tag;

import java.util.HashSet;

import static com.wynncraft.core.EquipmentAssertions.*;
import static org.assertj.core.api.Assertions.fail;

@Tag("curated")
class SyntheticCombinationTests {

    @CombinationTest
    public void case1_optimal(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 68);
            builder.allocate(SkillPoint.DEFENCE, 33);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case1_subopt_assign(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 59);
            builder.allocate(SkillPoint.DEFENCE, 43);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case1_tff(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 59);
            builder.allocate(SkillPoint.DEFENCE, 40);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1
        );
        assertInvalid(result,
            item2,
            item3
        );
    }

    @CombinationTest
    public void case2_strictChain_abc(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 1, 0, 0, 0, 0 }, new int[] { 0, 2, -1, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 2, 0, 0, 0 }, new int[] { 0, 0, 1, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 0, 1, 0, 0 }, new int[] { 0, 0, 0, 1, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 1);
            builder.allocate(SkillPoint.INTELLIGENCE, 1);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case2_strictChain_cab(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 0, 1, 0, 0 }, new int[] { 0, 0, 0, 1, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 1, 0, 0, 0, 0 }, new int[] { 0, 2, -1, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 2, 0, 0, 0 }, new int[] { 0, 0, 1, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 1);
            builder.allocate(SkillPoint.INTELLIGENCE, 1);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case3_noRequirements(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 5, 5, 5, 5, 5 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { -2, 0, 0, 0, 0 });
        {
            builder.equipment(
                item1,
                item2
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case4_impossibleRequirements(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 100, 0, 0, 0, 0 }, new int[] { 5, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 10);
            builder.equipment(
                item1
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result);
        assertInvalid(result,
            item1
        );
    }

    @CombinationTest
    public void case5_negativeInvalidatesPrior(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 10, 0, 0, 0, 0 }, new int[] { 5, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { -20, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 10);
            builder.equipment(
                item1,
                item2
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1
        );
        assertInvalid(result,
            item2
        );
    }

    @CombinationTest
    public void case6_exactRequirement(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 50, 30, 0, 0, 0 }, new int[] { 0, 0, 10, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 50);
            builder.allocate(SkillPoint.DEXTERITY, 30);
            builder.equipment(
                item1
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case7_mutualDependency(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 10, 0, 0, 0 }, new int[] { 10, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 10, 0, 0, 0, 0 }, new int[] { 0, 10, 0, 0, 0 });
        {
            builder.equipment(
                item1,
                item2
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result);
        assertInvalid(result,
            item1,
            item2
        );
    }

    @CombinationTest
    public void case8_fullBuild_8items(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 40, 0, 0, 40, 40 }, new int[] { 9, 0, 0, 9, 9 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 15, 0, 0, 50 }, new int[] { 0, 15, 0, 0, 25 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 30, 30, 30, 30, 30 }, new int[] { 8, 8, 8, 8, 8 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 40, 70, 0, 0, 0 }, new int[] { 13, 0, -50, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 25, 0, 0, 0, 0 }, new int[] { 5, 0, 0, 0, -3 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 25, 25, 25, 25, 25 }, new int[] { 3, 3, 3, 3, 3 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 4, 4, 4, 4, 4 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 21);
            builder.allocate(SkillPoint.DEXTERITY, 40);
            builder.allocate(SkillPoint.INTELLIGENCE, 73);
            builder.allocate(SkillPoint.DEFENCE, 28);
            builder.allocate(SkillPoint.AGILITY, 29);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case9_dexIntAgiBuild(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 40, 40, 0, 40 }, new int[] { -30, 0, 0, -30, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 50, 0, 0, 65 }, new int[] { 0, 8, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 50, 55, 0, 50 }, new int[] { 0, 6, 4, 0, 6 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 55, 0, 0, 55 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 50, 0, 0, 45 }, new int[] { 0, 4, 0, 0, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 0, 60, 0, 0, 0 }, new int[] { 0, 0, 6, 0, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 0, 45, 45, 0, 45 }, new int[] { -15, 0, 0, 0, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 45, 0, 0, 0 }, new int[] { 0, 8, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.DEXTERITY, 45);
            builder.allocate(SkillPoint.INTELLIGENCE, 49);
            builder.allocate(SkillPoint.AGILITY, 65);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case10_intAgiHeavyMageBuild(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, -80, 5, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 50, 0, 65, 0, 50 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 50, 0, 55, 0, 0 }, new int[] { 6, 0, 6, 0, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 0, 70, 0, 80 }, new int[] { -10, -10, 35, -40, 60 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 0, 55, 0, 0 }, new int[] { 0, 0, 4, 0, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 0, 0, 65, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 6, 0, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 0, 55, 0, 0 }, new int[] { 0, 0, 4, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.INTELLIGENCE, 60);
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case11_strDexDefWarriorBuild(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 0, 65, 0 }, new int[] { 0, 10, 0, 5, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 49, 31, 0, 37, 0 }, new int[] { 0, 0, 0, 0, -43 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 60, 0, 0, 70, 0 }, new int[] { 20, 0, 0, 25, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 45, 0, 0, 0 }, new int[] { 0, 6, 0, 0, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 0, 45, 0, 0, 0 }, new int[] { 0, 6, 0, 0, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 45, 0, 0, 50, 0 }, new int[] { 0, 0, 0, 7, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 50, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.DEXTERITY, 58);
            builder.allocate(SkillPoint.DEFENCE, 58);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case12_strStackingBuildWithNegativeAgi(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 90, 0, 0, 0, 0 }, new int[] { 15, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 49, 31, 0, 37, 0 }, new int[] { 0, 0, 0, 0, -43 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 65, 0, 65, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 3, 3, 0, 0, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 3, 3, 0, 0, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 100, 0, 0, 0, 0 }, new int[] { 6, 0, 0, 0, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 40, 0, 40, 0 }, new int[] { 0, 7, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 84);
            builder.allocate(SkillPoint.DEXTERITY, 52);
            builder.allocate(SkillPoint.DEFENCE, 65);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case13_intAgiMageBuildWithMoontowersLargeNegatives(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 40, 0, 40, 0, 0 }, new int[] { 7, 0, 7, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 120, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 70 }, new int[] { 0, 0, 0, 0, 5 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 0, 70, 0, 80 }, new int[] { -10, -10, 35, -40, 60 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 0, 45, 0, 45 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 0, 0, 45, 0, 45 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 50 }, new int[] { 0, 0, 0, 0, 6 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 50);
            builder.allocate(SkillPoint.INTELLIGENCE, 78);
            builder.allocate(SkillPoint.AGILITY, 69);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case14_strIntMeleeBuild(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 65, 0, 0, 0, 0 }, new int[] { 10, 0, 0, 0, -5 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 50, 0, 50, 50, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 105, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 60, 0, 60, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 0, 0, 45, 0 }, new int[] { 0, 0, 0, 4, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 4, 0, 0, 2, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 60, 0, 0, 0, 0 }, new int[] { 0, 4, 0, 0, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 96);
            builder.allocate(SkillPoint.INTELLIGENCE, 60);
            builder.allocate(SkillPoint.DEFENCE, 44);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case15_doubleDiamondHydroRings(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 });
        {
            builder.allocate(SkillPoint.INTELLIGENCE, 100);
            builder.equipment(
                item1,
                item2
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case15_doubleDiamondHydroRings_fail(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 });
        {
            builder.allocate(SkillPoint.INTELLIGENCE, 95);
            builder.equipment(
                item1,
                item2
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result);
        assertInvalid(result,
            item1,
            item2
        );
    }

    @CombinationTest
    public void case16_strStackingWithCascadingBonuses(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 90, 0, 0, 0, 0 }, new int[] { 15, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 95, 0, 0, 0, 0 }, new int[] { 10, 0, 0, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 5, 3, 0, 0, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 50, 0, 50, 0 }, new int[] { 0, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 85);
            builder.allocate(SkillPoint.DEXTERITY, 57);
            builder.allocate(SkillPoint.DEFENCE, 50);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case17_negDefBlocksChain(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 50, 0, 0, 50, 0 }, new int[] { 9, 0, 0, 8, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 59);
            builder.allocate(SkillPoint.DEFENCE, 40);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item3
        );
        assertInvalid(result,
            item1,
            item2
        );
    }

    @CombinationTest
    public void case18_multiStatAllEquip(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 45, 0, 45, 0 }, new int[] { 10, 10, 0, 10, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 50, 0, 55, 0 }, new int[] { 0, 5, -35, 5, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 65, 0, 65, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 45, 0, 0, 0, 55 }, new int[] { 0, 0, 0, 0, 5 });
        SyntheticEquipment item6 = SyntheticEquipment.of(new int[] { 45, 0, 0, 0, 55 }, new int[] { 0, 0, 0, 0, 5 });
        SyntheticEquipment item7 = SyntheticEquipment.of(new int[] { 0, 25, 0, 0, 0 }, new int[] { 0, 6, 0, 0, 0 });
        SyntheticEquipment item8 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 45 }, new int[] { 0, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 48);
            builder.allocate(SkillPoint.DEXTERITY, 47);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.allocate(SkillPoint.AGILITY, 60);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5,
                item6,
                item7,
                item8
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3,
            item4,
            item5,
            item6,
            item7,
            item8
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case19_dualRingsWithNegNecklace(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { -3, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 100);
            builder.equipment(
                item1,
                item2,
                item3
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1,
            item2,
            item3
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case20_bothDisabledInsufficientStr(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 40, 70, 0, 0, 0 }, new int[] { 13, 0, -50, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 37);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.equipment(
                item1,
                item2
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result);
        assertInvalid(result,
            item1,
            item2
        );
    }

    @CombinationTest
    public void case21_repurposedVessels(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 45, 0, 45, 0 }, new int[] { 30, -3, -3, -3, -3 });
        {
            builder.allocate(SkillPoint.DEXTERITY, 48);
            builder.allocate(SkillPoint.DEFENCE, 48);
            builder.equipment(
                item1
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case21_repurposedVessels_2(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 45, 0, 45, 0 }, new int[] { 30, -3, -3, -3, -3 });
        {
            builder.allocate(SkillPoint.DEXTERITY, 45);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.equipment(
                item1
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            item1
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void case22_double_tie(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { -15, 0, 10, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 0 }, new int[] { 10, 0, -15, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 0, 10, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 10, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        {
            builder.allocate(SkillPoint.STRENGTH, 10);
            builder.allocate(SkillPoint.INTELLIGENCE, 10);
            builder.equipment(
                item1,
                item2,
                item3,
                item4
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertExpected(result, new SyntheticEquipment[] {
            item1,
            item2,
            item3,
            item4
        },
            new boolean[] { false, true, false, true },
            new boolean[] { false, false, true, true },
            new boolean[] { true, false, true, false }
        );
    }

    /**
     * Passes if {@code result} matches any one of the {@code acceptable}
     * boolean masks (unordered). Each mask, paired with {@code items}, defines
     * the expected valid/invalid sets.
     */
    private static void assertExpected(IAlgorithm.Result result, SyntheticEquipment[] items, boolean[]... acceptable) {
        HashSet<IEquipment> actualValid = new HashSet<>(result.valid());
        HashSet<IEquipment> actualInvalid = new HashSet<>(result.invalid());

        for (boolean[] mask : acceptable) {
            HashSet<IEquipment> expectedValid = new HashSet<>();
            HashSet<IEquipment> expectedInvalid = new HashSet<>();
            for (int i = 0; i < items.length; i++) {
                if (mask[i]) {
                    expectedValid.add(items[i]);
                } else {
                    expectedInvalid.add(items[i]);
                }
            }

            if (expectedValid.size() == result.valid().size()
                    && expectedInvalid.size() == result.invalid().size()
                    && expectedValid.equals(actualValid)
                    && expectedInvalid.equals(actualInvalid)) {
                return;
            }
        }

        StringBuilder msg = new StringBuilder("result did not match any acceptable expected mask.\n");
        msg.append("  valid:   ").append(result.valid()).append('\n');
        msg.append("  invalid: ").append(result.invalid()).append('\n');
        msg.append("  acceptable masks:\n");
        for (boolean[] mask : acceptable) {
            msg.append("    ");
            for (boolean b : mask) {
                msg.append(b ? '1' : '0');
            }
            msg.append('\n');
        }
        fail(msg.toString());
    }

}
