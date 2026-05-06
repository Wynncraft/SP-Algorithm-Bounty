package com.wynncraft.benchmarks;

import com.wynncraft.AlgorithmRegistry;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.SkillPoint;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.wynncraft.enums.Equipment.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class OneByOneBenchmark {

    private static final List<IEquipment> TARGET_BUILD = List.of(
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

    @Param("__ignore__")
    public String algorithm;

    private IAlgorithm _algorithm;
    private Supplier<IPlayerBuilder> _builderSupplier;

    @Setup(value = Level.Trial)
    public void prepare() {
        // Find the correct algorithm, quite stupid but necessary since
        // JMH doesn't support proper parameters due to its structure
        AlgorithmRegistry.Entry entry = AlgorithmRegistry.registry()
            .stream()
            .filter(e -> e.name().equals(algorithm))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm benchmark parameter: " + algorithm));

        _algorithm = entry.algorithm();
        _builderSupplier = entry::builder;
    }

    @Benchmark
    public void one_by_one(Blackhole blackhole) {
        // Cold cache per invocation; cache then accumulates across the 14
        // incremental run() calls, mirroring ServerSimBenchmark's sequence
        // semantics.
        _algorithm.clearCache();

        IPlayerBuilder builder = _builderSupplier.get();
        builder.allocate(SkillPoint.STRENGTH, 56);
        builder.allocate(SkillPoint.DEXTERITY, 37);
        builder.allocate(SkillPoint.INTELLIGENCE, 46);
        builder.allocate(SkillPoint.DEFENCE, 61);
        builder.allocate(SkillPoint.AGILITY, 0);

        // On this benchmark we include each equipment one by one
        // in the common order (weapon -> armour -> accessory -> tomes)
        for (int i = 0; i < TARGET_BUILD.size(); i++) {
            builder.equipment(TARGET_BUILD.get(i));
            blackhole.consume(_algorithm.run(builder.build()));
        }
    }

    @Benchmark
    public void one_by_one_inverse(Blackhole blackhole) {
        _algorithm.clearCache();

        IPlayerBuilder builder = _builderSupplier.get();
        builder.allocate(SkillPoint.STRENGTH, 56);
        builder.allocate(SkillPoint.DEXTERITY, 37);
        builder.allocate(SkillPoint.INTELLIGENCE, 46);
        builder.allocate(SkillPoint.DEFENCE, 61);
        builder.allocate(SkillPoint.AGILITY, 0);

        // On this benchmark we include each equipment one by one
        // in the inverse order (tomes -> accessories -> armour -> weapon)
        for (int i = TARGET_BUILD.size(); i > 0; i--) {
            builder.equipment(TARGET_BUILD.get(i - 1));
            blackhole.consume(_algorithm.run(builder.build()));
        }
    }

}
