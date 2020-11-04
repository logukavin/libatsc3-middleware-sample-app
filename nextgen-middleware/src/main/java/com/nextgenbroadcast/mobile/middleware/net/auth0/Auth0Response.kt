package com.nextgenbroadcast.mobile.middleware.net.auth0

import org.json.JSONObject

internal class Auth0Response(
        private val body: String
) {
    private val bodyJson: JSONObject by lazy {
        JSONObject(body)
    }

    val accessToken: String?
        get() = bodyJson.optString("access_token")

    val idToken: String?
        get() = bodyJson.optString("id_token")

    val scope: String?
        get() = bodyJson.optString("scope")

    val expiresIn: Int?
        get() = bodyJson.optInt("expires_in", -1)
}