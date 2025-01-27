/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.signout

import android.content.Context
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.crypto.CryptoModule
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.di.*
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.SessionModule
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal interface SignOutTask : Task<Unit, Unit>

internal class DefaultSignOutTask @Inject constructor(private val context: Context,
                                                      @UserId private val userId: String,
                                                      private val signOutAPI: SignOutAPI,
                                                      private val sessionManager: SessionManager,
                                                      private val sessionParamsStore: SessionParamsStore,
                                                      @SessionDatabase private val clearSessionDataTask: ClearCacheTask,
                                                      @CryptoDatabase private val clearCryptoDataTask: ClearCacheTask,
                                                      @UserCacheDirectory private val userFile: File,
                                                      private val realmKeysUtils: RealmKeysUtils,
                                                      @SessionDatabase private val realmSessionConfiguration: RealmConfiguration,
                                                      @CryptoDatabase private val realmCryptoConfiguration: RealmConfiguration,
                                                      @UserMd5 private val userMd5: String) : SignOutTask {

    override suspend fun execute(params: Unit) {
        Timber.d("SignOut: send request...")
        executeRequest<Unit> {
            apiCall = signOutAPI.signOut()
        }

        Timber.d("SignOut: release session...")
        sessionManager.releaseSession(userId)

        Timber.d("SignOut: cancel pending works...")
        WorkManagerUtil.cancelAllWorks(context)

        Timber.d("SignOut: delete session params...")
        sessionParamsStore.delete(userId)

        Timber.d("SignOut: clear session data...")
        clearSessionDataTask.execute(Unit)

        Timber.d("SignOut: clear crypto data...")
        clearCryptoDataTask.execute(Unit)

        Timber.d("SignOut: clear file system")
        userFile.deleteRecursively()

        Timber.d("SignOut: clear the database keys")
        realmKeysUtils.clear(SessionModule.DB_ALIAS_PREFIX + userMd5)
        realmKeysUtils.clear(CryptoModule.DB_ALIAS_PREFIX + userMd5)

        // Sanity check
        if (BuildConfig.DEBUG) {
            Realm.getGlobalInstanceCount(realmSessionConfiguration)
                    .takeIf { it > 0 }
                    ?.let { Timber.e("All realm instance for session has not been closed ($it)") }
            Realm.getGlobalInstanceCount(realmCryptoConfiguration)
                    .takeIf { it > 0 }
                    ?.let { Timber.e("All realm instance for crypto has not been closed ($it)") }
        }
    }
}
