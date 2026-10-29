# Parity with the android-fhir engine

The engine is a Kotlin Multiplatform port of the
[android-fhir](https://github.com/google/android-fhir) `engine` module. The FHIR model is
[kotlin-fhir](https://github.com/ohs-foundation/kotlin-fhir) instead of HAPI and the HTTP client is
Ktor instead of Retrofit. This page documents how closely the port matches the original's API and
behavior. For conformance against the FHIR specifications themselves see
[Conformance](conformance.md).

## Status legend

- ✅ Matches the original.
- ⚠️ Present but partial or diverges from the original. See *Notes*.
- ❌ Accepted but not functional yet.

## FhirEngine API

All 14 methods of the original `FhirEngine` interface are present with identical signatures in
[`FhirEngine.kt`](../engine/src/commonMain/kotlin/dev/ohs/fhir/engine/FhirEngine.kt).
These are `create`, `get`, `update`, `delete`, `search`, `count`, `syncUpload` (deprecated upstream
too), `syncDownload` (deprecated), `getLastSyncTimeStamp`, `clearDatabase`, `getLocalChanges`,
`purge` (single and bulk) and `withTransaction`. All work as in the original except one. ✅

`getLastSyncTimeStamp` always returns `null`, where android-fhir returns the time of the last sync.
Read it with `FhirEngineProvider.getFhirDataStore().readLastSyncTimestamp()` instead. ❌

Resource types come from the kotlin-fhir model. `getLastSyncTimeStamp` returns the engine's own
multiplatform [`OffsetDateTime`](../engine/src/commonMain/kotlin/dev/ohs/fhir/engine/OffsetDateTime.kt)
instead of `java.time.OffsetDateTime`.

## Configuration

[`FhirEngineConfiguration.kt`](../engine/src/commonMain/kotlin/dev/ohs/fhir/engine/FhirEngineConfiguration.kt)
keeps the original's shape. One knob is renamed and some behave differently, as the table shows.

| Knob                                                       | Status | Notes                                                                                                                  |
|:-----------------------------------------------------------|:-------|:-----------------------------------------------------------------------------------------------------------------------|
| `serverConfiguration` (baseUrl, authenticator, httpLogger) | ✅      |                                                                                                                        |
| `NetworkConfiguration` timeouts                            | ⚠️     | `readTimeOut` limits the whole request, where android-fhir limits each read. `writeTimeOut` maps to the socket timeout.|
| `customSearchParameters`                                   | ✅      | See [Custom search parameters](conformance.md#custom-search-parameters).                                               |
| `storageDirectory`                                         | ✅      | Desktop and web only. See [Platform support](conformance.md#platform-support).                                         |
| `uploadWithGzip`                                           | ⚠️     | Works on Android and Desktop. Ignored on iOS and web, which send uploads uncompressed and log a warning. See [Platform support](conformance.md#platform-support). |
| `httpCache`                                                | ⚠️     | Toggles Ktor's default in-memory cache. `CacheConfiguration.cacheDir` and `maxSize` are ignored.                       |
| `enableEncryptionIfSupported`                              | ✅      | Renamed `enableEncryption`. Android and iOS. Desktop and web throw at init. See [Platform support](conformance.md#platform-support). |
| `databaseErrorStrategy`                                    | ✅      | `RECREATE_AT_OPEN` recreates an encrypted database the current key cannot open. Unlike android-fhir, it keeps the file on other errors, such as a full disk.|
| `testMode`                                                 | ✅      | In-memory database on every platform.                                                                                  |

## Database

Same schema, file name and migrations as android-fhir. An app switching from android-fhir keeps its
data. ✅

Databases from releases 2.0.0-alpha01 to alpha04 are converted in place and keep their data. ✅

Switching is one way. This engine moves the database to schema version 11, and android-fhir, at
version 10, cannot open it, so an app cannot go back to android-fhir after updating. ⚠️

An app on android-fhir with encryption on can update to this engine. Its database opens with the
same Keystore key and keeps its data, including changes not uploaded yet. Verified by updating an
app from android-fhir 1.2.0 to this engine on an API 36 emulator. ✅
