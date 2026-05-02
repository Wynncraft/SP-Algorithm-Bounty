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
class EdgeCaseTests {

    // item1 has the lowest sp reqs but is the item to exclude.
    @CombinationTest
    public void negative_bonus(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 1, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 5, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 0, 5, 0, 0 }, new int[] { 0, 0, 0, 0, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 0, 0, 5, 0 }, new int[] { -150, 0, 0, 0, 0 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 5 }, new int[] { -150, 0, 0, 0, 0 });

            builder.allocate(SkillPoint.STRENGTH, 10);
            builder.allocate(SkillPoint.DEXTERITY, 10);
            builder.allocate(SkillPoint.INTELLIGENCE, 10);
            builder.allocate(SkillPoint.DEFENCE, 10);
            builder.allocate(SkillPoint.AGILITY, 10);
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5
            );
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);

        assertValid(result,
            item2,
            item3,
            item4,
            item5
        );
    }

    // items cyclically enable each other but not 1-1
    // Together there is enough sp, but item4 prevents item2 from being equiped
    // Item1 fixes the sp problem but it requires item4 indirectly.
    @CombinationTest
    public void largeItemCycle(IAlgorithm algorithm, IPlayerBuilder builder) {
        SyntheticEquipment item1 = SyntheticEquipment.of(new int[] { 5, 0, 0, 0, 0 }, new int[] { 0, 5, 0, 0, 0 });
        SyntheticEquipment item2 = SyntheticEquipment.of(new int[] { 0, 5, 0, 0, 0 }, new int[] { 0, 0, 5, 0, 0 });
        SyntheticEquipment item3 = SyntheticEquipment.of(new int[] { 0, 0, 5, 0, 0 }, new int[] { 0, 0, 0, 5, 0 });
        SyntheticEquipment item4 = SyntheticEquipment.of(new int[] { 0, 0, 0, 5, 0 }, new int[] { 0, -1, 0, 0, 5 });
        SyntheticEquipment item5 = SyntheticEquipment.of(new int[] { 0, 0, 0, 0, 5 }, new int[] { 5, 0, 0, 0, 0 });

            builder.allocate(SkillPoint.STRENGTH, 0);
            builder.allocate(SkillPoint.DEXTERITY, 5);
            builder.allocate(SkillPoint.INTELLIGENCE, 0);
            builder.allocate(SkillPoint.DEFENCE, 0);
            builder.allocate(SkillPoint.AGILITY, 0);
            
            builder.equipment(
                item1,
                item2,
                item3,
                item4,
                item5
            );
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);

        assertValid(result, item2, item3);
        assertInvalid(result, item1, item4, item5);
    }
}
