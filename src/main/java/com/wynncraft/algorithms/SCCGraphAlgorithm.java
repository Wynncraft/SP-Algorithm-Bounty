package com.wynncraft.algorithms;

import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.Information;
import com.wynncraft.enums.SkillPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

@Information(name = "SCCGraphAlgorithm", version = 1, authors = {"hppeng"})
public class SCCGraphAlgorithm implements IAlgorithm<WynnPlayer> {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();
    private static final int NUM_SKILLPOINTS = 5;

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

    static class GraphNode<T> {
        T data;
        List<GraphNode<T>> parents = new ArrayList<GraphNode<T>>();
        List<GraphNode<T>> children = new ArrayList<GraphNode<T>>();
        int nodeIndex = 0;
        boolean visited = false;    // convenience, must be reset

        public GraphNode(T data) { this.data = data; }
    }


    private static <T> void addEdge(GraphNode<T> from, GraphNode<T> to) {
        from.children.add(to);
        to.parents.add(from);
    }


    private static <T> void visitForward(GraphNode<T> cur, List<GraphNode<T>> res) {
        cur.visited = true;
        for (GraphNode<T> child : cur.children) {
            if (!child.visited) { visitForward(child, res); }
        }
        res.add(cur);
    }


    private static <T> void visitBackward(GraphNode<T> cur, List<GraphNode<T>> res) {
        cur.visited = true;
        for (GraphNode<T> parent : cur.parents) {
            if (!parent.visited) { visitBackward(parent, res); }
        }
        res.add(cur);
    }


    private static <T> List<List<GraphNode<T>>> computeSCCGraph(GraphNode<T> root) {
        List<GraphNode<T>> res = new ArrayList<GraphNode<T>>();
        visitForward(root, res);
        Collections.reverse(res);
        List<List<GraphNode<T>>> sccs = new ArrayList<List<GraphNode<T>>>();
        for (GraphNode<T> node : res) {
            node.visited = false;
        }
        for (GraphNode<T> node : res) {
            if (node.visited) { continue; }
            List<GraphNode<T>> scc = new ArrayList<GraphNode<T>>();
            visitBackward(node, scc);
            sccs.add(scc);
        }
        return sccs;
    }


    static class ItemSCCResult {
        final GraphNode<IEquipment> root;
        final GraphNode<IEquipment> terminal;
        final List<List<GraphNode<IEquipment>>> sccs;
        final boolean[][] negatives;
        final long elapsedNS;

        public ItemSCCResult(GraphNode<IEquipment> root,
                             GraphNode<IEquipment> terminal,
                             List<List<GraphNode<IEquipment>>> sccs,
                             boolean[][] negatives, long elapsedNS) {
            this.root = root;
            this.terminal = terminal;
            this.sccs = sccs;
            this.negatives = negatives;
            this.elapsedNS = elapsedNS;
        }
    }


