# Matcher benchmarking and profiling

This module includes a JMH benchmark harness for the V2 matching engine so performance changes can be measured and profiled against a repeatable workload.

## Benchmarks

The current benchmark class is:

- `au.com.dius.pact.core.matchers.engine.V2MatchingEngineBenchmark`

It exposes three benchmark methods:

- `buildResponsePlan`
- `executeResponsePlan`
- `buildAndExecuteResponsePlan`

Each benchmark currently runs with `itemCount` values `10`, `50`, and `100`.

## Run the benchmarks

Run the full JMH suite for this module:

```bash
./gradlew :core:matchers:jmh
```

Results are written to:

```text
core/matchers/build/results/jmh/results.txt
```

## Record a JFR profile

Use the dedicated helper task to run the benchmark jar with Java Flight Recorder enabled:

```bash
./gradlew :core:matchers:jmhJfr
```

By default this records:

- benchmark: `au.com.dius.pact.core.matchers.engine.V2MatchingEngineBenchmark.executeResponsePlan`
- `itemCount=100`
- output file under `core/matchers/build/jfr/`

Example:

```bash
./gradlew :core:matchers:jmhJfr \
  -PjmhBenchmark=au.com.dius.pact.core.matchers.engine.V2MatchingEngineBenchmark.executeResponsePlan \
  -PjmhItemCount=100
```

### Common overrides

| Property | Purpose | Default |
| --- | --- | --- |
| `-PjmhBenchmark=...` | Fully qualified benchmark method to run | `...V2MatchingEngineBenchmark.executeResponsePlan` |
| `-PjmhItemCount=...` | Benchmark payload size | `100` |
| `-PjmhWarmupIterations=...` | JMH warmup iterations | `3` |
| `-PjmhMeasurementIterations=...` | JMH measurement iterations | `5` |
| `-PjmhIterationTime=...` | Warmup/measurement time per iteration | `1s` |
| `-PjmhForks=...` | Number of benchmark forks | `1` |
| `-PjfrSettings=...` | JFR settings profile | `profile` |
| `-PjfrFile=...` | Output `.jfr` file | `core/matchers/build/jfr/<benchmark>-<itemCount>.jfr` |

Relative `-PjfrFile` values are resolved from the repository root. Absolute paths are also supported.

Example with a fixed output path:

```bash
./gradlew :core:matchers:jmhJfr \
  -PjmhBenchmark=au.com.dius.pact.core.matchers.engine.V2MatchingEngineBenchmark.executeResponsePlan \
  -PjmhItemCount=10 \
  -PjmhWarmupIterations=1 \
  -PjmhMeasurementIterations=1 \
  -PjmhIterationTime=1s \
  -PjfrFile=core/matchers/build/jfr/execute-10.jfr
```

## Viewing the recording

### IntelliJ IDEA

IntelliJ can open the generated `.jfr` file directly. This is often the simplest way to inspect:

- Flame Graph
- Call Tree
- hottest methods on the benchmark path

Recommended starting point:

1. Capture `executeResponsePlan` with `itemCount=100`
2. Look at the Flame Graph for wide stacks
3. Use Call Tree to find the main callers/callees
4. Repeat for `buildResponsePlan` to separate plan construction cost from plan execution cost

### JDK Mission Control

Mission Control is also suitable if you want the standard JVM tooling view. The most useful tabs for this workload are:

- Method Profiling
- Allocation / Object Statistics
- Garbage Collections
- Lock Instances

## IntelliJ profiler

If you prefer IntelliJ's built-in profiler UI, use the same benchmark workload and parameters as the JMH/JFR flow. The important thing is to keep the benchmark method and `itemCount` consistent so profiling runs stay comparable with the JMH numbers.

The benchmark harness is the stable workload; JFR, IntelliJ's profiler, and any other profiler are just different ways to inspect it.

## Recommended workflow

1. Use `:core:matchers:jmh` to establish the baseline timings
2. Profile `executeResponsePlan` first, since it is usually the most interesting runtime path
3. Profile `buildResponsePlan` separately if you need to split construction cost from execution cost
4. Change one thing at a time
5. Re-run the same benchmark and compare the numbers
