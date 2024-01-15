package com.example.seeds
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.seeds.database.StudentDatabase
import com.example.seeds.network.provideService
import com.example.seeds.utils.EmailIdString
import com.example.seeds.utils.TokenAuthenticator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltAppModule {

    @Singleton
    @Provides
    fun provideStudentDatabase(app: Application) =
        Room.databaseBuilder(app, StudentDatabase::class.java, "student_database")
            .fallbackToDestructiveMigration()
            .build()

    //    @Singleton
//    @Provides
//    fun provideNetworkService(authenticator: TokenAuthenticator) = provideService(authenticator)
    @Singleton
    @Provides
    fun provideNetworkService(
        authenticator: TokenAuthenticator,
        @ApplicationContext context: Context
    ) = provideService(authenticator, context)

    @Singleton
    @Provides
    @EmailIdString
    fun provideEmailIdString(): String = Firebase.auth.currentUser!!.phoneNumber.toString()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
    }

    @Singleton
    @Provides
    fun provideStudentDao(studentDatabase: StudentDatabase) = studentDatabase.studentDao

    @Singleton
    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @Singleton
    @Provides
    fun provideLogDao(database: StudentDatabase) = database.logDao

}