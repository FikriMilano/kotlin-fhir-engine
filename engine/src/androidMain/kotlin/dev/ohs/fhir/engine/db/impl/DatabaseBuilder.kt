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

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers

internal actual fun getDatabaseBuilder(
  platformContext: Any,
  storageDirectory: String?,
  config: DatabaseConfig,
): RoomDatabase.Builder<ResourceDatabase> {
  val builder =
    if (config.inMemory) {
      Room.inMemoryDatabaseBuilder<ResourceDatabase>()
    } else {
      Room.databaseBuilder<ResourceDatabase>(
        platformContext as Context,
        databaseFileName(platformContext, storageDirectory, config.encrypt),
      )
    }
  return builder.setDriver(databaseDriver(config)).setQueryCoroutineContext(Dispatchers.IO)
}

internal actual val isDatabaseEncryptionSupported: Boolean = false

internal actual fun databaseDriver(config: DatabaseConfig): SQLiteDriver = BundledSQLiteDriver()

internal actual fun databaseFileName(
  platformContext: Any,
  storageDirectory: String?,
  encrypted: Boolean,
): String =
  (platformContext as Context)
    .getDatabasePath(if (encrypted) ENCRYPTED_DATABASE_NAME else DATABASE_NAME)
    .absolutePath

internal actual fun databaseFileExists(
  platformContext: Any,
  storageDirectory: String?,
  encrypted: Boolean,
): Boolean = File(databaseFileName(platformContext, storageDirectory, encrypted)).exists()

internal actual fun createDatabaseDirectory(platformContext: Any, storageDirectory: String?) {}
