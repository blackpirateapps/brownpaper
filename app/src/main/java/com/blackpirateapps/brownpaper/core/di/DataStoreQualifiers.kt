package com.blackpirateapps.brownpaper.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReaderPreferencesStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WallabagSessionPreferencesStore
