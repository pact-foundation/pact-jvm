# Copilot Instructions

## Build, test, and lint commands

- This is a multi-module Gradle build targeting Java 17; the shared build logic lives in `buildSrc/`.
- Full build: `./gradlew clean build`
- Run tests only: `./gradlew test`
- Run checks for the main module groups:
  - `./gradlew :core:model:check`
  - `./gradlew :consumer:check`
  - `./gradlew :provider:check`
  - CI also scopes builds with `./gradlew -p core check`, `./gradlew -p consumer check`, `./gradlew -p provider check`, and `./gradlew -p pact-specification-test check`
- Run one test class: `./gradlew :core:model:test --tests "au.com.dius.pact.core.model.SomeSpec"`
- Run one test method: `./gradlew :core:model:test --tests "au.com.dius.pact.core.model.SomeSpec.some test name"`
- Lint and static analysis:
  - `./gradlew detekt`
  - `./gradlew codenarcMain`
  - `./gradlew codenarcTest`
- Repo-specific verification tasks:
  - `./gradlew :consumer:checkDoctests`
  - `./gradlew :consumer:testDoctest`
  - `./gradlew :provider:testSnapshots`
  - `./gradlew :compatibility-suite:v1`
  - `./gradlew :compatibility-suite:v2`
  - `./gradlew :compatibility-suite:v3`
  - `./gradlew :compatibility-suite:v4`
- Publish locally when testing changes across dependent modules: `./gradlew publishToMavenLocal`
- Maven is required when working on `provider:maven`

## High-level architecture

- `settings.gradle` defines a multi-project build organized around three primary areas plus supporting executables/test suites.
- `core/` holds the shared Pact implementation:
  - `core:model` contains the Pact domain model and pact file read/write logic.
  - `core:matchers` contains request/response matching logic.
  - `core:pactbroker` handles Pact Broker integration.
  - `core:support` provides shared HTTP and utility support used across the repo.
- `consumer/` builds on `core/*` to provide consumer-side pact authoring, mock server support, and framework-specific integrations for Groovy, JUnit 4, JUnit 5, Kotlin, and Spock.
- `provider/` builds on `core/*` to provide provider verification plus adapters/plugins for JUnit 4, JUnit 5, Spring, Spring 6, Spring 7, Gradle, Maven, and Leiningen.
- `pact-jvm-server/` packages a standalone server/CLI around the consumer and broker pieces.
- `pact-specification-test/` verifies pact-jvm behavior against the upstream Pact specification suites.
- `compatibility-suite/` runs Cucumber-based compatibility suites for spec versions V1-V4.
- `pact-publish/` contains publishing utilities used outside the main consumer/provider library split.

## Key conventions

- Main source code is predominantly Kotlin, but tests are predominantly Groovy + Spock. Expect test files under `src/test/groovy/**/*Spec.groovy`, classes extending `Specification`, and parameterized tests using `@Unroll` with `where:` tables.
- Shared Gradle behavior is centralized in the convention plugins under `buildSrc/src/main/groovy/au.com.dius.pact.*-conventions.gradle`. For cross-cutting build changes, update those plugins instead of duplicating config in individual module build files.
- Dependency versions are centralized in the constraints block in `buildSrc/src/main/groovy/au.com.dius.pact.kotlin-common-conventions.gradle`; update versions there instead of scattering version bumps through module build files.
- Tests and custom verification tasks set `pact_do_not_track=true`; preserve that when adding new test tasks or integration-style tasks.
- `consumer:check` is stricter than a normal module check: it also validates README-derived doctest stubs via `checkDoctests` and runs them via `testDoctest`.
- `provider:check` is also specialized: snapshot reporter tests are excluded from the default `test` task and run separately through `testSnapshots`, which is wired into `check`.
- CI runs module checks on multiple JDKs. Detekt is only applied when the current JDK is below 23, so JDK-specific behavior matters when changing build logic.
- `provider:lein` is conditionally included and is not part of GitHub Actions builds; do not assume it is present in CI.
