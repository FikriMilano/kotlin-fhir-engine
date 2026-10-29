# Contributing

This guide is for working on the library itself. To use it in an app, see the [README](README.md).

## Setup

- JDK 21.
- The Android SDK with API 36, for the Android targets.
- Xcode on macOS, for the iOS targets and tests.
- Chrome, for the JS and Wasm browser tests.

The project has two modules. `engine` is the library and `engine-app` is the demo app. The iOS demo
is the Xcode project in `iosApp`.

## Building

```bash
./gradlew :engine:compileAndroidMain :engine:compileKotlinDesktop :engine:compileKotlinJs \
  :engine:compileKotlinWasmJs :engine:compileKotlinIosArm64 :engine:compileKotlinIosSimulatorArm64
```

## Testing

Shared tests live in `engine/src/commonTest` and run on every platform. Platform tests live in the
platform's test source set, for example `engine/src/iosTest`.

| Platform                   | Task                                          | Needs                                    |
|:---------------------------|:----------------------------------------------|:-----------------------------------------|
| Desktop                    | `:engine:desktopTest`                         |                                          |
| Android, on the JVM        | `:engine:testAndroidHostTest`                 | Skips tests that need an Android Context |
| Android, on a device       | `:engine:connectedAndroidDeviceTest`          | A running emulator or connected device   |
| iOS, with SQLCipher        | `:engine:iosSimulatorArm64Test`               | Xcode. Downloads SQLCipher on first run  |
| iOS, with the system SQLite | `:engine:iosSimulatorArm64SystemSqliteTest`  | Xcode                                    |
| JS                         | `:engine:jsBrowserTest`                       | Chrome                                   |
| Wasm                       | `:engine:wasmJsBrowserTest`                   | Chrome                                   |

Encryption tests run only where the platform can encrypt in tests: Android devices and iOS with
SQLCipher. Elsewhere they return early.

## Formatting

The code is formatted with [ktfmt](https://github.com/facebook/ktfmt) in Google style through
Spotless, which also adds the license header to new files. CI fails on unformatted code.

```bash
./gradlew spotlessApply   # format
./gradlew spotlessCheck   # what CI runs
```

## Continuous integration

[`ci.yml`](.github/workflows/ci.yml) runs on every pull request and every push to `main`:

1. Spotless, and compiling every platform.
2. The tests in the table above, except Android device tests, each in its own job.
3. The Android device tests, on an API 34 emulator.
4. The micro benchmarks, comparing the pull request with its base. See
   [Benchmarking](docs/benchmarking.md).

## How the code is organized

- **Search parameters** are generated at build time from the FHIR R4 search parameter definitions,
  by `GenerateSearchParamsTask` in `buildSrc`.
- **The database** is defined in `engine/src/commonMain/.../db/impl/ResourceDatabase.kt`. Each
  platform's `DatabaseBuilder.kt` picks the SQLite driver.
- **Sync** lives in `engine/src/commonMain/.../sync`. The Android scheduler is in
  `engine/src/androidMain/.../sync`.

## Changing the database schema

Room exports every schema version to `engine/schemas`. A schema change needs:

1. The entity change, and `ResourceDatabase.VERSION` raised by one.
2. A `Migration` from the previous version, added to the end of `ResourceDatabase.STEPS`.
3. A build, which exports the new schema file to `engine/schemas`. Commit it.
4. The new schema's statements in `ExportedSchemas` in `commonTest`. `ExportedSchemasTest` checks
   they match the exported file.
5. A test in `ResourceDatabaseMigrationTest` that builds the old schema, inserts rows, migrates and
   checks the rows. It runs on the plain and the encrypted database.

Never edit an exported schema or an existing migration. Apps in the field have databases at every
old version.

## Publishing

To publish a new release, first update `mavenVersion` in `gradle.properties` to the new version. Then
follow one of the methods below.

### Maven Local

To publish artifacts to your local Maven repository (`~/.m2/repository`) for local development and
testing, run:

```bash
./gradlew :engine:publishToMavenLocal
```

The build signs every artifact, even for a local publish, so this needs the GPG `signing.*`
properties described in [Publishing to Maven Central manually](#publishing-to-maven-central-manually).
Without them it fails with `Cannot perform signing task ... because it has no configured signatory`.

### Maven Central

Publishing to Maven Central requires two sets of credentials:

1. Maven Central credentials: your Sonatype portal username and password tokens.
2. GPG signing: a GPG key and its passphrase, used to sign all published artifacts.

See the
[Kotlin Multiplatform Publishing Guide](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries-to-maven.html)
and the
[Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-portal-guide/) for
more information on how to set up these credentials.

### Publishing to Maven Central manually

For manual publishing, store the credentials in the global `~/.gradle/gradle.properties` in your
environment (not the project's `gradle.properties`) so they are never committed to the repository:

```properties
# Maven Central Credentials
mavenCentralUsername=YOUR_USERNAME_TOKEN
mavenCentralPassword=YOUR_PASSWORD_TOKEN

# GPG Signing (file-based)
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_KEY_PASSWORD
signing.secretKeyRingFile=/path/to/secring.gpg
```

Then run:

```bash
./gradlew :engine:publishToMavenCentral
```

### Publishing to Maven Central using GitHub Actions

The project includes a GitHub Actions [workflow](.github/workflows/publish.yml) that publishes to
Maven Central when a new GitHub release (or pre-release) is created.

The workflow requires the following GitHub organization or repository secrets (already set up):

| Secret                   | Description                                                                           |
|:-------------------------|:--------------------------------------------------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Same as `mavenCentralUsername`                                                        |
| `MAVEN_CENTRAL_PASSWORD` | Same as `mavenCentralPassword`                                                        |
| `GPG_KEY_CONTENTS`       | Needs to be exported using the command `gpg --armor --export-secret-keys YOUR_KEY_ID` |
| `SIGNING_PASSWORD`       | Same as `signing.password`                                                            |
