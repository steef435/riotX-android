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

package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.R
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.sync.model.GroupsSyncResponse
import im.vector.matrix.android.internal.session.sync.model.InvitedGroupSync
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import javax.inject.Inject

internal class GroupSyncHandler @Inject constructor(private val monarchy: Monarchy) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, Any>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedGroupSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, Any>) : HandlingStrategy()
    }

    suspend fun handle(roomsSyncResponse: GroupsSyncResponse, reporter: DefaultInitialSyncProgressService? = null) {
        monarchy.awaitTransaction { realm ->
            handleGroupSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), reporter)
            handleGroupSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), reporter)
            handleGroupSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), reporter)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleGroupSync(realm: Realm, handlingStrategy: HandlingStrategy, reporter: DefaultInitialSyncProgressService?) {
        val groups = when (handlingStrategy) {
            is HandlingStrategy.JOINED  ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_groups, 0.6f) {
                    handleJoinedGroup(realm, it.key)
                }

            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_groups, 0.3f) {
                    handleInvitedGroup(realm, it.key)
                }

            is HandlingStrategy.LEFT    ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_groups, 0.1f) {
                    handleLeftGroup(realm, it.key)
                }
        }

        /** Note: [im.vector.matrix.android.internal.session.group.GroupSummaryUpdater] is observing changes */
        realm.insertOrUpdate(groups)
    }

    private fun handleJoinedGroup(realm: Realm,
                                  groupId: String): GroupEntity {
        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = Membership.JOIN
        return groupEntity
    }

    private fun handleInvitedGroup(realm: Realm,
                                   groupId: String): GroupEntity {
        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = Membership.INVITE
        return groupEntity
    }

    private fun handleLeftGroup(realm: Realm,
                                groupId: String): GroupEntity {
        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = Membership.LEAVE
        return groupEntity
    }
}
