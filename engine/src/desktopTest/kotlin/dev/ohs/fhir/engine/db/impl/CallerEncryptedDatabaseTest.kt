/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ohs.fhir.engine.db.impl

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ohs.fhir.engine.FhirEngineConfiguration
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.index.ResourceIndexer
import dev.ohs.fhir.engine.index.SearchParamDefinitionsProviderImpl
import dev.ohs.fhir.engine.testPlatformContext
import dev.ohs.fhir.engine.testStorageDirectory
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Desktop and web ship no encrypting SQLite, so the caller brings the driver. These run on desktop,
 * the wiring they cover is common to both.
 *
 * The driver here records and delegates without encrypting, so these prove the round trip. The
 * engine hands its database to the driver it was given, names the file as encrypted, and reads back
 * what it wrote. Whether a real driver keeps the bytes unreadable is untested.
 */
class CallerEncryptedDatabaseTest {
  private val storageDirectory = testStorageDirectory()

  @AfterTest
  fun tearDown() {
    FhirEngineProvider.clearInstance()
  }

  @Test
  fun encrypt_withACallerDriver_opensThroughItAndUsesTheEncryptedFileName() = runTest {
    val driver = RecordingDriver()
    openDatabase(driver).let { database ->
      try {
        database.insert(PATIENT)
        assertEquals(PATIENT_ID, database.select(ResourceType.Patient, PATIENT_ID).id)
      } finally {
        database.close()
      }
    }

    assertTrue(driver.openedFiles.isNotEmpty(), "the engine never opened through the driver")
    assertTrue(
      driver.openedFiles.all { it.endsWith(ENCRYPTED_DATABASE_NAME) },
      "opened ${driver.openedFiles}",
    )
  }

  @Test
  fun encrypt_withoutACallerDriver_isRefused() {
    val failure = assertFailsWith<IllegalArgumentException> { openDatabase(driver = null) }
    assertTrue(
      failure.message!!.contains("encryptedDatabaseDriver"),
      "the message should name the property to set, was ${failure.message}",
    )
  }

  @Test
  fun init_withEncryption_andACallerDriver_isAccepted() {
    FhirEngineProvider.init(
      FhirEngineConfiguration(
        enableEncryptionIfSupported = true,
        storageDirectory = storageDirectory,
        encryptedDatabaseDriver = RecordingDriver(),
      ),
      testPlatformContext(),
    )
    assertTrue(FhirEngineProvider.isInitialized())
  }

  private fun openDatabase(driver: SQLiteDriver?) =
    DatabaseImpl(
      testPlatformContext(),
      ResourceIndexer(SearchParamDefinitionsProviderImpl()),
      storageDirectory,
      DatabaseConfig(encrypt = true, encryptedDatabaseDriver = driver),
    )

  /** Stands in for an application's encrypting driver, and remembers what it was asked to open. */
  private class RecordingDriver : SQLiteDriver {
    val openedFiles = mutableListOf<String>()
    private val delegate = BundledSQLiteDriver()

    override val hasConnectionPool: Boolean
      get() = delegate.hasConnectionPool

    override fun open(fileName: String): SQLiteConnection {
      openedFiles += fileName
      return delegate.open(fileName)
    }
  }

  private companion object {
    const val PATIENT_ID = "caller-encrypted-patient"
    val PATIENT = Patient(id = PATIENT_ID)
  }
}
