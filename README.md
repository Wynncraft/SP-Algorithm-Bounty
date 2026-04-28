## 🏆 Skill Point Algorithm Bounty
Wynncraft is seeking an **optimized skill point allocation algorithm** capable of efficiently validating and maximizing equipment combinations under strict performance constraints.

A bounty reward of **up to 100 in-game shares** will be granted for a successful solution.</br>
Exceptional implementations may qualify for a higher reward.

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

## 🧪 Combinatory Test Cases
This repository contains a few combinatory test cases for equipment.
🏆 We are also offering rewards for newly introduced test cases that break current algorithms.

### Instructions
- Fork this repository and implement your new test case in `CombinationTests`.
- Open a Pull Request to this repository with your new test case.
- If your test case breaks current algorithms in ways that are not similar to already existing tests, you will be eligible for an untradable share reward depending on the case.

### Running Test Cases
```bash
./gradlew test
```

or to execute a specific algorithm only

```bash
./gradlew test -Palgorithm='Your Algorithm Name'
```
