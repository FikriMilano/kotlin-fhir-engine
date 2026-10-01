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

import dev.ohs.fhir.engine.unopenedDriver
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Runs on every platform, including the two that encrypt on their own. A driver from the caller has
 * to win there too, which is what the configuration documents. Nothing is opened, so no platform
 * driver, keystore or worker is touched.
 *
 * This covers which driver the engine picks and nothing more. No test ships a driver that really
 * encrypts, so CallerEncryptedDatabaseTest covers the round trip with one that does not.
 */
class DatabaseDriverSelectionTest {
  @Test
  fun encrypt_withACallerDriver_usesThatDriver() {
    val driver = unopenedDriver()
    assertSame(
      driver,
      databaseDriver(DatabaseConfig(encrypt = true, encryptedDatabaseDriver = driver)),
    )
  }
}
