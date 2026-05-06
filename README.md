## 🏆 Skill Point Algorithm Bounty
Wynncraft is seeking an **optimized skill point allocation algorithm** capable of efficiently validating and maximizing equipment combinations under strict performance constraints.

A bounty reward of **up to 100 in-game shares** will be granted for a successful solution.</br>
Exceptional implementations may qualify for a higher reward.
---
## 📌 Objective
Design an algorithm that:

- Accepts a given set of equipment items
- Evaluates viable combinations
- Returns the **combination containing the highest number of valid items**

## ⚙️ Requirements
Your solution must be written in Java 21 so we can evaluate it in a real scenario.

### Functional Constraints
- Each piece of equipment has **skill point requirements** that must be validated.
- Equipment may add or **subtract skill points** when equipped.
- Skill points from equipment must **not recursively enable other equipment**; no bootstrapping between items is allowed.

### Validation Rules
- A piece of equipment is considered **valid** only if all of its requirements are met at the time of evaluation.
- The algorithm must determine validity across the full combination; **the order of items should not matter**.
- In the event of a tie, the combination with the **highest total** given skill points should win.
---
## 🧑‍💻 Implementation
This repository provides a base structure similar to our real usage, including testing and benchmarking.

### Instructions
- Fork this repository and implement your changes under the `com.wynncraft.algorithms` package.
  - If necessary for your optimization, you may implement a custom version of `IPlayer`; otherwise, use `WynnPlayer`.
  - Every piece of equipment has an associated type. You may use these types to further optimize your algorithm as you see fit.
- Ensure your algorithm is registered under `AlgorithmRegistry`.
- Open a Pull Request to this repository with your new algorithm so we can evaluate it.
- Once we validate your PR, it will be merged and considered a valid entry in the competition.
  - If any further modification to your algorithm is necessary, you may submit another Pull Request.
  - Make sure the new version is in a separate class and registered again, even if it is only a one-line change.
- You may submit multiple algorithms as you see fit. We will choose the best one for our usage case.

### Implementing `IAlgorithm`
Beyond `run()`, two optional hooks are load-bearing for benchmarking and correctness:

`IAlgorithm#clearCache()`</br>
Override if your algorithm holds any state across `run()` calls (memoization, mask caches, last-seen-player refs). Benchmarks call it to establish a cold baseline — per-invocation in `FullEquipBenchmark` / `OneByOneBenchmark`, once per trial in `ServerSimBenchmark`.

`AlgorithmRegistry`</br> 
Holds one instance per algorithm, reused across all tests and benchmarks. Tests do **not** call `clearCache()` between cases, so your cache must self-invalidate when the equipment array or assigned SP change — otherwise stale state can mask correctness bugs. Implementations must be safe to call before the first `run()`. Pure algorithms can leave the default no-op.

**Inviolable rule**: never modify an instance `IEquipment` instance.

---
## 🧪 Combinatory Test Cases
This repository contains a few combinatory test cases for equipment.
🏆 We are also offering rewards for newly introduced test cases that break current algorithms.

### Instructions
- Fork this repository and implement your new test case in `CombinationTests`.
- Open a Pull Request to this repository with your new test case.
- If your test case breaks current algorithms in ways that are not similar to already existing tests, you will be eligible for an untradable share reward depending on the case.

---

## 🏁 Local Workflow Notes

### Test filtering

Tag-based selection (`@Tag` on each test class):

| Class | Tag | Cases |
|---|---|---|
| `CombinationTests` | `upstream` | Hand-written upstream baseline |
| `SyntheticCombinationTests` | `curated` | Synthetic curated cases (`SyntheticEquipment`) |
| `GeneratedCombinationTests` | `generated` | Auto-generated cases |

```bash
./gradlew test -Pcases=upstream                 # one tag
./gradlew test -Pcases=curated,generated        # multiple tags
./gradlew test -Palgo='WynnSolver V1'           # algorithm filter (alias for -Palgorithm=)
./gradlew test -Palgo='WynnSolver V1' -Pcases=curated
```

### Benchmark structure

JMH lives in `src/jmh/java/com/wynncraft/`:
- `JMHEntry` — holds `BUILD_REGISTRY: Map<String, BuildFactory>`. Registers six canonical builds (two sanity + four archetypes from Sugo's forum thread).
- `benchmarks/BuildSpec` — `(IEquipment[] equipment, int[] assignedSkillpoints)`. Use `spec.apply(builder)` to materialize, or read fields directly when manipulating items/SP individually.
- `benchmarks/BenchOps` — shared helpers (equip permutations, SP increments, sequence runners).
- `benchmarks/FullEquipBenchmark` — single full-build `run()` per invocation.
- `benchmarks/OneByOneBenchmark` — single-item incremental equip.
- `benchmarks/ServerSimBenchmark` — primary mixed workload: 10 equip sequences (8 perms each) + 10 SP-change sequences + 4000 weapon swaps, drawn from the 9 full-build synthetic cases via seeded RNG. The most representative perf benchmark; raise per-measurement time for serious evaluation so the JIT can settle.

```bash
./gradlew jmh                                                  # all benchmarks, all algorithms
./gradlew jmh -Pbm=ServerSimBenchmark                          # one benchmark class (regex match)
./gradlew jmh -Palgo='WynnSolver V1'                           # one algorithm
./gradlew jmh -Palgo='WynnSolver V1,Pruned Mask V2'            # multiple algorithms
./gradlew jmh -Pbm=ServerSimBenchmark -Palgo='WynnSolver V1'   # combine
./gradlew jmh -PjmhArgs="-i 1 -wi 1 -f 1"                      # raw JMH CLI passthrough
```

Adding a new build:
```java
JMHEntry.register("my_build", new BuildSpec(
    new IEquipment[] { /* items */ },
    new int[] { str, dex, int_, def, agi }
));
```
