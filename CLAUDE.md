# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build and test everything
./gradlew clean build

# Build/test a specific module
./gradlew :core:model:check
./gradlew :consumer:check
./gradlew :provider:check

# Run tests only (skip other checks)
./gradlew test

# Run a single test class
./gradlew :core:model:test --tests "au.com.dius.pact.core.model.SomeSpec"

# Run a single test method
./gradlew :core:model:test --tests "au.com.dius.pact.core.model.SomeSpec.some test name"

# Run the tests with a different JDK to the one running Gradle (e.g. one newer than Gradle supports)
./gradlew test -PtestJavaHome=/path/to/jdk

# Publish to local Maven repo (required when testing changes against dependent modules)
./gradlew publishToMavenLocal
```

Maven is required to build the Maven plugin (`provider:maven`).

If the build runs out of memory, add to `~/.gradle/gradle.properties`:
```
org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

## Linting

Two static analysis tools run automatically as part of `check`/`build`:

- **Detekt** (Kotlin): config at `config/detekt-config.yml`. Run standalone: `./gradlew detekt`
- **CodeNarc** (Groovy/tests): config at `config/codenarc/ruleset.groovy` (main) and `config/codenarc/rulesetTest.groovy` (tests). Run standalone: `./gradlew codenarcMain` or `./gradlew codenarcTest`

## Architecture

The project is divided into three main areas:

### Core (`core/`)
Foundation modules depended on by everything else:
- **`core:support`** — HTTP client utilities and common helpers
- **`core:model`** — Pact domain model: `Pact`, `Interaction`, `Request`, `Response`, `OptionalBody`, pact file reader/writer
- **`core:matchers`** — Matching logic used when verifying requests/responses against a pact
- **`core:pactbroker`** — Pact Broker integration (publishing/retrieving pacts)

### Consumer (`consumer/`)
DSL and mock server support for writing consumer-side tests:
- **`consumer`** — Base library: mock HTTP server (Ktor/Netty), interaction building, pact file writing
- **`consumer:groovy`** — Groovy DSL wrapper
- **`consumer:junit`** / **`consumer:junit5`** — JUnit 4 / JUnit 5 integration (`@PactTestFor`, `@Pact` annotations)
- **`consumer:kotlin`** — Kotlin DSL

### Provider (`provider/`)
Verification support for provider-side tests:
- **`provider`** — Base verification engine: replays recorded interactions against a live provider
- **`provider:junit`** / **`provider:junit5`** — JUnit test runners for provider verification
- **`provider:spring`** / **`provider:junit5spring`** / **`provider:spring6`** — Spring Boot test support
- **`provider:gradle`** — Gradle plugin (`pactVerify` task)
- **`provider:maven`** — Maven plugin
- **`provider:lein`** — Leiningen plugin (excluded on Windows CI)

### Other
- **`pact-jvm-server`** — Standalone mock server process
- **`pact-specification-test`** — Runs pact-jvm against the upstream Pact specification test suite
- **`compatibility-suite`** — Compatibility tests against other Pact implementations
- **`pact-publish`** — Publishing utilities
- **`buildSrc/`** — Shared Gradle convention plugins (`kotlin-common-conventions.gradle`, `kotlin-library-conventions.gradle`)

## Language and Test Conventions

- **Source**: Kotlin (JDK 17 target, Kotlin 1.8.22)
- **Tests**: Groovy using [Spock](https://spockframework.org/) — test files live in `src/test/groovy/**/*Spec.groovy`
- Test classes extend `Specification` and use `given/when/then` blocks; parameterised tests use `@Unroll` with `where:` tables
- The system property `pact_do_not_track=true` is always set during tests to suppress analytics

## Key Conventions

- Convention plugins in `buildSrc/` apply consistently across all modules — prefer editing them over per-module build files when making cross-cutting build changes
- Dependency versions are managed via `constraints` blocks in `kotlin-common-conventions.gradle`; update versions there, not in individual modules
- The `provider:spring6` module is conditionally included only on JDK 17+; the `provider:lein` module is excluded on Windows