    private static void addItemEdges(GraphNode<IEquipment> from, GraphNode<IEquipment> to) {
        int[] fromBonuses = from.data.bonuses();
        int[] fromReqs = from.data.requirements();
        int[] toBonuses = to.data.bonuses();
        int[] toReqs = to.data.requirements();
        for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
            if (fromBonuses[i] > 0 && (
                    toReqs[i] > fromReqs[i]
                    || toBonuses[i] < 0
                )
            ) {
                addEdge(from, to);
                return;
            }
        }
    }


    /**
     * Break a set of items up into subgraphs based on their skill point dependency.
     *
     * @param items : List of items to process
     * @return List of subgraphs, each one being a strongly connected component of the
     *         full skill point dependency graph, in topological sort order.
     */
    private static ItemSCCResult constructSCCGraph(List<IEquipment> items) {
        long l1 = System.nanoTime();

        List<GraphNode<IEquipment>> nodes = new ArrayList<GraphNode<IEquipment>>();
        GraphNode<IEquipment> root = new GraphNode<IEquipment>(null);
        root.children = nodes;
        GraphNode<IEquipment> terminal = new GraphNode<IEquipment>(null);
        terminal.parents = nodes;

        for (int i = 0; i < items.size(); ++i) {
            GraphNode<IEquipment> g = new GraphNode<IEquipment>(items.get(i));
            g.parents.add(root);
            g.children.add(terminal);
            g.nodeIndex = i;	// Save the list index
            nodes.add(g);
            for (int j = 0; j < i; ++j) {
                addItemEdges(nodes.get(j), nodes.get(i));
                addItemEdges(nodes.get(i), nodes.get(j));
            }
        }
        List<List<GraphNode<IEquipment>>> sccs = computeSCCGraph(root);
        boolean[][] negatives = new boolean[sccs.size()][];
        for (int i = 1; i < sccs.size()-1; ++i) {
            List<GraphNode<IEquipment>> scc = sccs.get(i);
            boolean[] sccNegatives = new boolean[items.size()];
            boolean[] sccMembership = new boolean[items.size()];
            for (int j = 0; j < scc.size(); ++j) {
                IEquipment item = scc.get(j).data;
                int[] bonuses = item.bonuses();
                int itemIndex = scc.get(j).nodeIndex;
                sccMembership[itemIndex] = true;
                for (int k = 0; k < NUM_SKILLPOINTS; ++k) {
                    if (bonuses[k] < 0) {
                        sccNegatives[itemIndex] = true;
                        break;
                    }
                }
            }
            for (GraphNode<IEquipment> node : scc) {
                node.children.removeIf(n -> !sccMembership[n.nodeIndex]);
                node.parents.removeIf(n -> !sccMembership[n.nodeIndex]);
                node.visited = false;
            }
            negatives[i] = sccNegatives;
        }
        long l2 = System.nanoTime();
        return new ItemSCCResult(root, terminal, sccs, negatives, l2 - l1);
    }

    // Generate all possible full-length "DFS walks" of a graph.
    static class GraphPermutationGenerator<T> implements Iterable<GraphNode<T>[]> {
        private GraphNode<T> root;
        private int numNodes;

        GraphPermutationGenerator(List<GraphNode<T>> graph) {
            this.root = new GraphNode<T>(null);
            this.numNodes = graph.size();
            this.root.children = graph;
            for (GraphNode<T> n : graph) {
                n.visited = false;
            }
        }
        @Override
        public Iterator<GraphNode<T>[]> iterator() { return new CustomIter(); }
        private class CustomIter implements Iterator<GraphNode<T>[]> {
            boolean done = numNodes == 0;
            private int[] pointers = new int[numNodes];
            private GraphNode<T>[] stack;
            private GraphNode<T>[] buffer;

            @SuppressWarnings("unchecked")
            CustomIter() {
                stack = (GraphNode<T>[]) Array.newInstance(GraphNode.class, numNodes + 1);
                stack[0] = root;
                advanceHelper(0);

                // Scary...
                buffer = (GraphNode<T>[]) Array.newInstance(GraphNode.class, numNodes);
            }

            @Override
            public boolean hasNext() {
                return !done;
            }

            public void advanceHelper(int i) {
                GraphNode<T> n = stack[i];
                addNodeLoop:
                while (i < numNodes) {
                    for (int j = pointers[i]; j < n.children.size(); ++j) {
                        // visited check
                        if (n.children.get(j).visited) { continue; }
                        pointers[i] = j+1; // Next pointer
                        n = n.children.get(j);
                        n.visited = true;
                        i += 1;
                        stack[i] = n;
                        continue addNodeLoop;
                    }
                    // No children were valid. Backtrack
                    pointers[i] = 0;
                    n.visited = false;
                    i -= 1;

                    if (i < 0) {
                        done = true;
                        return;
                    }
                    n = stack[i];
                }
            }

            public void advance() {
                GraphNode<T> n = stack[numNodes];
                n.visited = false;

                // Optimize: Unrolled first iteration -- always happens
                int i = numNodes - 1;
                n = stack[i];
                pointers[i] = 0;
                n.visited = false;
                i -= 1;
                if (i < 0) {
                    done = true;
                    return;
                }
                advanceHelper(i);
            }

            @Override
            public GraphNode<T>[] next() {
                System.arraycopy(stack, 1, buffer, 0, buffer.length);
                advance();
                return buffer;
            }
        }
    }

    static class OptimizationContext {
        int bestViolations;
        int bestTotal;
        int[] skillpointReal;
        int[] bestOrder;
        boolean[] bestValid;
        int evals;
        List<IEquipment> items;
        ItemSCCResult sccData;

        void update(int[] initialSkillpoints, List<IEquipment> items) {
            this.bestViolations = items.size() + 1;
            this.bestTotal = 0;
            this.skillpointReal = initialSkillpoints;
            this.bestValid = new boolean[items.size()];
            this.evals = 0;

            boolean refresh = true;
            if (this.items != null && items.size() == this.items.size()) {
                refresh = false;
                for (int i = 0; i < items.size(); ++i) {
                    IEquipment a = items.get(i);
                    IEquipment b = this.items.get(i);
                    if (Arrays.equals(a.requirements(), b.requirements())
                            && Arrays.equals(a.bonuses(), b.bonuses())) continue;
                    refresh = true;
                    break;
                }
            }
            if (refresh) {
                this.items = items;
                this.sccData = constructSCCGraph(items);
            }
        }
    }

    private void equipItem(IEquipment item, boolean negative,
            int[] skillpointApplied, int[] skillpointRequired, int[] skillpointMin,
            int[] order, int equipIndex, int itemIndex) {

        int[] reqs = item.requirements();
        int[] bonuses = item.bonuses();
        for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
            skillpointRequired[i] = Math.max(
                    skillpointRequired[i], reqs[i] - skillpointApplied[i]
            );

            // Update sp floor, for popping off calcs
            if (reqs[i] != 0) {
                skillpointMin[i] = Math.max(
                    skillpointMin[i], reqs[i] + bonuses[i]
                );
            }
        }
        for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
            skillpointApplied[i] += bonuses[i];
        }
        if (negative) {
            for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
                if (skillpointMin[i] != 0) {
                    // Apply missing sp to fix negatives
                    // at the end, localMin <= localRequired + localApplied
                    skillpointRequired[i] = Math.max(
                            skillpointRequired[i], skillpointMin[i] - skillpointApplied[i]
                    );
                }
            }
        }
        order[equipIndex] = itemIndex;
    }

    private static boolean canEquip(int[] reqs, int[] currentSkillpoints) {
        for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
            // NOTE: cannot be > 0. Thanks to crafted items
            if (reqs[i] != 0) {
                if (reqs[i] > currentSkillpoints[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean permuteCheck(int index,
            int[] skillpointApplied, int[] skillpointRequired, int[] skillpointMin,
            int[] order, int equipIndex, OptimizationContext ctx) {

        // Last SCC is the terminal
        if (index == ctx.sccData.sccs.size() - 1) {
            ctx.evals += 1;
            int violations = 0;
            int[] skillpoints = ctx.skillpointReal.clone();
            int[] localMin = new int[NUM_SKILLPOINTS];
            boolean[] valid = new boolean[order.length];
            applyLoop:
            for (int itemIndex : order) {
                IEquipment item = ctx.items.get(itemIndex);
                int[] reqs = item.requirements();
                int[] bonuses = item.bonuses();
                if (!canEquip(reqs, skillpoints)) {
                    violations += 1;
                    continue;
                }
                for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
                    if (bonuses[i] + skillpoints[i] < localMin[i] && localMin[i] != 0) {
                        // Pop negative items... if they would force earlier items to pop :sob:
                        continue applyLoop;
                    }
                }
                valid[itemIndex] = true;
                for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
                    // Update sp floor, for popping off calcs
                    if (reqs[i] != 0) {
                        localMin[i] = Math.max(
                                localMin[i], reqs[i] + bonuses[i]
                        );
                    }
                }
                for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
                    skillpoints[i] += bonuses[i];
                }
            }
            int score = IntStream.of(skillpoints).sum();
            if (violations < ctx.bestViolations ||
                    (violations == ctx.bestViolations && score > ctx.bestTotal)) {
                ctx.bestOrder = order;
                ctx.bestTotal = score;
                ctx.bestViolations = violations;
                ctx.bestValid = valid;
            }
            return violations == 0;
        }
        List<GraphNode<IEquipment>> scc = ctx.sccData.sccs.get(index);

        // TODO: can we do wynncraft's "fast application" for positive-only SCCs?
        // I don't think so... since later steps may want different skews
        if (scc.size() == 1) {
            int itemIndex = scc.get(0).nodeIndex;
            boolean negative = ctx.sccData.negatives[index][itemIndex];

            equipItem(scc.get(0).data, negative,
                    skillpointApplied, skillpointRequired, skillpointMin,
                    order, equipIndex, itemIndex);
            return permuteCheck(index + 1, skillpointApplied, skillpointRequired, skillpointMin,
                    order, equipIndex + 1, ctx);
        }

        for (GraphNode<IEquipment>[] perm : new GraphPermutationGenerator<IEquipment>(scc)) {
            int[] localRequired = skillpointRequired.clone();
            int[] localApplied = skillpointApplied.clone();
            int[] localMin = skillpointMin.clone();
            int newIndex = equipIndex;
            for (GraphNode<IEquipment> n : perm) {
                int itemIndex = n.nodeIndex;
                boolean negative = ctx.sccData.negatives[index][itemIndex];
                // order is always mutated (set before recursive call), this is safe
                equipItem(n.data, negative,
                        localApplied, localRequired, localMin,
                        order, newIndex, itemIndex);
                newIndex += 1;
                // TODO: ANY level of pruning at all LOL
            }
            if (permuteCheck(index + 1, localApplied, localRequired, localMin,
                    order, equipIndex + scc.size(), ctx)) {
                return true;
            }
        }
        return false;
    }


    public OptimizationContext ctx = new OptimizationContext();

    @Override
    public void clearCache() {
        ctx = new OptimizationContext();
    }

    public boolean[] check(IEquipment[] items, int[] assignedSkillpoints) {
        // TODO: Filter items
        List<IEquipment> toCheck = new ArrayList<IEquipment>();
        List<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < items.length; ++i) {
            IEquipment item = items[i];
            toCheck.add(item);
            indices.add(i);
        }

        ctx.update(assignedSkillpoints, toCheck);
        permuteCheck(1, new int[5], new int[5], new int[5], new int[toCheck.size()], 0, ctx);
        if (ctx.bestOrder == null) {
            return new boolean[items.length];
        }
        boolean[] valid = new boolean[items.length];
        for (int i = 0; i < indices.size(); ++i) {
            valid[indices.get(i)] = ctx.bestValid[i];
        }
        return valid;
    }
}
