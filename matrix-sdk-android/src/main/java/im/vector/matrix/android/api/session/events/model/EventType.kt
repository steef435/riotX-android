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

package im.vector.matrix.android.api.session.events.model

/**
 * Constants defining known event types from Matrix specifications.
 */
object EventType {

    const val PRESENCE = "m.presence"
    const val MESSAGE = "m.room.message"
    const val STICKER = "m.sticker"
    const val ENCRYPTED = "m.room.encrypted"
    const val ENCRYPTION = "m.room.encryption"
    const val FEEDBACK = "m.room.message.feedback"
    const val TYPING = "m.typing"
    const val REDACTION = "m.room.redaction"
    const val RECEIPT = "m.receipt"
    const val TAG = "m.tag"
    const val ROOM_KEY = "m.room_key"
    const val FULLY_READ = "m.fully_read"
    const val PLUMBING = "m.room.plumbing"
    const val BOT_OPTIONS = "m.room.bot.options"
    const val PREVIEW_URLS = "org.matrix.room.preview_urls"

    // State Events

    const val STATE_ROOM_NAME = "m.room.name"
    const val STATE_ROOM_TOPIC = "m.room.topic"
    const val STATE_ROOM_AVATAR = "m.room.avatar"
    const val STATE_ROOM_MEMBER = "m.room.member"
    const val STATE_ROOM_THIRD_PARTY_INVITE = "m.room.third_party_invite"
    const val STATE_ROOM_CREATE = "m.room.create"
    const val STATE_ROOM_JOIN_RULES = "m.room.join_rules"
    const val STATE_ROOM_GUEST_ACCESS = "m.room.guest_access"
    const val STATE_ROOM_POWER_LEVELS = "m.room.power_levels"
    const val STATE_ROOM_ALIASES = "m.room.aliases"
    const val STATE_ROOM_TOMBSTONE = "m.room.tombstone"
    const val STATE_CANONICAL_ALIAS = "m.room.canonical_alias"
    const val STATE_HISTORY_VISIBILITY = "m.room.history_visibility"
    const val STATE_RELATED_GROUPS = "m.room.related_groups"
    const val STATE_PINNED_EVENT = "m.room.pinned_events"

    // Call Events

    const val CALL_INVITE = "m.call.invite"
    const val CALL_CANDIDATES = "m.call.candidates"
    const val CALL_ANSWER = "m.call.answer"
    const val CALL_HANGUP = "m.call.hangup"

    // Key share events
    const val ROOM_KEY_REQUEST = "m.room_key_request"
    const val FORWARDED_ROOM_KEY = "m.forwarded_room_key"

    // Interactive key verification
    const val KEY_VERIFICATION_START = "m.key.verification.start"
    const val KEY_VERIFICATION_ACCEPT = "m.key.verification.accept"
    const val KEY_VERIFICATION_KEY = "m.key.verification.key"
    const val KEY_VERIFICATION_MAC = "m.key.verification.mac"
    const val KEY_VERIFICATION_CANCEL = "m.key.verification.cancel"

    // Relation Events
    const val REACTION = "m.reaction"

    private val STATE_EVENTS = listOf(
            STATE_ROOM_NAME,
            STATE_ROOM_TOPIC,
            STATE_ROOM_AVATAR,
            STATE_ROOM_MEMBER,
            STATE_ROOM_THIRD_PARTY_INVITE,
            STATE_ROOM_CREATE,
            STATE_ROOM_JOIN_RULES,
            STATE_ROOM_GUEST_ACCESS,
            STATE_ROOM_POWER_LEVELS,
            STATE_ROOM_TOMBSTONE,
            STATE_HISTORY_VISIBILITY,
            STATE_RELATED_GROUPS,
            STATE_PINNED_EVENT
    )

    fun isStateEvent(type: String): Boolean {
        return STATE_EVENTS.contains(type)
    }

    fun isCallEvent(type: String): Boolean {
        return type == CALL_INVITE
                || type == CALL_CANDIDATES
                || type == CALL_ANSWER
                || type == CALL_HANGUP
    }
}
