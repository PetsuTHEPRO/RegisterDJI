package com.sloth.registerapp.di

import android.content.Context
import com.sloth.registerapp.data.network.AuthInterceptor
import com.sloth.registerapp.data.network.SdiaApiService
import com.sloth.registerapp.data.repository.MissionRepository
import com.sloth.registerapp.data.repository.TokenRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://192.168.2.46:5000/api/" // TODO: Replace with actual IP address

    @Provides
    @Singleton
    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
        // Since TokenRepository's constructor is private, we can't use @Inject
        // We will call the public getInstance method
        return TokenRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenRepository: TokenRepository): AuthInterceptor {
        return AuthInterceptor(tokenRepository)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideSdiaApiService(okHttpClient: OkHttpClient): SdiaApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SdiaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMissionRepository(apiService: SdiaApiService, tokenRepository: TokenRepository): MissionRepository {
        return MissionRepository(apiService, tokenRepository)
    }
}
