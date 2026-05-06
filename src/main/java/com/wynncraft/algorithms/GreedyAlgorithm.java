package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.List;

@Information(name = "GreedyAlgorithm", version = 1, authors = {"EpicPuppy613"})
public class GreedyAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    @Override
    public Result run(WynnPlayer player) {
        List<IEquipment> equipment = player.equipment();
        IEquipment[] items = equipment.toArray(new IEquipment[0]);
        int[] assignedSP = new int[SKILL_POINTS.length];
        for (int i = 0; i < SKILL_POINTS.length; i++) {
            assignedSP[i] = player.allocated(SKILL_POINTS[i]);
        }

        boolean[] keep = check(items, assignedSP);

        List<IEquipment> valid = new ArrayList<>();
        List<IEquipment> invalid = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            if (keep[i]) {
                valid.add(items[i]);
                player.modify(items[i].bonuses(), true);
            } else {
                invalid.add(items[i]);
            }
        }
        return new Result(valid, invalid);
    }

    private boolean meetsReqs(int[] sp, int[] reqs) {
        return (sp[0] >= reqs[0] || reqs[0] == 0) &&
                (sp[1] >= reqs[1] || reqs[1] == 0) &&
                (sp[2] >= reqs[2] || reqs[2] == 0) &&
                (sp[3] >= reqs[3] || reqs[3] == 0) &&
                (sp[4] >= reqs[4] || reqs[4] == 0);
    }

    private int[] applyBonuses(int[] sp, int[] bonuses) {
        return new int[]{
                sp[0] + bonuses[0],
                sp[1] + bonuses[1],
                sp[2] + bonuses[2],
                sp[3] + bonuses[3],
                sp[4] + bonuses[4]
        };
    }

    private int[] updateMinimums(int[] minimums, IEquipment item) {
        int[] requirements = item.requirements();
        int[] bonuses = item.bonuses();
        int[] reqs = {requirements[0], requirements[1], requirements[2], requirements[3], requirements[4]};
        for (int s = 0; s < 5; s++) {
            if (requirements[s] > 0 && bonuses[s] > 0) {
                reqs[s] += bonuses[s];
            }
        }
        return new int[]{
                Math.max(minimums[0], reqs[0]),
                Math.max(minimums[1], reqs[1]),
                Math.max(minimums[2], reqs[2]),
                Math.max(minimums[3], reqs[3]),
                Math.max(minimums[4], reqs[4])
        };
    }

    private boolean[] check(IEquipment[] items, int[] assignedSkillpoints) {
        boolean[] output = new boolean[items.length];
        boolean[] hasNegative = new boolean[items.length];
        int[] sp = {assignedSkillpoints[0], assignedSkillpoints[1], assignedSkillpoints[2], assignedSkillpoints[3], assignedSkillpoints[4]};
        int[] minimums = {0, 0, 0, 0, 0};
        // Input pre-processing
        for (int i = 0; i < items.length; i++) {
            IEquipment item = items[i];
            int[] requirements = item.requirements();
            int[] bonuses = item.bonuses();
            boolean hasRequirements = false;
            for (int s = 0; s < 5; s++) {
                if (requirements[s] > 0) {
                    hasRequirements = true;
                    break;
                }
            }
            hasNegative[i] = false;
            for (int s = 0; s < 5; s++) {
                if (bonuses[s] < 0) {
                    hasNegative[i] = true;
                    break;
                }
            }
            if (!hasNegative[i] && !hasRequirements) {
                output[i] = true;
                sp = applyBonuses(sp, bonuses);
            } else {
                output[i] = false;
            }
        }

        while (true) {
            int best = -1;
            int bestScore = -1;
            for (int i = 0; i < items.length; i++) {
                if (output[i]) continue;
                IEquipment item = items[i];
                if (meetsReqs(sp, item.requirements())) {
                    int[] post = applyBonuses(sp, item.bonuses());
                    if (!meetsReqs(post, minimums)) continue;
                    int score = 0;
                    for (int j = 0; j < items.length; j++) {
                        if (i == j || output[j]) continue;
                        if (meetsReqs(post, items[j].requirements())) score++;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best = i;
                    }
                }
            }
            if (best == -1) break;
            output[best] = true;
            sp = applyBonuses(sp, items[best].bonuses());
            minimums = updateMinimums(minimums, items[best]);
        }

        return output;
    }
}
