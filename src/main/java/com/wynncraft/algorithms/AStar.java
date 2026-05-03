package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.Equipment;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 
 */
@Information(name = "AStar", version = 1, authors = { "Tapu_Zuko" })
public class AStar implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    // admissable or consistent heuristic for A*
    public void scoreHeuristic(Node node) {
        node.f_scores = node.g_scores;

        // int[] heuristicAssign = node.assigned_sp.clone();
        // int[] heuristicReqs = node.reqs.clone();
        // int[] heuristicBonus = node.bonus_sp.clone();

        // // assume all bonuses apply and get minimum reqs for everything
        // for (int i = 0; i < node.value.length; i++) {
        //     if (node.value[i]) continue;
        //     updateReqs(heuristicReqs, equipment[i].requirements());
        //     updateBonusIgnoreNeg(heuristicBonus, equipment[i].bonuses());
        // }

        // for (int i = 0; i < 5; i++) {
        //     if (heuristicReqs[i] > 0 && 
        //         heuristicReqs[i] > heuristicAssign[i] + heuristicBonus[i]) {
        //         heuristicAssign[i] = heuristicReqs[i] - heuristicBonus[i];
        //     }
        // }

        // long[] diff = new long[5];
        // for (int i = 0; i < 5; i++) {
        //     diff[i] = heuristicAssign[i] - maxSp[i]; // negative with 0 being exact
        //     if (diff[i] > 0) {
        //         node.f_scores = 1;
        //         return;
        //     }
        // }
        // node.f_scores = scoreDiff(diff);
    }


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

    static int[] maxSp;
    static IEquipment[] equipment;
    static Node[] solutions;

    private boolean[] check(IEquipment[] items, int[] buildSkillpoints) {
        maxSp = buildSkillpoints;
        equipment = items;
        solutions = new Node[equipment.length + 1];

        Node start = new Node(new boolean[equipment.length], Long.MIN_VALUE,
                new int[] { -1000, -1000, -1000, -1000, -1000 }, new int[5], new int[5], 0);
        AstarSearch(start);

        Node endPoint = start;
        for (int i = 0; i < solutions.length; i++) {
            if (solutions[i] != null) {
                endPoint = solutions[i];
            }
        }

        return endPoint.value;
    }

    public static void AstarSearch(Node source) {

        PriorityQueue<Node> queue = new PriorityQueue<Node>(20,
                new Comparator<Node>() {
                    // override compare method
                    public int compare(Node i, Node j) {
                        if (i.f_scores > j.f_scores) {
                            return 1;
                        } else if (i.f_scores < j.f_scores) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                });

        // cost from start
        source.g_scores = Long.MIN_VALUE;
        source.f_scores = Long.MIN_VALUE;

        queue.add(source);

        boolean found = false;

        while ((!queue.isEmpty()) && (!found)) {

            // the node in having the lowest f_score value
            Node current = queue.poll();

            if (current.g_scores <= 0)
                if (solutions[current.itemCount] == null ||
                        bonusWeight(solutions[current.itemCount]) < bonusWeight(current)) {
                    solutions[current.itemCount] = current;
                }

            // only invalid possibilities left
            if (current.f_scores >= 1) {
                return;
            }
            // goal found
            if (current.itemCount == equipment.length) {
                return;
            }

            // check every child of current node
            for (Node child : current.adjacencies()) {
                queue.add(child);
            }

        }

        return;

    }

    class Node {

        public final int itemCount;
        public final boolean[] value;
        public final int[] reqs;
        public final int[] assigned_sp;
        public final int[] bonus_sp;

        public boolean equals(Node other) {
            return itemCount == other.itemCount &&
                    value.equals(other.value) &&
                    reqs.equals(other.reqs) &&
                    assigned_sp.equals(other.assigned_sp) &&
                    bonus_sp.equals(other.bonus_sp);
        }

        public long g_scores;
        public long h_scores;
        public long f_scores;

        public List<Node> adjacencies() {
            List<Node> nodes = new ArrayList<>(value.length);
            for (int i = 0; i < value.length; i++) {
                if (value[i])
                    continue;

                boolean[] newVal = value.clone();
                newVal[i] = true;
                int[] nextReqs = reqs.clone();
                int[] nextAssigned = assigned_sp.clone();
                int[] nextBonus = bonus_sp.clone();

                long nextG = scoreAddition(nextReqs, nextAssigned, nextBonus, equipment[i], value);
                Node next = new Node(newVal, nextG, nextReqs, nextAssigned, nextBonus, itemCount + 1);
                scoreHeuristic(next);
                nodes.add(next);

            }

            return nodes;
        }

        public Node(boolean[] val, long gVal, int[] r, int[] s, int[] b, int c) {
            value = val;
            g_scores = gVal;
            reqs = r;
            assigned_sp = s;
            bonus_sp = b;
            itemCount = c;
        }

        public String toString() {
            String out = "Req: ";
            for (int i = 0; i < 5; i++) {
                out += reqs[i] + ", ";
            }
            out += "Bonus: ";
            for (int i = 0; i < 5; i++) {
                out += bonus_sp[i] + ", ";
            }
            out += "Assigned: ";
            for (int i = 0; i < 5; i++) {
                out += assigned_sp[i] + ", ";
            }
            return out;
        }
    }

    public static long scoreAddition(int[] currReqs, int[] currSpAsign, int[] currBonus, IEquipment newItem,
            boolean[] oldValue) {
        int[] newReqs = newItem.requirements();
        int[] newBonus = newItem.bonuses();

        // if invalid return greater than 0 for overshooting reqs
        for (int i = 0; i < 5; i++) {
            if (newReqs[i] > 0 && newReqs[i] > maxSp[i] + currBonus[i]) {
                return 1;
            }
            // if (currReqs[i] > 0 && newBonus[i] < 0 && currReqs[i] > maxSp[i] + currBonus[i] + newBonus[i]) {
            //     return 1;
            // }
            // check each item for validity when held out for negative bonus
            if (currReqs[i] > 0 && newBonus[i] < 0) {
                for (int j = 0; j < oldValue.length; j++) {
                    if (!oldValue[j])
                        continue;
                    IEquipment holdout = equipment[j];
                    if (holdout.requirements()[i] <= 0) continue;
                    int holdoutBonus = currBonus[i] - holdout.bonuses()[i];
                    if (holdout.requirements()[i] > maxSp[i]
                            + holdoutBonus
                            + newBonus[i]) {
                        return 1;
                    }
                }
            }
        }

        // item is valid to activate
        for (int i = 0; i < 5; i++) {
            
            if (newReqs[i] > 0 && newReqs[i] > currSpAsign[i] + currBonus[i]) {
                currSpAsign[i] = newReqs[i] - currBonus[i];
            }
            // check every item for a negative bonus
            if (newBonus[i] < 0) {
                // int spToAdd = 0;
                if (newReqs[i] > 0 && newReqs[i] > currSpAsign[i] + currBonus[i]) {
                    currSpAsign[i] = newReqs[i] - currBonus[i];
                }
                for (int j = 0; j < oldValue.length; j++) {
                    if (!oldValue[j])
                        continue;
                    IEquipment holdout = equipment[j];
                    if (holdout.requirements()[i] <= 0) continue;
                    int holdoutBonus = currBonus[i] - holdout.bonuses()[i];
                    if (holdout.requirements()[i] > currSpAsign[i]
                            + holdoutBonus
                            + newBonus[i]) {
                        int requiredSp = holdout.requirements()[i] - holdoutBonus - newBonus[i];
                        currSpAsign[i] = Math.max(requiredSp, currSpAsign[i]);
                    }
                }
                // currSpAsign[i] += spToAdd;
                if (currSpAsign[i] > maxSp[i])
                    assert false;
            }
            
        }
        updateReqs(currReqs, newReqs);
        updateBonus(currBonus, newBonus);

        return score(currSpAsign, maxSp);
    }

    private static long score(int[] currSpAsign, int[] maxSp) {
        long[] diff = new long[5];
        for (int i = 0; i < 5; i++) {
            diff[i] = currSpAsign[i] - maxSp[i]; // negative with 0 being exact
        }
        return scoreDiff(diff);
    }

    private static long scoreDiff(long[] diff) {
        Arrays.sort(diff); // place in order of largest diff to smallest diff
        long score = 0;
        for (int i = 0; i < 5; i++) {
            // may fail for differences between sp and requirements larger than 1024
            // smallest diff is most significant value
            score -= (-1 * diff[i]) << (10l * i);
        }
        return score;
    }

    private static void updateReqs(int[] reqs, int[] newReqs) {
        for (int i = 0; i < 5; i++) {
            if (newReqs[i] != 0) {
                reqs[i] = Math.max(reqs[i], newReqs[i]);
            }
        }
    }

    private static void updateBonus(int[] bonus, int[] newBonus) {
        for (int i = 0; i < 5; i++) {
            bonus[i] += newBonus[i];
        }
    }

    private static void updateBonusIgnoreNeg(int[] bonus, int[] newBonus) {
        for (int i = 0; i < 5; i++) {
            if (newBonus[i] > 0) 
                bonus[i] += newBonus[i];
            
            if (bonus[i] < 0)
                bonus[i] = 0;
        }
    }

    private static int bonusWeight(Node item) {
        int sum = 0;
        int[] bonuses = item.bonus_sp;
        for (int b : bonuses)
            sum += b;
        return sum;
    }
}
