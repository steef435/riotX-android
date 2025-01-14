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
package im.vector.riotx.features.home.room.list.actions

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetRoomPreviewItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetSeparatorItem
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

/**
 * Epoxy controller for room list actions
 */
class RoomListQuickActionsEpoxyController @Inject constructor(private val avatarRenderer: AvatarRenderer)
    : TypedEpoxyController<RoomListQuickActionsState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomListQuickActionsState) {
        val roomSummary = state.roomSummary() ?: return

        // Preview
        bottomSheetRoomPreviewItem {
            id("preview")
            avatarRenderer(avatarRenderer)
            roomName(roomSummary.displayName)
            avatarUrl(roomSummary.avatarUrl)
            roomId(roomSummary.roomId)
            settingsClickListener(View.OnClickListener { listener?.didSelectMenuAction(RoomListQuickActionsSharedAction.Settings(roomSummary.roomId)) })
        }

        // Notifications
        bottomSheetSeparatorItem {
            id("notifications_separator")
        }

        val selectedRoomState = state.roomNotificationState()
        RoomListQuickActionsSharedAction.NotificationsAllNoisy(roomSummary.roomId).toBottomSheetItem(0, selectedRoomState)
        RoomListQuickActionsSharedAction.NotificationsAll(roomSummary.roomId).toBottomSheetItem(1, selectedRoomState)
        RoomListQuickActionsSharedAction.NotificationsMentionsOnly(roomSummary.roomId).toBottomSheetItem(2, selectedRoomState)
        RoomListQuickActionsSharedAction.NotificationsMute(roomSummary.roomId).toBottomSheetItem(3, selectedRoomState)

        // Leave
        bottomSheetSeparatorItem {
            id("leave_separator")
        }
        RoomListQuickActionsSharedAction.Leave(roomSummary.roomId).toBottomSheetItem(5)
    }

    private fun RoomListQuickActionsSharedAction.toBottomSheetItem(index: Int, roomNotificationState: RoomNotificationState? = null) {
        val selected = when (this) {
            is RoomListQuickActionsSharedAction.NotificationsAllNoisy     -> roomNotificationState == RoomNotificationState.ALL_MESSAGES_NOISY
            is RoomListQuickActionsSharedAction.NotificationsAll          -> roomNotificationState == RoomNotificationState.ALL_MESSAGES
            is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> roomNotificationState == RoomNotificationState.MENTIONS_ONLY
            is RoomListQuickActionsSharedAction.NotificationsMute         -> roomNotificationState == RoomNotificationState.MUTE
            is RoomListQuickActionsSharedAction.Settings,
            is RoomListQuickActionsSharedAction.Leave                     -> false
        }
        return bottomSheetActionItem {
            id("action_$index")
            selected(selected)
            iconRes(iconResId)
            textRes(titleRes)
            destructive(this@toBottomSheetItem.destructive)
            listener(View.OnClickListener { listener?.didSelectMenuAction(this@toBottomSheetItem) })
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickAction: RoomListQuickActionsSharedAction)
    }
}
