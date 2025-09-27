package com.nxweb.jellyfin_wearos.activity

import com.nxweb.jellyfin_wearos.R
import android.content.Context
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult


class Jellyfin(androidcontext: Context) {
    val context: Context = androidcontext
    lateinit var api: ApiClient
    lateinit var userid: UUID
    val sharedPref = androidcontext.getSharedPreferences("jellyfin", Context.MODE_PRIVATE)


    init {
        if (checkCredentials()) {
            val hostname = sharedPref.getString("hostname", null)!!
            val username = sharedPref.getString("username", null)!!
            val password = sharedPref.getString("password", null)!!
            createApi(context, hostname, username, password)
        }
    }

    fun createApi(androidcontext: Context, hostname: String, username: String, password: String) {
        val jellyfin = createJellyfin {
            clientInfo = ClientInfo(name = "Wear OS", version = R.string.Version.toString())
            context = androidcontext
        }
        api = jellyfin.createApi(baseUrl = hostname)

        if (sharedPref.getString("api_key", null) != null && sharedPref.getString(
                "user_id",
                null
            ) != null
        ) {
            userid = UUID.fromString(sharedPref.getString("user_id", null)!!)
            api.update(accessToken = sharedPref.getString("api_key", null)!!)
        } else {
            login(username, password)
        }
    }

    fun login(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val authenticationResult by api.userApi.authenticateUserByName(
                username = username,
                password = password,
            )
            api.update(accessToken = authenticationResult.accessToken)
            userid = authenticationResult.user?.id!!
            sharedPref.edit { putString("api_key", authenticationResult.accessToken) }
            sharedPref.edit { putString("user_id", authenticationResult.user?.id.toString()) }
        }
    }

    fun checkCredentials(): Boolean {
        val hostname = sharedPref.getString("hostname", null)
        val username = sharedPref.getString("username", null)
        val password = sharedPref.getString("password", null)
        return !(hostname.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty())
    }

    fun saveCredentials(hostname: String, username: String, password: String) {
        sharedPref.edit {
            putString("api_key", null)
            putString("hostname", hostname)
            putString("username", username)
            putString("password", password)
        }
        createApi(context, hostname, username, password)
    }

    suspend fun getLibraries(): List<BaseItemDto> {
        val library = api.userViewsApi.getUserViews(userId = userid).content.items
        return library.filter { it.collectionType?.serialName == "music" }
    }

    suspend fun getItems(libraryId: UUID): Response<BaseItemDtoQueryResult> {
        return api.itemsApi.getItems(userId = userid, parentId = libraryId)
    }

    fun getAudioUrl(itemId: UUID): String {
        return "${api.baseUrl}/Audio/$itemId/stream?api_key=${api.accessToken}&container=mp3&static=true"
    }
}

