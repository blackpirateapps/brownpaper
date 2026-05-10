package com.blackpirateapps.brownpaper.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.blackpirateapps.brownpaper.core.di.ReaderPreferencesStore
import com.blackpirateapps.brownpaper.core.di.WallabagSessionPreferencesStore
import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import com.blackpirateapps.brownpaper.data.local.BrownPaperDao
import com.blackpirateapps.brownpaper.data.local.BrownPaperDatabase
import com.blackpirateapps.brownpaper.data.preferences.ReaderPreferencesRepositoryImpl
import com.blackpirateapps.brownpaper.data.repository.ArticleRepositoryImpl
import com.blackpirateapps.brownpaper.data.wallabag.AndroidKeystoreWallabagSecretBox
import com.blackpirateapps.brownpaper.data.wallabag.OkHttpWallabagTransport
import com.blackpirateapps.brownpaper.data.wallabag.WallabagHttpTransport
import com.blackpirateapps.brownpaper.data.wallabag.WallabagRepositoryImpl
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSecretBox
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncScheduler
import com.blackpirateapps.brownpaper.data.wallabag.WorkManagerWallabagSyncScheduler
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import com.blackpirateapps.brownpaper.domain.repository.ReaderPreferencesRepository
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BrownPaperDatabase = Room
        .databaseBuilder(context, BrownPaperDatabase::class.java, "brownpaper.db")
        .addMigrations(BrownPaperDatabase.Migration2To3, BrownPaperDatabase.Migration3To4)
        .build()

    @Provides
    fun provideBrownPaperDao(database: BrownPaperDatabase): BrownPaperDao = database.brownPaperDao()

    @Provides
    @Singleton
    @ReaderPreferencesStore
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("reader_preferences.preferences_pb") },
        )

    @Provides
    @Singleton
    @WallabagSessionPreferencesStore
    fun provideWallabagSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("wallabag_session.preferences_pb") },
        )

    @Provides
    @Singleton
    fun provideDispatchers(): AppDispatchers = AppDispatchers()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    @Binds
    @Singleton
    abstract fun bindReaderPreferencesRepository(
        impl: ReaderPreferencesRepositoryImpl,
    ): ReaderPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindWallabagRepository(impl: WallabagRepositoryImpl): WallabagRepository

    @Binds
    @Singleton
    abstract fun bindWallabagHttpTransport(impl: OkHttpWallabagTransport): WallabagHttpTransport

    @Binds
    @Singleton
    abstract fun bindWallabagSecretBox(impl: AndroidKeystoreWallabagSecretBox): WallabagSecretBox

    @Binds
    @Singleton
    abstract fun bindWallabagSyncScheduler(impl: WorkManagerWallabagSyncScheduler): WallabagSyncScheduler
}
