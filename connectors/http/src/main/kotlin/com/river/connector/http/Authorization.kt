package com.river.connector.http

import java.util.Base64.getEncoder

fun interface Authorization {
    /**
     * Returns the value of the Authorization header.
     *
     * @return The value of the Authorization header.
     */
    fun headerValue(): String

    companion object {
        /**
         * Returns an Authorization object that represents a Basic authorization.
         *
         * @param username The username.
         * @param password The password.
         *
         * @return The Authorization object.
         */
        fun basic(username: String, password: String) = Authorization {
            val encoded = getEncoder().encodeToString("$username:$password".encodeToByteArray())
            "Basic $encoded"
        }

        /**
         * Returns an Authorization object that represents a Bearer token authorization.
         *
         * @param token The bearer token.
         * @return The Authorization object.
         */
        fun bearer(token: String) = Authorization { "Bearer $token" }
    }
}
