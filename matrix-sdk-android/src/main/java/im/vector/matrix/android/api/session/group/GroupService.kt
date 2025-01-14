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

package im.vector.matrix.android.api.session.group

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.session.group.model.GroupSummary

/**
 * This interface defines methods to get groups. It's implemented at the session level.
 */
interface GroupService {

    /**
     * Get a group from a groupId
     * @param groupId the groupId to look for.
     * @return the group with groupId or null
     */
    fun getGroup(groupId: String): Group?

    /**
     * Get a live list of group summaries. This list is refreshed as soon as the data changes.
     * @return the [LiveData] of [GroupSummary]
     */
    fun liveGroupSummaries(): LiveData<List<GroupSummary>>
}
