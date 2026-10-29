# Kotlin FHIR Engine

[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine?color=yellow&label=fhir-engine)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine)
[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine-android?color=yellow&label=android)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine-android)
[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine-desktop?color=yellow&label=desktop)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine-desktop)
[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine-wasm-js?color=yellow&label=wasm-js)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine-wasm-js)
[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine-js?color=yellow&label=js)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine-js)
[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine-iossimulatorarm64?color=yellow&label=iossimulatorarm64)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine-iossimulatorarm64)
[![Release](https://img.shields.io/maven-central/v/dev.ohs.fhir/fhir-engine-iosarm64?color=yellow&label=iosarm64)](https://central.sonatype.com/artifact/dev.ohs.fhir/fhir-engine-iosarm64)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Kotlin Multiplatform library for building offline-capable healthcare apps with
[HL7 FHIR R4](https://hl7.org/fhir/R4/). It stores FHIR resources on the device, searches them with
a type-safe API, and synchronizes them with a FHIR server. It is a multiplatform port of the
[android-fhir](https://github.com/google/android-fhir) engine and uses the same database, so an app
that switches keeps its data.

The library is in alpha. The API can still change between releases.

## Contents

- [Features](#features)
- [Platform support](#platform-support)
- [Getting started](#getting-started)
- [Storing and reading resources](#storing-and-reading-resources)
- [Searching](#searching)
- [Syncing with a FHIR server](#syncing-with-a-fhir-server)
- [Encryption](#encryption)
- [Configuration reference](#configuration-reference)
- [How the engine stores data](#how-the-engine-stores-data)
- [Demo app](#demo-app)
- [More documentation](#more-documentation)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Local storage.** Create, read, update and delete FHIR resources offline. Every change is
  recorded so it can be uploaded later.
- **Search.** A Kotlin DSL for FHIR search with filters, sorting, paging, `_include`,
  `_revinclude` and `_has`, plus x-fhir-query strings.
- **Sync.** Download from and upload to a FHIR server, with conflict resolution. Android schedules
  it with WorkManager. Other platforms call it from their own scheduler.
- **Encryption.** SQLCipher on Android and iOS.
- **Migrations.** Databases from android-fhir and from earlier releases of this library are
  migrated in place and keep their data.

## Platform support

| Platform                           | Gradle target       | Artifact suffix      | Encryption | Sync scheduling                  |
|:-----------------------------------|:--------------------|:---------------------|:-----------|:---------------------------------|
| Android (minSdk 26)                | `android`           | `-android`           | ✅          | WorkManager, built in            |
| iOS (device)                       | `iosArm64`          | `-iosarm64`          | ✅          | By the app, with `runSync`       |
| iOS (Apple silicon simulator)      | `iosSimulatorArm64` | `-iossimulatorarm64` | ✅          | By the app, with `runSync`       |
| Kotlin/JVM (desktop)               | `jvm("desktop")`    | `-desktop`           | ❌          | By the app, with `runSync`       |
| Kotlin/Wasm                        | `wasmJs`            | `-wasm-js`           | ❌          | By the app, with `runSync`       |
| Kotlin/JS                          | `js`                | `-js`                | ❌          | By the app, with `runSync`       |

The library targets FHIR R4 (v4.0.1). [Conformance](docs/conformance.md) lists what is supported
from the FHIR [Search](https://hl7.org/fhir/R4/search.html) and
[RESTful API](https://hl7.org/fhir/R4/http.html) specifications, per platform.
[android-fhir parity](docs/android-fhir-parity.md) compares this library with the android-fhir
engine.

## Getting started

### Install

Add `mavenCentral()`[^1] to your repositories:

[^1]: Early versions of this library were published under the group ID `com.google.android.fhir` and
artifact ID `engine` on [Google Maven](https://maven.google.com/web/index.html#com.google.android.fhir:engine).

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

Then add the engine and the FHIR model library. The model provides the resource classes, such as
`Patient`, which the engine does not expose itself.

```kotlin
// Kotlin Multiplatform, e.g. shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.ohs.fhir:fhir-engine:2.0.0-alpha04")
            implementation("dev.ohs.fhir:fhir-model-r4:1.0.0-rc03")
        }
    }
}
```

```kotlin
// Android only, e.g. app/build.gradle.kts
dependencies {
    implementation("dev.ohs.fhir:fhir-engine:2.0.0-alpha04")
    implementation("dev.ohs.fhir:fhir-model-r4:1.0.0-rc03")
}
```

### Platform setup

Android and desktop need no setup.

#### iOS

The app must link SQLite, even without encryption. The engine does not include SQLite on iOS, so
the app can choose SQLCipher instead, which is how encryption works on iOS. Link the system SQLite,
or SQLCipher to encrypt (see [Encryption](#encryption)). Without either, the app fails to link with
undefined `sqlite3_` symbols. Releases up to 2.0.0-alpha04 included SQLite, so apps upgrading from
them must add this.

In Xcode, add `libsqlite3.tbd` under *Frameworks, Libraries, and Embedded Content*, or add
`-lsqlite3` to *Other Linker Flags*. Apple explains linking a library in
[Link against additional frameworks and libraries](https://developer.apple.com/documentation/xcode/customizing-the-build-phases-of-a-target#Link-against-additional-frameworks-and-libraries).
A dynamic Kotlin framework is linked by Gradle, not Xcode, so add the flag in Gradle instead:

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            linkerOpts("-lsqlite3")
        }
    }
}
```

#### Web (JS and Wasm)

The engine's SQLite worker and the page headers it needs do not reach your app through Maven, so a
web app needs three steps. The demo app (`engine-app`) has all three set up.

1. Copy
   [`package.json`](https://github.com/ohs-foundation/kotlin-fhir-engine/blob/main/engine-app/src/webMain/npm/sqlite-wasm-worker/package.json)
   and
   [`worker.js`](https://github.com/ohs-foundation/kotlin-fhir-engine/blob/main/engine-app/src/webMain/npm/sqlite-wasm-worker/worker.js)
   into your project, e.g. `app/src/webMain/npm/sqlite-wasm-worker/`.
2. Declare a matching npm dependency on the source set that compiles for your `js`/`wasmJs` targets
   (Kotlin's default hierarchy template groups both under `webMain` once both targets are declared):

   ```kotlin
   // e.g. app/build.gradle.kts
   kotlin {
     sourceSets {
       webMain.dependencies {
         implementation(
           npm(
             "sqlite-wasm-worker",
             layout.projectDirectory.dir("src/webMain/npm/sqlite-wasm-worker").asFile,
           ),
         )
       }
     }
   }
   ```
3. Copy
   [`engine-app/webpack.config.d/coop-coep.js`](https://github.com/ohs-foundation/kotlin-fhir-engine/blob/main/engine-app/webpack.config.d/coop-coep.js)
   into your own project's `webpack.config.d/` directory. Every Kotlin/JS module builds its own
   webpack config, so this doesn't propagate from `:engine` — without it, `sqlite3.oo1.OpfsDb` is
   unavailable and every database operation fails. This only covers your local dev server; your
   production host/CDN must also send the same `Cross-Origin-Opener-Policy: same-origin` and
   `Cross-Origin-Embedder-Policy: require-corp` headers.

Why: `sqlite-wasm-worker` is a local npm module inside the engine, not a published one, so Gradle
does not pass it on to apps (`@sqlite.org/sqlite-wasm`, a real npm package, is passed on). Without
steps 1 and 2 the build fails with `Module not found: Error: Can't resolve
'sqlite-wasm-worker/worker.js'`. SQLite-WASM and OPFS need `SharedArrayBuffer`, which only works on
a [cross-origin-isolated](https://web.dev/articles/coop-coep) page, hence step 3.

This is temporary. AndroidX's own
[Room 3 release notes](https://developer.android.com/jetpack/androidx/releases/room3) note that
`WebWorkerSQLiteDriver` doesn't yet ship a default worker and that "a future version of the Web
driver might contain a default worker published in NPM, making the web setup simpler." Once that
ships, this workaround — and the local `sqlite-wasm-worker` module in `:engine` itself — will no
longer be necessary.

### Quickstart

Initialize the engine once at startup, then use it anywhere.

```kotlin
import dev.ohs.fhir.engine.FhirEngineConfiguration
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.ServerConfiguration
import dev.ohs.fhir.engine.get
import dev.ohs.fhir.engine.search.search
import dev.ohs.fhir.model.r4.Patient

FhirEngineProvider.init(
    FhirEngineConfiguration(
        // Optional: the server to sync with.
        serverConfiguration = ServerConfiguration(baseUrl = "https://hapi.fhir.org/baseR4/"),
    ),
    platformContext, // Android: the application Context. Other platforms: Unit, the default.
)
val fhirEngine = FhirEngineProvider.getInstance(platformContext)

// Every engine call is a suspend function.
val ids = fhirEngine.create(Patient(id = "patient-1"))
val patient = fhirEngine.get<Patient>("patient-1")
val patients = fhirEngine.search<Patient> {}.map { it.resource }
```

`init` throws `IllegalStateException` if it is called twice. `getInstance` creates the engine on
its first call. The database opens on the first engine call, so that call can throw what opening
throws, for example `DatabaseEncryptionException`. Swift sees every engine call as `async throws`.

## Storing and reading resources

```kotlin
import dev.ohs.fhir.engine.delete
import dev.ohs.fhir.engine.get
import dev.ohs.fhir.model.r4.terminologies.ResourceType

// A resource without an id gets a random UUID. create returns the ids.
val ids = fhirEngine.create(patient, observation)

// Throws ResourceNotFoundException when the resource does not exist.
val patient = fhirEngine.get<Patient>("patient-1")

fhirEngine.update(patient)
fhirEngine.delete<Patient>("patient-1")

// Several writes that succeed or fail together.
fhirEngine.withTransaction {
    create(newPatient)
    update(existingPatient)
}
```

Every create, update and delete is stored as a local change until sync uploads it.

| Call                                        | What it does                                                                                                  |
|:--------------------------------------------|:--------------------------------------------------------------------------------------------------------------|
| `getLocalChanges(type, id)`                 | The changes to one resource that are not uploaded yet.                                                        |
| `purge(type, id, forcePurge = false)`       | Removes a resource on the device only, without telling the server. Throws if it has local changes, unless `forcePurge` is true. |
| `purge(type, ids, forcePurge = false)`      | The same for several resources, all or none.                                                                  |
| `clearDatabase()`                           | Deletes everything on the device, including changes not uploaded yet.                                         |

## Searching

```kotlin
import dev.ohs.fhir.engine.search.Order
import dev.ohs.fhir.engine.search.StringClientParam
import dev.ohs.fhir.engine.search.TokenClientParam
import dev.ohs.fhir.engine.search.search

val results = fhirEngine.search<Patient> {
    filter(StringClientParam("family"), { value = "Jones" })
    filter(TokenClientParam("gender"), { value = of("female") })
    sort(StringClientParam("given"), Order.ASCENDING)
    count = 20 // page size
    from = 0 // offset
}
val patients = results.map { it.resource }
```

- Search parameters are named as in the FHIR specification, for example `family` or `birthdate`.
  Build them with the class for their type: `StringClientParam`, `TokenClientParam`,
  `DateClientParam`, `NumberClientParam`, `QuantityClientParam`, `ReferenceClientParam` or
  `UriClientParam`.
- Separate `filter` calls must all match. Several criteria in one `filter` call match if any of
  them does.
- String filters match by prefix by default. Set `modifier` to `StringFilterModifier.MATCHES_EXACTLY`
  or `CONTAINS` to change that.
- `sort` works on string, number and date parameters.
- `fhirEngine.count<Patient> { ... }` returns the number of matches.

Each result is a `SearchResult` with the matching `resource`, plus the resources that `include` and
`revInclude` add:

```kotlin
import dev.ohs.fhir.engine.search.ReferenceClientParam
import dev.ohs.fhir.engine.search.has
import dev.ohs.fhir.engine.search.include
import dev.ohs.fhir.engine.search.revInclude

fhirEngine.search<Patient> {
    // Only patients that have a matching Observation (_has).
    has<Observation>(ReferenceClientParam("subject")) {
        filter(TokenClientParam("code"), { value = of("8867-4") })
    }
    // Also return each patient's practitioners (_include) and observations (_revinclude).
    include<Practitioner>(ReferenceClientParam("general-practitioner"))
    revInclude<Observation>(ReferenceClientParam("subject"))
}
```

`included` is keyed by the search parameter name, and `revIncluded` by the resource type and
search parameter name.

Searches can also be written as [x-fhir-query](https://hl7.org/fhir/uv/sdc/expressions.html#x-fhir-query-enhancements)
strings. Plain parameters, `_sort` and `_count` are supported. Modifiers, prefixes and chaining are
not.

```kotlin
val results = fhirEngine.search("Patient?gender=female&_sort=-family&_count=10")
```

To search on a parameter the specification does not define, add a `SearchParamDefinition` to
`customSearchParameters` in the [configuration](#configuration-reference).

## Syncing with a FHIR server

A sync first downloads the resources that changed on the server, then uploads the local changes.
When a downloaded resource also has local changes, a conflict resolver decides which version to
keep. A failed sync keeps the local changes, and the next successful sync uploads them.

Sync needs a `ServerConfiguration` in the engine configuration. Then implement `FhirSyncTask` and
schedule it.

### 1. Implement `FhirSyncTask`

```kotlin
class MyFhirSyncTask : FhirSyncTask {
    override fun getFhirEngine(): FhirEngine = FhirEngineProvider.getInstance()
    override fun getDownloadWorkManager(): DownloadWorkManager = MyDownloadWorkManager()
    override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver
    override fun getUploadStrategy(): UploadStrategy =
        UploadStrategy.forIndividualRequest(
            methodForCreate = HttpCreateMethod.PUT,
            methodForUpdate = HttpUpdateMethod.PATCH,
            squash = true,
        )
}
```

- `getDownloadWorkManager()` decides what to download.
- `getConflictResolver()` picks the version to keep: `AcceptLocalConflictResolver`,
  `AcceptRemoteConflictResolver`, or your own `ConflictResolver`.
- `getUploadStrategy()` decides how local changes are sent.

Individual requests work with every server. A bundle sends many changes in one request, which is
faster, but only works with servers that accept transaction bundles. Some reject them, for example
HAPI with request validation on, which answers HTTP 422. To use bundles:

```kotlin
UploadStrategy.forBundleRequest(
    methodForCreate = HttpCreateMethod.PUT,
    methodForUpdate = HttpUpdateMethod.PATCH,
    squash = true,
    bundleSize = 500,
)
```

### Upload strategies

[`UploadStrategy.kt`](engine/src/commonMain/kotlin/dev/ohs/fhir/engine/sync/upload/UploadStrategy.kt)
exposes two factories. All valid configurations send creates as PUT or POST and updates as PATCH.

| Configuration                                                               | Status | Notes                                                                                                                                               |
|:----------------------------------------------------------------------------|:-------|:----------------------------------------------------------------------------------------------------------------------------------------------------|
| `forBundleRequest(create=PUT\|POST, update=PATCH, squash=true, bundleSize)` | ✅      | Transaction bundles with per resource squashed patches.                                                                                             |
| `forBundleRequest(update=PUT, ...)`                                         | ❌      | Throws `NotImplementedError`.                                                                                                                       |
| `forBundleRequest(squash=false, ...)`                                       | ❌      | Throws `NotImplementedError`. Bundles require squashing.                                                                                            |
| `forIndividualRequest(create=PUT\|POST, update=PATCH, squash=true)`         | ✅      | One HTTP request per resource.                                                                                                                      |
| `forIndividualRequest(squash=false)`                                        | ❌      | Constructs but fails at sync time in [`LocalChangeFetcher.kt`](engine/src/commonMain/kotlin/dev/ohs/fhir/engine/sync/upload/LocalChangeFetcher.kt). |
| `forIndividualRequest(update=PUT, ...)`                                     | ❌      | Throws `NotImplementedError`.                                                                                                                       |

Updates are sent as JSON Patches generated by
[`JsonDiff.kt`](engine/src/commonMain/kotlin/dev/ohs/fhir/engine/db/impl/JsonDiff.kt). They use the
`add`, `replace` and `remove` operations. The `move`, `copy` and `test` operations are not
generated.

### 2. Implement `DownloadWorkManager`

`ResourceParamsBasedDownloadWorkManager` downloads every resource type you list, with your search
parameters, and only what changed since the last download. It stores that time through the
`TimestampContext` you pass:

```kotlin
ResourceParamsBasedDownloadWorkManager(
    syncParams = mapOf(ResourceType.Patient to mapOf("address-city" to "NAIROBI")),
    context = myTimestampContext,
)
```

To decide the requests yourself, implement the interface:

```kotlin
class MyDownloadWorkManager : DownloadWorkManager {
    // The next request, or null when the download is done.
    override suspend fun getNextRequest(): DownloadRequest? =
        nextUrl?.let { DownloadRequest.of(it) }

    // Count requests per resource type, used for progress. Can be empty.
    override suspend fun getSummaryRequestUrls(): Map<ResourceType, String> = emptyMap()

    // The resources to save from one response.
    override suspend fun processResponse(response: Resource): Collection<Resource> =
        (response as? Bundle)?.entry?.mapNotNull { it.resource } ?: emptyList()
}
```

The demo app's
[`TimestampBasedDownloadWorkManager`](engine-app/src/commonMain/kotlin/dev/ohs/fhir/engine/app/data/TimestampBasedDownloadWorkManager.kt)
is a full example.

### 3. Schedule sync on Android

Extend `FhirSyncWorker`, a WorkManager worker that is also a `FhirSyncTask`:

```kotlin
class MyFhirSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    FhirSyncWorker(appContext, workerParams) {
    override fun getFhirEngine() = FhirEngineProvider.getInstance(applicationContext)
    override fun getDownloadWorkManager() = MyDownloadWorkManager()
    override fun getConflictResolver() = AcceptLocalConflictResolver
    override fun getUploadStrategy() = UploadStrategy.forIndividualRequest(
        methodForCreate = HttpCreateMethod.PUT,
        methodForUpdate = HttpUpdateMethod.PATCH,
        squash = true,
    )
}
```

Call `FhirEngineProvider.getInstance` right after `init` in `Application.onCreate`. A sync worker
that starts before that fails, because `getInstance` creates the server connection.

Then start sync with `Sync`. Its functions suspend, so call them from a coroutine:

```kotlin
// One-time sync. The flow never completes, so stop at the final state.
val result = Sync.oneTimeSync<MyFhirSyncWorker>(context)
    .catch { /* the status is unknown, the sync itself may have worked */ }
    .firstOrNull {
        it is CurrentSyncJobStatus.Succeeded ||
            it is CurrentSyncJobStatus.Failed ||
            it is CurrentSyncJobStatus.Cancelled
    }

// Periodic sync every 15 minutes while the device is online.
Sync.periodicSync<MyFhirSyncWorker>(
    context,
    PeriodicSyncConfiguration(repeat = RepeatInterval(15.minutes)),
)
    .catch { }
    .collect { status -> /* show status.lastSyncJobStatus */ }

Sync.cancelOneTimeSync<MyFhirSyncWorker>(context)
Sync.cancelPeriodicSync<MyFhirSyncWorker>(context)
```

- Always put `.catch` on the status flow. It can throw
  `IllegalStateException: Inconsistent terminal syncJobStatus` while the sync itself is fine.
- Keep the default `retryConfiguration` and `existingWorkPolicy`. With `retryConfiguration = null`
  the flow throws when the sync ends. `ExistingWorkPolicy.APPEND` can throw too, and
  `APPEND_OR_REPLACE` can report an earlier sync's result.
- With the default retries, an offline sync shows `Enqueued` between attempts. `Failed` comes only
  after three retries, about three minutes later.

### 3. Schedule sync on iOS, desktop and web

On these platforms the app schedules sync itself. Call `runSync` on your `FhirSyncTask` from your
own scheduler:

```kotlin
val status: SyncJobStatus = task.runSync(taskName = "sync") { progress -> /* Started, InProgress */ }
val succeeded = status is SyncJobStatus.Succeeded
```

`runSync` returns `SyncJobStatus.Succeeded` or `SyncJobStatus.Failed`. A failed download or upload
is returned, not thrown, but the call can still throw, for example when no `ServerConfiguration` is
set. The demo app has sample schedulers:

- **iOS** uses `BGProcessingTask` in
  [`IosBgSyncScheduler`](engine-app/src/iosMain/kotlin/dev/ohs/fhir/engine/app/data/IosBgSyncScheduler.kt),
  with the task in
  [`DemoFhirSyncTask.ios.kt`](engine-app/src/iosMain/kotlin/dev/ohs/fhir/engine/app/data/DemoFhirSyncTask.ios.kt).
  The app's `Info.plist` needs `UIBackgroundModes` with `processing`, and
  `BGTaskSchedulerPermittedIdentifiers` with the task identifier.
- **Desktop and web** use a coroutine scheduler in
  [`Sync.kt`](engine-app/src/foregroundSyncMain/kotlin/dev/ohs/fhir/engine/app/data/Sync.kt), with
  the task in
  [`DemoFhirSyncTask.desktop.kt`](engine-app/src/desktopMain/kotlin/dev/ohs/fhir/engine/app/data/DemoFhirSyncTask.desktop.kt).
  Sync only runs while the app is running.

### Sync status

On Android, `Sync` returns a flow of status updates:

| Status                           | Description                            |
|:---------------------------------|:---------------------------------------|
| `CurrentSyncJobStatus.Enqueued`  | Job is queued, not yet started         |
| `CurrentSyncJobStatus.Running`   | Sync is in progress                    |
| `CurrentSyncJobStatus.Succeeded` | Sync completed successfully            |
| `CurrentSyncJobStatus.Failed`    | Sync failed (after configured retries) |
| `CurrentSyncJobStatus.Cancelled` | Sync was cancelled                     |
| `CurrentSyncJobStatus.Blocked`   | Waiting for another job to finish      |

For periodic sync, `PeriodicSyncJobStatus` combines `currentSyncJobStatus` with `lastSyncJobStatus`, the terminal result of the most recently completed cycle.

`runSync` reports progress with `SyncJobStatus`:

| Status                                              | Description                              |
|:----------------------------------------------------|:-----------------------------------------|
| `SyncJobStatus.Started`                             | Sync started                             |
| `SyncJobStatus.InProgress(syncOperation, total, completed)` | Downloading or uploading, with counts |
| `SyncJobStatus.Succeeded`                           | Sync completed successfully              |
| `SyncJobStatus.Failed(exceptions)`                  | Sync failed, with one exception per failed resource |

### Compressing uploads (gzip)

`NetworkConfiguration(uploadWithGzip = true)` compresses upload request bodies with gzip.

This only works on Android and Desktop. Ktor can only compress request bodies on the JVM. On iOS
and js/wasmJs it would label the body as gzip but send it uncompressed, and the server would
reject it. The engine therefore ignores the flag on those platforms, sends uploads uncompressed,
and logs a warning. Tracked upstream as
[KTOR-8198](https://youtrack.jetbrains.com/issue/KTOR-8198).

Downloads are not affected. Android, iOS and web accept gzip responses. Desktop asks for them only
when `uploadWithGzip` is on, and otherwise downloads uncompressed.

### Last sync time

`FhirEngine.getLastSyncTimeStamp()` is not implemented yet and always returns `null`. Read the time
of the last sync attempt, successful or not, with
`FhirEngineProvider.getFhirDataStore().readLastSyncTimestamp()`, or with
`Sync.getLastSyncTimestamp(context)` on Android.

## Encryption

Set `enableEncryption = true` to store the database encrypted with
[SQLCipher](https://www.zetetic.net/sqlcipher/). The encrypted database is `resources_encrypted.db`.

### What each platform does

- **Android.** Works out of the box. The passphrase is derived from a key in the Android Keystore,
  the same way the android-fhir engine does it, so an app updating from android-fhir keeps its
  encrypted data.
- **iOS.** Add the [SQLCipher.swift](https://github.com/sqlcipher/SQLCipher.swift) package to the
  app target in place of `libsqlite3.tbd`, as Apple describes in
  [Adding package dependencies to your app](https://developer.apple.com/documentation/xcode/adding-package-dependencies-to-your-app).
  Build the Kotlin framework as static, so the app's link decides which SQLite the engine calls. The key is 32 random bytes kept in the Keychain, which
  only works in a signed build. If the app links the system SQLite instead, the first database call
  throws `DatabaseEncryptionException` with `errorCode` `UNSUPPORTED`.
- **Desktop and web.** Not supported. `FhirEngineProvider.init` throws
  `UnsupportedOperationException` rather than silently storing plaintext. See
  [Desktop and web](#desktop-and-web) for why, and what support would need.

Opening a stored database with the other setting throws `IllegalStateException`. If the key is
lost, the first database call throws `SQLiteException`, or, with
`DatabaseErrorStrategy.RECREATE_AT_OPEN`, the database is deleted and created again empty.

If the Keystore or Keychain cannot provide the key, the first database call throws
`DatabaseEncryptionException`. An `errorCode` of `TIMEOUT` means the key store stayed busy or
locked through the engine's retries, so a later call can succeed. `UNSUPPORTED` and `UNKNOWN` will
not clear on their own.

The key never leaves the device, so a backup restored to another device cannot be read. On Android,
exclude `resources_encrypted.db` from your app's backup rules so a restored app starts empty. On iOS
the engine already excludes it. Local changes not uploaded before the restore are lost either way.

An emptied database does not refill on the next sync. Your `DownloadWorkManager` asks only for
resources changed since the timestamps it keeps, so reset them when the database starts empty, and
leave them out of backups too, or everything older is never downloaded again.

### Desktop and web

The engine opens its database through Room, which reaches SQLite through an androidx.sqlite
`SQLiteDriver`. Encryption needs a SQLite build with a cipher behind that driver, and a safe place
for the key. Android and iOS have both. Desktop and web have neither ready to use.

Room does not help here. No driver that androidx.sqlite ships encrypts, and neither Room's
[Kotlin Multiplatform guide](https://developer.android.com/kotlin/multiplatform/room), the
[androidx.sqlite guide](https://developer.android.com/kotlin/multiplatform/sqlite) nor the
[Room 3.0 announcement](https://android-developers.googleblog.com/2026/03/room-30-modernizing-room.html)
mentions encryption. On Android and iOS the engine puts SQLCipher behind those drivers itself.

**Desktop.** The engine uses `BundledSQLiteDriver`, which compiles a plain SQLite into its native
library. No encrypting SQLite ships as a `SQLiteDriver` for the JVM. It exists over JDBC instead,
for example [sqlite-jdbc-crypt](https://github.com/Willena/sqlite-jdbc-crypt), which is built on
[SQLite3MultipleCiphers](https://github.com/utelle/SQLite3MultipleCiphers) and can write SQLCipher
compatible files. Support would need:

1. A `SQLiteDriver` over that JDBC driver, or over a JNI binding to SQLCipher, tested against how
   Room uses connections: several at once, write-ahead logging and foreign keys.
2. A key kept in the operating system's store: the macOS Keychain, the Windows Credential Manager or
   the Linux Secret Service.
3. A native library for each desktop operating system the engine runs on.

Until then, full disk encryption (FileVault, BitLocker, LUKS) protects the database file at rest
with no change to the engine.

**Web.** Neither SQLCipher nor any other encrypting SQLite publishes a WebAssembly build ready to
use. The commercial [SQLite Encryption Extension](https://sqlite.org/wasm/doc/trunk/see.md) and
SQLite3MultipleCiphers can both be compiled into sqlite-wasm, but neither has been tested with the
origin private file system, where the engine stores its database. Support would need:

1. One of those WebAssembly builds, built and maintained by the project, with the engine's database
   worker on top.
2. A place for the key. Browsers have no secure key store, so the key would come from a passphrase
   the user types, from the server after sign-in, or wrapped by a WebCrypto key that cannot be
   exported. None of these stops script running on the same origin, so on the web, encryption
   mainly protects against someone copying the browser profile.

## Configuration reference

`FhirEngineConfiguration`, passed to `FhirEngineProvider.init`:

| Field                    | Default       | Description                                                                                   |
|:-------------------------|:--------------|:----------------------------------------------------------------------------------------------|
| `serverConfiguration`    | `null`        | The server to sync with. Sync needs it.                                                       |
| `enableEncryption`       | `false`       | Encrypts the database on Android and iOS. Throws on desktop and web. See [Encryption](#encryption). |
| `databaseErrorStrategy`  | `UNSPECIFIED` | `RECREATE_AT_OPEN` deletes and recreates an encrypted database the key cannot read.           |
| `customSearchParameters` | `null`        | Extra search parameters, as `SearchParamDefinition(name, type, path)`.                         |
| `storageDirectory`       | `null`        | Where the database is stored, on desktop and web only. Desktop defaults to `~/.fhir-engine`.  |
| `testMode`               | `false`       | Keeps the database in memory, for tests.                                                      |

`ServerConfiguration`:

| Field                  | Default                  | Description                                                     |
|:-----------------------|:-------------------------|:----------------------------------------------------------------|
| `baseUrl`              |                          | The server's base URL.                                          |
| `networkConfiguration` | `NetworkConfiguration()` | Timeouts, gzip and HTTP cache, below.                           |
| `authenticator`        | `null`                   | Returns `HttpAuthenticationMethod.Basic(username, password)` or `Bearer(token)` for each request. |
| `httpLogger`           | `HttpLogger.NONE`        | `HttpLogger(level, headersToIgnore)`, with level `NONE`, `BASIC`, `HEADERS` or `BODY`. |

`NetworkConfiguration`:

| Field               | Default | Description                                                                 |
|:--------------------|:--------|:----------------------------------------------------------------------------|
| `connectionTimeOut` | `10`    | Seconds to connect.                                                         |
| `readTimeOut`       | `10`    | Seconds for a whole request, response included. Raise it for slow networks or large pages, or sync fails while data is still arriving. |
| `writeTimeOut`      | `10`    | Seconds the connection may sit idle.                                        |
| `uploadWithGzip`    | `false` | Android and desktop only. See [Compressing uploads](#compressing-uploads-gzip). |
| `httpCache`         | `null`  | Turns on Ktor's in-memory HTTP cache. Its `cacheDir` and `maxSize` are ignored. |

Exceptions to expect:

| Exception                         | Thrown when                                                                                  |
|:----------------------------------|:---------------------------------------------------------------------------------------------|
| `ResourceNotFoundException`       | `get` finds no resource with that id.                                                        |
| `DatabaseEncryptionException`     | The Keystore or Keychain cannot provide the key, or SQLCipher is not linked on iOS. See [Encryption](#encryption). |
| `IllegalStateException`           | `init` is called twice, `getInstance` before `init`, or the stored database has the other encryption setting. |
| `UnsupportedOperationException`   | `init` asks for encryption on desktop or web.                                                |

## How the engine stores data

The engine stores resources with [Room 3](https://developer.android.com/kotlin/multiplatform/room)
(`androidx.room3`) on every platform. Room 2 has no web target. Room reaches SQLite through a
driver that differs per platform:

| Platform                     | SQLite driver                                                                                             |
|:-----------------------------|:----------------------------------------------------------------------------------------------------------|
| Android                      | `BundledSQLiteDriver`, a SQLite compiled into the library. SQLCipher's driver when encrypted.             |
| Desktop                      | `BundledSQLiteDriver`.                                                                                    |
| iOS                          | `NativeSQLiteDriver`, which calls the SQLite the app links: the system `libsqlite3.tbd` or SQLCipher.     |
| Web (`js` and `wasmJs`)      | `WebWorkerSQLiteDriver`, with SQLite compiled to WebAssembly in a Web Worker, storing the database in [OPFS](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API/Origin_private_file_system). |

The schema, file name and migrations are the same as android-fhir's, so an app that switches from
android-fhir keeps its data. Databases from 2.0.0-alpha01 to alpha04 are converted in place. See
[android-fhir parity](docs/android-fhir-parity.md).

## Demo app

The `engine-app` module is a demo app for every platform:

```bash
./gradlew :engine-app:run                              # Desktop
./gradlew :engine-app:wasmJsBrowserDevelopmentRun      # Web (Wasm)
./gradlew :engine-app:jsBrowserDevelopmentRun          # Web (JS)
./gradlew :engine-app:installDebug                     # Android
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode 26 or later and run it. Xcode builds the Kotlin
framework itself. The demo's Compose UI needs the iOS 26 SDK, the library does not. To run on a device, copy `iosApp/iosApp/Local.xcconfig.example` to `Local.xcconfig` and set
your team ID.

## More documentation

| Page                                                  | Content                                                        |
|:------------------------------------------------------|:---------------------------------------------------------------|
| [Conformance](docs/conformance.md)                    | FHIR search and RESTful API support, and support per platform. |
| [android-fhir parity](docs/android-fhir-parity.md)    | How this library compares with the android-fhir engine.        |
| [Benchmarking](docs/benchmarking.md)                  | How to run the micro benchmarks.                               |
| [Benchmark results](docs/benchmark-results.md)        | What the benchmarks found.                                     |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for building, testing, formatting, changing the database
schema and publishing.

## License

Licensed under the Apache License, Version 2.0.
