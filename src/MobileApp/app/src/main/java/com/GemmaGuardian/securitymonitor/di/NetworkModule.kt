package com.GemmaGuardian.securitymonitor.di

import android.content.Context
import android.content.SharedPreferences
import com.GemmaGuardian.securitymonitor.config.NetworkConfig
import com.GemmaGuardian.securitymonitor.config.ConfigurationManager
import com.GemmaGuardian.securitymonitor.data.alarm.AlarmManager
import com.GemmaGuardian.securitymonitor.data.network.SecurityApiService
import com.GemmaGuardian.securitymonitor.data.network.SecurityNetworkClient
import com.GemmaGuardian.securitymonitor.data.notification.NotificationHandler
import com.GemmaGuardian.securitymonitor.data.notification.NotificationPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("security_monitor_prefs", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideConfigurationManager(@ApplicationContext context: Context): ConfigurationManager {
        return ConfigurationManager(context)
    }
    
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(configurationManager: ConfigurationManager): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                
                // Get current server configuration
                val currentBaseUrl = configurationManager.getBaseUrl()
                val newUrl = currentBaseUrl.toHttpUrl()
                
                android.util.Log.d("NetworkModule", "🌐 Original URL: ${original.url}")
                android.util.Log.d("NetworkModule", "🔧 ConfigurationManager base URL: $currentBaseUrl")
                android.util.Log.d("NetworkModule", "📍 Server Host: ${configurationManager.getServerHost()}")
                android.util.Log.d("NetworkModule", "🔌 Server Port: ${configurationManager.getServerPort()}")
                
                // Replace the base URL with current configuration
                val newRequest = original.newBuilder()
                    .url(original.url.newBuilder()
                        .scheme(newUrl.scheme)
                        .host(newUrl.host)
                        .port(newUrl.port)
                        .build())
                    .build()
                
                android.util.Log.d("NetworkModule", "🎯 New URL: ${newRequest.url}")
                
                chain.proceed(newRequest)
            }
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        // Use a placeholder base URL - actual URLs will be dynamically resolved by OkHttp interceptor
        return Retrofit.Builder()
            .baseUrl("http://localhost:8888/") // Placeholder URL, will be replaced by interceptor
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSecurityApiService(retrofit: Retrofit): SecurityApiService {
        return retrofit.create(SecurityApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideNotificationPreferences(
        @ApplicationContext context: Context,
        preferences: SharedPreferences
    ): NotificationPreferences {
        return NotificationPreferences(context, preferences)
    }
    
    @Provides
    @Singleton
    fun provideAlarmManager(
        @ApplicationContext context: Context
    ): AlarmManager {
        return AlarmManager(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationHandler(
        @ApplicationContext context: Context,
        notificationPreferences: NotificationPreferences,
        alarmManager: AlarmManager
    ): NotificationHandler {
        return NotificationHandler(context, notificationPreferences, alarmManager)
    }
}
