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

package im.vector.riotx.features.home.room.breadcrumbs

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.room.detail.RoomDetailSharedAction
import im.vector.riotx.features.home.room.detail.RoomDetailSharedActionViewModel
import kotlinx.android.synthetic.main.fragment_breadcrumbs.*
import javax.inject.Inject

class BreadcrumbsFragment @Inject constructor(
        private val breadcrumbsController: BreadcrumbsController,
        val breadcrumbsViewModelFactory: BreadcrumbsViewModel.Factory
) : VectorBaseFragment(), BreadcrumbsController.Listener {

    private lateinit var sharedActionViewModel: RoomDetailSharedActionViewModel
    private val breadcrumbsViewModel: BreadcrumbsViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_breadcrumbs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        sharedActionViewModel = activityViewModelProvider.get(RoomDetailSharedActionViewModel::class.java)

        breadcrumbsViewModel.subscribe { renderState(it) }
    }

    override fun onDestroyView() {
        breadcrumbsRecyclerView.cleanup()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        breadcrumbsRecyclerView.configureWith(breadcrumbsController, BreadcrumbsAnimator(), hasFixedSize = false)
        breadcrumbsController.listener = this
    }

    private fun renderState(state: BreadcrumbsViewState) {
        breadcrumbsController.update(state)
    }

    // BreadcrumbsController.Listener **************************************************************

    override fun onBreadcrumbClicked(roomId: String) {
        sharedActionViewModel.post(RoomDetailSharedAction.SwitchToRoom(roomId))
    }
}
