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

package im.vector.riotx.features.roomdirectory

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.VectorBaseFragment
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_public_rooms.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * What can be improved:
 * - When filtering more (when entering new chars), we could filter on result we already have, during the new server request, to avoid empty screen effect
 */
class PublicRoomsFragment @Inject constructor(
        private val publicRoomsController: PublicRoomsController,
        private val errorFormatter: ErrorFormatter
) : VectorBaseFragment(), PublicRoomsController.Callback {

    private val viewModel: RoomDirectoryViewModel by activityViewModel()
    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel

    override fun getLayoutResId() = R.layout.fragment_public_rooms

    override fun getMenuRes() = R.menu.menu_room_directory

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(publicRoomsToolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()

        publicRoomsFilter.queryTextChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeBy {
                    viewModel.handle(RoomDirectoryAction.FilterWith(it.toString()))
                }
                .disposeOnDestroyView()

        publicRoomsCreateNewRoom.setOnClickListener {
            sharedActionViewModel.post(RoomDirectorySharedAction.CreateRoom)
        }

        viewModel.joinRoomErrorLiveData.observeEvent(this) { throwable ->
            Snackbar.make(publicRoomsCoordinator, errorFormatter.toHumanReadable(throwable), Snackbar.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onDestroyView() {
        publicRoomsController.callback = null
        publicRoomsList.cleanup()
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_room_directory_change_protocol -> {
                sharedActionViewModel.post(RoomDirectorySharedAction.ChangeProtocol)
                true
            }
            else                                     ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val epoxyVisibilityTracker = EpoxyVisibilityTracker()
        epoxyVisibilityTracker.attach(publicRoomsList)
        publicRoomsList.configureWith(publicRoomsController)
        publicRoomsController.callback = this
    }

    override fun onPublicRoomClicked(publicRoom: PublicRoom, joinState: JoinState) {
        Timber.v("PublicRoomClicked: $publicRoom")

        when (joinState) {
            JoinState.JOINED        -> {
                navigator.openRoom(requireActivity(), publicRoom.roomId)
            }
            JoinState.NOT_JOINED,
            JoinState.JOINING_ERROR -> {
                // ROOM PREVIEW
                navigator.openRoomPreview(publicRoom, requireActivity())
            }
            else                    -> {
                Snackbar.make(publicRoomsCoordinator, getString(R.string.please_wait), Snackbar.LENGTH_SHORT)
                        .show()
            }
        }
    }

    override fun onPublicRoomJoin(publicRoom: PublicRoom) {
        Timber.v("PublicRoomJoinClicked: $publicRoom")
        viewModel.handle(RoomDirectoryAction.JoinRoom(publicRoom.roomId))
    }

    override fun loadMore() {
        viewModel.handle(RoomDirectoryAction.LoadMore)
    }

    private var initialValueSet = false

    override fun invalidate() = withState(viewModel) { state ->
        if (!initialValueSet) {
            initialValueSet = true
            if (publicRoomsFilter.query.toString() != state.currentFilter) {
                // For initial filter
                publicRoomsFilter.setQuery(state.currentFilter, false)
            }
        }

        // Populate list with Epoxy
        publicRoomsController.setData(state)
    }
}
