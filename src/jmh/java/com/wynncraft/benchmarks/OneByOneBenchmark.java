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

import static com.wynncraft.enums.Equipment.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
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

    private AlgorithmRegistry.Entry _entry;
    private IPlayerBuilder _builder;

    @Setup(value = Level.Trial)
    public void prepare() {
        // Find the correct algorithm, quite stupid but necessary since
        // JMH doesn't support proper parameters due to its structure
        _entry = AlgorithmRegistry.registry()
            .stream()
            .filter(e -> e.name().equals(algorithm))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm benchmark parameter: " + algorithm));
    }

    @Setup(Level.Iteration)
    public void clean() {
        // Before each iteration we need to create a
        // new builder so we reset the build
        _builder = _entry.builder();
        _builder.allocate(SkillPoint.STRENGTH, 56);
        _builder.allocate(SkillPoint.DEXTERITY, 37);
        _builder.allocate(SkillPoint.INTELLIGENCE, 46);
        _builder.allocate(SkillPoint.DEFENCE, 61);
        _builder.allocate(SkillPoint.AGILITY, 0);
    }

    @Benchmark
    public void one_by_one(Blackhole blackhole) {
        // On this benchmark we include each equipment one by one
        // in the common order (weapon -> armour -> accessory -> tomes)
        clean(); // kept growing over 64...
        for (int i = 0; i < TARGET_BUILD.size(); i++) {
            // Include the next equipment
            IEquipment equipment = TARGET_BUILD.get(i);
            _builder.equipment(equipment);

            // Run the algorithm with the new part
            IAlgorithm algorithm = _entry.algorithm();
            blackhole.consume(algorithm.run(_builder.build()));
        }
    }

    @Benchmark
    public void one_by_one_inverse(Blackhole blackhole) {
        // On this benchmark we include each equipment one by one
        // in the inverse order (tomes -> accessories -> armour -> weapon)
        clean(); // kept growing over 64...
        for (int size = TARGET_BUILD.size(); size > 0; size--) {
            // Include the next equipment
            IEquipment equipment = TARGET_BUILD.get(size - 1);
            _builder.equipment(equipment);

            // Run the algorithm with the new part
            IAlgorithm algorithm = _entry.algorithm();
            blackhole.consume(algorithm.run(_builder.build()));
        }
    }

}
