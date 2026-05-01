package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom IPlayer for the WynnSolver V3.5 / V4.5 algorithms.
 *
 * Exposes the assigned-SP and bonus arrays directly via package-private
 * fields so the algorithm can:
 *   - read base SP without 5x SkillPoint.ordinal() lookups,
 *   - write the final bonus vector once (no per-kept-item modify() loop).
 *
 * Implements IPlayer correctly so external callers (tests, harnesses)
 * that go through the interface still observe consistent state.
 */
public final class WynnSolverPlayer implements IPlayer {

    private static final SkillPoint[] SKILL_POINTS = SkillPoint.values();

    final List<IEquipment> equipment;
    final int[] allocated;
    final int[] bonus = new int[SKILL_POINTS.length];
    int weight;

    private WynnSolverPlayer(List<IEquipment> equipment, int[] allocated) {
        this.equipment = equipment;
        this.allocated = allocated;
    }

    @Override
    public List<IEquipment> equipment() {
        return equipment;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public int total(SkillPoint skill) {
        int i = skill.ordinal();
        return allocated[i] + bonus[i];
    }

    @Override
    public int allocated(SkillPoint skill) {
        return allocated[skill.ordinal()];
    }

    @Override
    public void modify(int[] skillPoints, boolean sum) {
        int sign = sum ? 1 : -1;
        int d0 = skillPoints[0] * sign;
        int d1 = skillPoints[1] * sign;
        int d2 = skillPoints[2] * sign;
        int d3 = skillPoints[3] * sign;
        int d4 = skillPoints[4] * sign;
        bonus[0] += d0;
        bonus[1] += d1;
        bonus[2] += d2;
        bonus[3] += d3;
        bonus[4] += d4;
        weight += d0 + d1 + d2 + d3 + d4;
    }

    @Override
    public void reset() {
        Arrays.fill(bonus, 0);
        weight = 0;
    }

    public static final class Builder implements IPlayerBuilder<WynnSolverPlayer> {

        private final List<IEquipment> equipment = new ArrayList<>(16);
        private final int[] allocated = new int[SKILL_POINTS.length];

        @Override
        public IPlayerBuilder<WynnSolverPlayer> equipment(IEquipment... items) {
            for (IEquipment it : items) equipment.add(it);
            return this;
        }

        @Override
        public IPlayerBuilder<WynnSolverPlayer> allocate(SkillPoint point, int amount) {
            allocated[point.ordinal()] = amount;
            return this;
        }

        @Override
        public WynnSolverPlayer build() {
            return new WynnSolverPlayer(new ArrayList<>(equipment), allocated.clone());
        }
    }
}
