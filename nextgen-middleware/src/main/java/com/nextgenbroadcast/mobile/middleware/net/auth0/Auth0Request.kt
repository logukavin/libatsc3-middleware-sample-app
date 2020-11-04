package com.nextgenbroadcast.mobile.middleware.net.auth0

import okhttp3.FormBody
import okhttp3.Request

internal class Auth0Request {
    class Builder {
        private val body = FormBody.Builder()

        fun username(username: String): Builder {
            body.add("username", username)
            return this
        }

        fun password(password: String): Builder {
            body.add("password", password)
            return this
        }

        fun audience(audience: String): Builder {
            body.add("audience", audience)
            return this
        }

        fun clientId(clientId: String): Builder {
            body.add("client_id", clientId)
            return this
        }

        fun clientSecret(clientSecret: String): Builder {
            body.add("client_secret", clientSecret)
            return this
        }

        fun build(url: String): Request {
            body.add("grant_type", "http://auth0.com/oauth/grant-type/password-realm")
                    .add("scope", "openid profile email")
                    .add("realm", "Username-Password-Authentication")

            return Request.Builder()
                    .url(url)
                    .addHeader("content-type", "application/x-www-form-urlencoded")
                    .post(body.build())
                    .build()
        }
    }
}