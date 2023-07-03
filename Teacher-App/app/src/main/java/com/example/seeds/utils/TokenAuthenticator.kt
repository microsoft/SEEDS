package com.example.seeds.utils

import android.app.Application
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

//reference:https://square.github.io/okhttp/recipes/#handling-authentication-kt-java
val Response.responseCount: Int
    get() = generateSequence(this) { it.priorResponse() }.count()

//ref: https://stackoverflow.com/questions/22450036/refreshing-oauth-token-using-retrofit-without-modifying-all-calls
class TokenAuthenticator @Inject constructor(val application: Application): Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if(response.responseCount > 2) return null
        FirebaseToken.resetIdToken()
        val token = FirebaseToken.getIdToken()
        return response.request().newBuilder()
            .header("authtoken", token)
            .build()
    }
}