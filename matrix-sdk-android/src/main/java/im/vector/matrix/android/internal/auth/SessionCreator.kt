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

package im.vector.matrix.android.internal.auth

import android.net.Uri
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.SessionManager
import timber.log.Timber
import javax.inject.Inject

internal interface SessionCreator {
    suspend fun createSession(credentials: Credentials, homeServerConnectionConfig: HomeServerConnectionConfig): Session
}

internal class DefaultSessionCreator @Inject constructor(
        private val sessionParamsStore: SessionParamsStore,
        private val sessionManager: SessionManager,
        private val pendingSessionStore: PendingSessionStore
) : SessionCreator {

    /**
     * Credentials can affect the homeServerConnectionConfig, override home server url and/or
     * identity server url if provided in the credentials
     */
    override suspend fun createSession(credentials: Credentials, homeServerConnectionConfig: HomeServerConnectionConfig): Session {
        // We can cleanup the pending session params
        pendingSessionStore.delete()

        val sessionParams = SessionParams(
                credentials = credentials,
                homeServerConnectionConfig = homeServerConnectionConfig.copy(
                        homeServerUri = credentials.wellKnown?.homeServer?.baseURL
                                // remove trailing "/"
                                ?.trim { it == '/' }
                                ?.takeIf { it.isNotBlank() }
                                ?.also { Timber.d("Overriding homeserver url to $it") }
                                ?.let { Uri.parse(it) }
                                ?: homeServerConnectionConfig.homeServerUri,
                        identityServerUri = credentials.wellKnown?.identityServer?.baseURL
                                // remove trailing "/"
                                ?.trim { it == '/' }
                                ?.takeIf { it.isNotBlank() }
                                ?.also { Timber.d("Overriding identity server url to $it") }
                                ?.let { Uri.parse(it) }
                                ?: homeServerConnectionConfig.identityServerUri
                ))

        sessionParamsStore.save(sessionParams)
        return sessionManager.getOrCreateSession(sessionParams)
    }
}
