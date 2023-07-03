package com.example.seeds.network

import com.example.seeds.ApplicationJsonAdapterFactory
import com.example.seeds.database.LogEntity
import com.example.seeds.model.*
import com.example.seeds.utils.Constants
import com.example.seeds.utils.FirebaseToken
import com.example.seeds.utils.TokenAuthenticator
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.*
import java.util.concurrent.TimeUnit

interface SeedsService {
    @GET("call/accessToken")
    suspend fun getAccessToken(): AccessToken

    @POST("call/start")
    suspend fun startCall(@Body callDetails: CallDetails)

    @GET("call/{confId}/status")
    suspend fun getCallStatus(@Path("confId") confId: String): CallStatusDto

    @GET ("teacher/students")
    suspend fun getStudents(): List<String>

    @POST ("teacher/students")
    suspend fun setStudents(@Body students: StudentListContainer): List<String>

    @GET ("teacher/register")
    suspend fun registerTeacher()

    @GET("content")
    suspend fun getAllContent(): List<Content>

    @GET("content")
    suspend fun getContentsById(@Query("ids[]") ids: List<String>): List<Content>

    @GET("class")
    suspend fun getAllClassrooms(): List<ClassroomDto>

    @GET("class/{classId}")
    suspend fun getClassroomById(@Path("classId") classId: String): ClassroomDto

    @POST("class")
    suspend fun saveClassroom(@Body classroom: ClassroomDto): ClassroomDto

    @DELETE("class/{classId}")
    suspend fun deleteClassroom(@Path("classId") classId: String)

    @POST("log")
    suspend fun uploadLogs(@Body logs: List<LogEntity>)

}

fun provideService(authenticator: TokenAuthenticator):  SeedsService {
    //reference: https://proandroiddev.com/headers-in-retrofit-a8d71ede2f3e

    val httpClientBuilder = OkHttpClient.Builder().apply {
        addInterceptor(
            Interceptor { chain ->
                var token: String
                try {
                    token = FirebaseToken.getIdToken()
                } catch (e: NullPointerException) {
                    token = "postman"
                }
                val builder = chain.request().newBuilder()
                builder.header("authToken", token)
                builder.header("signootReqId", UUID.randomUUID().toString())
                return@Interceptor chain.proceed(builder.build())
            }
        )
        authenticator(authenticator)
        readTimeout(60, TimeUnit.SECONDS)
        connectTimeout(60, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
    }

    val moshi = Moshi.Builder()
        //.add(KotlinJsonAdapterFactory())
        .add(ApplicationJsonAdapterFactory)
        //.add(QuestionListConverter())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(httpClientBuilder.build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    return retrofit.create(SeedsService::class.java)
}