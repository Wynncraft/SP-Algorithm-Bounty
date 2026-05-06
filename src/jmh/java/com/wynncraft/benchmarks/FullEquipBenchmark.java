package com.wynncraft.benchmarks;

import com.wynncraft.AlgorithmRegistry;
import com.wynncraft.JMHEntry;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.instances.BuildFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class FullEquipBenchmark {

    @Param("__ignore__")
    public String algorithm;

    @Param({
        "complete_best_case",
        "complete_worst_case",
        "warrior_convergence",
        "warrior_ascendancy",
        "shaman_resonance",
        "assassin_vengeance"
    })
    public String build;

    private IAlgorithm _algorithm;
    private IPlayer _player;

    @Setup(value = Level.Trial)
    public void prepare() {
        // Find the correct algorithm, quite stupid but necessary since
        // JMH doesn't support proper parameters due to its structure
        BuildFactory spec = JMHEntry.build(build);
        AlgorithmRegistry.Entry entry = AlgorithmRegistry.registry()
            .stream()
            .filter(e -> e.name().equals(algorithm))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm benchmark parameter: " + algorithm));

        // Retrieve the algorithm and prepare the player
        // we will run against
        _algorithm = entry.algorithm();
        _player = spec.create(entry);
    }

    @Benchmark
    public void full_equip(Blackhole blackhole) {
        // Cold cache per invocation: every algorithm starts each measurement
        // from the same baseline, regardless of whether it caches across runs.
        _algorithm.clearCache();
        _player.reset();

        // Then after resetting run the algorithm
        IAlgorithm.Result result = _algorithm.run(_player);
        blackhole.consume(result);
    }

}
