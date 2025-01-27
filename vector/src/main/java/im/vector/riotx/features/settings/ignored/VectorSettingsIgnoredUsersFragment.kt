/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.settings.ignored

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import javax.inject.Inject

class VectorSettingsIgnoredUsersFragment @Inject constructor(
        val ignoredUsersViewModelFactory: IgnoredUsersViewModel.Factory,
        private val ignoredUsersController: IgnoredUsersController,
        private val errorFormatter: ErrorFormatter
) : VectorBaseFragment(), IgnoredUsersController.Callback {

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    private val ignoredUsersViewModel: IgnoredUsersViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        waiting_view_status_text.setText(R.string.please_wait)
        waiting_view_status_text.isVisible = true
        ignoredUsersController.callback = this
        recyclerView.configureWith(ignoredUsersController)
        ignoredUsersViewModel.requestErrorLiveData.observeEvent(this) {
            displayErrorDialog(it)
        }
    }

    override fun onDestroyView() {
        ignoredUsersController.callback = null
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_ignored_users)
    }

    override fun onUserIdClicked(userId: String) {
        AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.settings_unignore_user, userId))
                .setPositiveButton(R.string.yes) { _, _ ->
                    ignoredUsersViewModel.handle(IgnoredUsersAction.UnIgnore(userId))
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun displayErrorDialog(throwable: Throwable) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    // ==============================================================================================================
    // ignored users list management
    // ==============================================================================================================

    override fun invalidate() = withState(ignoredUsersViewModel) { state ->
        ignoredUsersController.update(state)

        handleUnIgnoreRequestStatus(state.unIgnoreRequest)
    }

    private fun handleUnIgnoreRequestStatus(unIgnoreRequest: Async<Unit>) {
        when (unIgnoreRequest) {
            is Loading -> waiting_view.isVisible = true
            else       -> waiting_view.isVisible = false
        }
    }
}
