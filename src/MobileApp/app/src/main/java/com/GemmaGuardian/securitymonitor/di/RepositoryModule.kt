package com.GemmaGuardian.securitymonitor.di

import android.content.Context
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.data.network.SecurityApiService
import com.GemmaGuardian.securitymonitor.data.network.SecurityNetworkClient
import com.GemmaGuardian.securitymonitor.config.ConfigurationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideSecurityRepository(
        apiService: SecurityApiService,
        networkClient: SecurityNetworkClient,
        configurationManager: ConfigurationManager,
        @ApplicationContext context: Context
    ): SecurityRepository {
        return SecurityRepository(apiService, networkClient, configurationManager, context)
    }
}
