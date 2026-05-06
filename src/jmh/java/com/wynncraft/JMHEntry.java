package com.wynncraft;

import com.wynncraft.instances.BuildFactory;
import com.wynncraft.benchmarks.FullEquipBenchmark;
import com.wynncraft.benchmarks.OneByOneBenchmark;
import com.wynncraft.core.interfaces.IEquipment;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.wynncraft.enums.Equipment.*;

public final class JMHEntry {

    private static final Map<String, BuildFactory> BUILD_REGISTRY = new LinkedHashMap<>();
    static {
        // ── Hand-written canonical builds ────────────────────────────────
        // SP indices: STRENGTH=0, DEXTERITY=1, INTELLIGENCE=2, DEFENCE=3, AGILITY=4

        // Complete player build (weapon, armour, tomes); all requirements met.
        register("complete_best_case", new BuildFactory(
            new IEquipment[] {
                SPRING,
                APHOTIC, TIME_RIFT, TAO,
                MOONTOWER, XEBEC, DIAMOND_HYDRO_BRACELET,
                MOON_POOL_CIRCLET, YANG,
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            },
            new int[] { 60, 0, 60, 0, 80 }
        ));

        // Complete player build with no SP allocated; almost nothing should pass.
        register("complete_worst_case", new BuildFactory(
            new IEquipment[] {
                SPRING,
                APHOTIC, TIME_RIFT, TAO,
                MOONTOWER, XEBEC, DIAMOND_HYDRO_BRACELET,
                MOON_POOL_CIRCLET, YANG,
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            },
            new int[] { 0, 0, 0, 0, 0 }
        ));

        // Builds adapted from Sugo's forum thread.
        register("warrior_convergence", new BuildFactory(
            new IEquipment[] {
                CONVERGENCE,
                CAESURA, DELIRIUM, CHAMPIONS_VALIANCE,
                SYMPHONIE_FANTASTIQUE, LODESTONE, LODESTONE,
                PROWESS, METAMORPHOSIS,
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            },
            new int[] { 41, 57, 41, 61, 0 }
        ));

        register("warrior_ascendancy", new BuildFactory(
            new IEquipment[] {
                ASCENDANCY,
                XIUHTECUHTLI, FIDELIUS, EDEN_BLESSED_GUARDS,
                BOREAL, CISTERN_CIRCLET, CISTERN_CIRCLET,
                BREAKTHROUGH, AMBIVALENCE,
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            },
            new int[] { 0, 0, 65, 75, 55 }
        ));

        register("shaman_resonance", new BuildFactory(
            new IEquipment[] {
                RESONANCE,
                PISCES, DROWN, CHAMPIONS_VALIANCE,
                CRUSADE_SABATONS, OLIVE, OLIVE,
                PROWESS, SIMULACRUM,
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            },
            new int[] { 56, 37, 46, 61, 0 }
        ));

        register("assassin_vengeance", new BuildFactory(
            new IEquipment[] {
                VENGEANCE,
                OBSIDIAN_FRAMED_HELMET, TAURUS, BABEL,
                DAWNBREAK, BEATDOWN, BEATDOWN,
                ETERNAL_TOIL, BLASTCRYSTAL_CHAIN,
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            },
            new int[] { 81, 65, 0, 56, 0 }
        ));
    }

    private JMHEntry() { }

    public static void main(String[] args) throws CommandLineOptionException, RunnerException {
        CommandLineOptions cli = new CommandLineOptions(args);
        ChainedOptionsBuilder builder = new OptionsBuilder().parent(cli);

        // Only seed our default include list if the CLI didn't pass one.
        // Otherwise our includes union with the CLI's, defeating filtering.
        if (cli.getIncludes().isEmpty()) {
            builder.include(FullEquipBenchmark.class.getName());
            builder.include(OneByOneBenchmark.class.getName());
        }

        // Same for the algorithm parameter: defer to CLI when provided.
        if (!cli.getParameter("algorithm").hasValue()) {
            String[] algorithmNames = AlgorithmRegistry.registry()
                .stream()
                .map(AlgorithmRegistry.Entry::name)
                .toArray(String[]::new);
            builder.param("algorithm", algorithmNames);
        }

        new Runner(builder.build()).run();
    }

    /**
     * Retrieves a registered build with the provided identifier
     *
     * @param id the build indetifier
     * @return the resulting factory
     */
    public static BuildFactory build(String id) {
        BuildFactory spec = BUILD_REGISTRY.get(id);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown build id: " + id);
        }

        return spec;
    }

    /**
     * Register a new build that benchmarks can address by id.
     */
    public static void register(String id, BuildFactory spec) {
        BUILD_REGISTRY.put(id, spec);
    }

}
