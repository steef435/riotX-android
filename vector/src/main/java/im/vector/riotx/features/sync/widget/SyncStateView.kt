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

package im.vector.riotx.features.sync.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.riotx.R
import kotlinx.android.synthetic.main.view_sync_state.view.*

class SyncStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_sync_state, this)
    }

    fun render(newState: SyncState) {
        syncStateProgressBar.visibility = when (newState) {
            is SyncState.RUNNING -> if (newState.afterPause) View.VISIBLE else View.GONE
            else                 -> View.GONE
        }
        syncStateNoNetwork.isVisible = newState == SyncState.NO_NETWORK
    }
}
