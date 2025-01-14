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

package im.vector.matrix.android.internal.auth.db

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.di.AuthDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import timber.log.Timber
import javax.inject.Inject

internal class RealmSessionParamsStore @Inject constructor(private val mapper: SessionParamsMapper,
                                                           @AuthDatabase
                                                           private val realmConfiguration: RealmConfiguration
) : SessionParamsStore {

    override fun getLast(): SessionParams? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(SessionParamsEntity::class.java)
                    .findAll()
                    .map { mapper.map(it) }
                    .lastOrNull()
        }
    }

    override fun get(userId: String): SessionParams? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.USER_ID, userId)
                    .findAll()
                    .map { mapper.map(it) }
                    .firstOrNull()
        }
    }

    override fun getAll(): List<SessionParams> {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(SessionParamsEntity::class.java)
                    .findAll()
                    .mapNotNull { mapper.map(it) }
        }
    }

    override suspend fun save(sessionParams: SessionParams) {
        awaitTransaction(realmConfiguration) {
            val entity = mapper.map(sessionParams)
            if (entity != null) {
                try {
                    it.insert(entity)
                } catch (e: RealmPrimaryKeyConstraintException) {
                    Timber.e(e, "Something wrong happened during previous session creation. Override with new credentials")
                    it.insertOrUpdate(entity)
                }
            }
        }
    }

    override suspend fun delete(userId: String) {
        awaitTransaction(realmConfiguration) {
            it.where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.USER_ID, userId)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }

    override suspend fun deleteAll() {
        awaitTransaction(realmConfiguration) {
            it.where(SessionParamsEntity::class.java)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }
}
