package com.wynncraft;

import com.wynncraft.benchmarks.FullEquipBenchmark;
import com.wynncraft.benchmarks.OneByOneBenchmark;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.wynncraft.enums.Equipment.*;

public final class JMHEntry {

    private static final Map<String, Consumer<IPlayerBuilder>> BUILD_REGISTRY = new HashMap<>();
    static {
        // This is a complete player build, with a weapon armor & tomes
        // All requirements are met so everything should pass
        register("complete_best_case", builder -> {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.INTELLIGENCE, 60);
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                SPRING,
                // Armour & Accessories
                APHOTIC, TIME_RIFT, TAO,
                MOONTOWER, XEBEC, DIAMOND_HYDRO_BRACELET,
                MOON_POOL_CIRCLET, YANG,
                // Tomes (only the first one matters!)
                MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                VOLTAIC_TOME_OF_COMBAT_MASTERY_III
            );
        });

        // This is a complete player build, with a weapon armor & tomes
        // No requirements are met, so almost nothing should pass
        register("complete_worst_case", builder -> builder.equipment(
            SPRING,
            // Armour & Accessories
            APHOTIC, TIME_RIFT, TAO,
            MOONTOWER, XEBEC, DIAMOND_HYDRO_BRACELET,
            MOON_POOL_CIRCLET, YANG,
            // Tomes (only the first one matters!)
            MASTERMINDS_TOME_OF_ALLEGIANCE_3,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
            VOLTAIC_TOME_OF_COMBAT_MASTERY_III
        ));

        // Here I'm registering some tests builds, these are mostly scrapped from
        // Sugo's forum thread, we just need example test data, thanks sugo lol
        {
            register("warrior_convergence", builder -> {
                builder.allocate(SkillPoint.STRENGTH, 41);
                builder.allocate(SkillPoint.DEXTERITY, 57);
                builder.allocate(SkillPoint.INTELLIGENCE, 41);
                builder.allocate(SkillPoint.DEFENCE, 61);
                builder.allocate(SkillPoint.AGILITY, 0);
                builder.equipment(
                    CONVERGENCE,
                    // Armour & Accessories
                    CAESURA, DELIRIUM, CHAMPIONS_VALIANCE,
                    SYMPHONIE_FANTASTIQUE, LODESTONE, LODESTONE,
                    PROWESS, METAMORPHOSIS,
                    // Tomes (only the first one matters!)
                    MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III
                );
            });
            register("warrior_ascendancy", builder -> {
                builder.allocate(SkillPoint.STRENGTH, 0);
                builder.allocate(SkillPoint.DEXTERITY, 0);
                builder.allocate(SkillPoint.INTELLIGENCE, 65);
                builder.allocate(SkillPoint.DEFENCE, 75);
                builder.allocate(SkillPoint.AGILITY, 55);
                builder.equipment(
                    ASCENDANCY,
                    // Armour & Accessories
                    XIUHTECUHTLI, FIDELIUS, EDEN_BLESSED_GUARDS,
                    BOREAL, CISTERN_CIRCLET, CISTERN_CIRCLET,
                    BREAKTHROUGH, AMBIVALENCE,
                    // Tomes (only the first one matters!)
                    MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III
                );
            });
            register("shaman_resonance", builder -> {
                builder.allocate(SkillPoint.STRENGTH, 56);
                builder.allocate(SkillPoint.DEXTERITY, 37);
                builder.allocate(SkillPoint.INTELLIGENCE, 46);
                builder.allocate(SkillPoint.DEFENCE, 61);
                builder.allocate(SkillPoint.AGILITY, 0);
                builder.equipment(
                    RESONANCE,
                    // Armour & Accessories
                    PISCES, DROWN, CHAMPIONS_VALIANCE,
                    CRUSADE_SABATONS, OLIVE, OLIVE,
                    PROWESS, SIMULACRUM,
                    // Tomes (only the first one matters!)
                    MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III
                );
            });
            register("assassin_vengeance", builder -> {
                builder.allocate(SkillPoint.STRENGTH, 81);
                builder.allocate(SkillPoint.DEXTERITY, 65);
                builder.allocate(SkillPoint.INTELLIGENCE, 0);
                builder.allocate(SkillPoint.DEFENCE, 56);
                builder.allocate(SkillPoint.AGILITY, 0);
                builder.equipment(
                    VENGEANCE,
                    // Armour & Accessories
                    OBSIDIAN_FRAMED_HELMET, TAURUS, BABEL,
                    DAWNBREAK, BEATDOWN, BEATDOWN,
                    ETERNAL_TOIL, BLASTCRYSTAL_CHAIN,
                    // Tomes (only the first one matters!)
                    MASTERMINDS_TOME_OF_ALLEGIANCE_3,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III, VOLTAIC_TOME_OF_COMBAT_MASTERY_III,
                    VOLTAIC_TOME_OF_COMBAT_MASTERY_III
                );
            });
        }


    }

    public static void main(String[] args) throws CommandLineOptionException, RunnerException {
        String[] algorithmNames = AlgorithmRegistry.registry()
            .stream()
            .map(AlgorithmRegistry.Entry::name)
            .toArray(String[]::new);

        Options options = new OptionsBuilder()
            .parent(new CommandLineOptions(args))
            .include(FullEquipBenchmark.class.getName())
            .include(OneByOneBenchmark.class.getName())
            .param("algorithm", algorithmNames)
            .build();

        new Runner(options).run();
    }

    /**
     * Creates a player for the provided build identifier
     *
     * @param id the id of the build
     * @param entry the algorithm entry
     * @return the resulting player
     */
    public static IPlayer build(String id, AlgorithmRegistry.Entry entry) {
        IPlayerBuilder builder = entry.builder();
        {
            Consumer<IPlayerBuilder> consumer = BUILD_REGISTRY.get(id);
            consumer.accept(builder);
        }
        return builder.build();
    }

    /**
     * Register a new build that will be evaluated against all test cases
     *
     * @param id the build identifier
     * @param consumer the builder consumer
     */
    public static void register(String id, Consumer<IPlayerBuilder> consumer) {
        BUILD_REGISTRY.put(id, consumer);
    }

}
