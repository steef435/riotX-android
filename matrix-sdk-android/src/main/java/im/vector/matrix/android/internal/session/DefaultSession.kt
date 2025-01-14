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

package im.vector.matrix.android.internal.session

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.failure.ConsentNotGivenError
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.InitialSyncProgressService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilitiesService
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.securestorage.SecureStorageService
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import im.vector.matrix.android.internal.session.sync.job.SyncWorker
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@SessionScope
internal class DefaultSession @Inject constructor(override val sessionParams: SessionParams,
                                                  private val context: Context,
                                                  private val liveEntityObservers: Set<@JvmSuppressWildcards LiveEntityObserver>,
                                                  private val sessionListeners: SessionListeners,
                                                  private val roomService: Lazy<RoomService>,
                                                  private val roomDirectoryService: Lazy<RoomDirectoryService>,
                                                  private val groupService: Lazy<GroupService>,
                                                  private val userService: Lazy<UserService>,
                                                  private val filterService: Lazy<FilterService>,
                                                  private val cacheService: Lazy<CacheService>,
                                                  private val signOutService: Lazy<SignOutService>,
                                                  private val pushRuleService: Lazy<PushRuleService>,
                                                  private val pushersService: Lazy<PushersService>,
                                                  private val cryptoService: Lazy<DefaultCryptoService>,
                                                  private val fileService: Lazy<FileService>,
                                                  private val secureStorageService: Lazy<SecureStorageService>,
                                                  private val syncThreadProvider: Provider<SyncThread>,
                                                  private val contentUrlResolver: ContentUrlResolver,
                                                  private val contentUploadProgressTracker: ContentUploadStateTracker,
                                                  private val initialSyncProgressService: Lazy<InitialSyncProgressService>,
                                                  private val homeServerCapabilitiesService: Lazy<HomeServerCapabilitiesService>)
    : Session,
        RoomService by roomService.get(),
        RoomDirectoryService by roomDirectoryService.get(),
        GroupService by groupService.get(),
        UserService by userService.get(),
        CryptoService by cryptoService.get(),
        SignOutService by signOutService.get(),
        FilterService by filterService.get(),
        PushRuleService by pushRuleService.get(),
        PushersService by pushersService.get(),
        FileService by fileService.get(),
        InitialSyncProgressService by initialSyncProgressService.get(),
        SecureStorageService by secureStorageService.get(),
        HomeServerCapabilitiesService by homeServerCapabilitiesService.get() {

    private var isOpen = false

    private var syncThread: SyncThread? = null

    @MainThread
    override fun open() {
        assertMainThread()
        assert(!isOpen)
        isOpen = true
        liveEntityObservers.forEach { it.start() }
        EventBus.getDefault().register(this)
    }

    override fun requireBackgroundSync() {
        SyncWorker.requireBackgroundSync(context, myUserId)
    }

    override fun startAutomaticBackgroundSync(repeatDelay: Long) {
        SyncWorker.automaticallyBackgroundSync(context, myUserId, 0, repeatDelay)
    }

    override fun stopAnyBackgroundSync() {
        SyncWorker.stopAnyBackgroundSync(context)
    }

    override fun startSync(fromForeground: Boolean) {
        Timber.i("Starting sync thread")
        assert(isOpen)
        val localSyncThread = getSyncThread()
        localSyncThread.setInitialForeground(fromForeground)
        if (!localSyncThread.isAlive) {
            localSyncThread.start()
        } else {
            localSyncThread.restart()
            Timber.w("Attempt to start an already started thread")
        }
    }

    override fun stopSync() {
        assert(isOpen)
        syncThread?.kill()
        syncThread = null
    }

    override fun close() {
        assert(isOpen)
        stopSync()
        liveEntityObservers.forEach { it.dispose() }
        cryptoService.get().close()
        isOpen = false
        EventBus.getDefault().unregister(this)
    }

    override fun syncState(): LiveData<SyncState> {
        return getSyncThread().liveState()
    }

    private fun getSyncThread(): SyncThread {
        return syncThread ?: syncThreadProvider.get().also {
            syncThread = it
        }
    }

    override fun clearCache(callback: MatrixCallback<Unit>) {
        stopSync()
        stopAnyBackgroundSync()
        cacheService.get().clearCache(object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                startSync(true)
                callback.onSuccess(data)
            }

            override fun onFailure(failure: Throwable) {
                startSync(true)
                callback.onFailure(failure)
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConsentNotGivenError(consentNotGivenError: ConsentNotGivenError) {
        sessionListeners.dispatchConsentNotGiven(consentNotGivenError)
    }

    override fun contentUrlResolver() = contentUrlResolver

    override fun contentUploadProgressTracker() = contentUploadProgressTracker

    override fun addListener(listener: Session.Listener) {
        sessionListeners.addListener(listener)
    }

    override fun removeListener(listener: Session.Listener) {
        sessionListeners.removeListener(listener)
    }

    // Private methods *****************************************************************************

    private fun assertMainThread() {
        if (Looper.getMainLooper().thread !== Thread.currentThread()) {
            throw IllegalStateException("This method can only be called on the main thread!")
        }
    }
}
