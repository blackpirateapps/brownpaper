package com.blackpirateapps.brownpaper.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import com.blackpirateapps.brownpaper.data.local.BrownPaperDao
import com.blackpirateapps.brownpaper.data.local.BrownPaperDatabase
import com.blackpirateapps.brownpaper.data.preferences.ReaderPreferencesRepositoryImpl
import com.blackpirateapps.brownpaper.data.repository.ArticleRepositoryImpl
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import com.blackpirateapps.brownpaper.domain.repository.ReaderPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BrownPaperDatabase = Room
        .databaseBuilder(context, BrownPaperDatabase::class.java, "brownpaper.db")
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideBrownPaperDao(database: BrownPaperDatabase): BrownPaperDao = database.brownPaperDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("reader_preferences.preferences_pb") },
        )

    @Provides
    @Singleton
    fun provideDispatchers(): AppDispatchers = AppDispatchers()
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
}
