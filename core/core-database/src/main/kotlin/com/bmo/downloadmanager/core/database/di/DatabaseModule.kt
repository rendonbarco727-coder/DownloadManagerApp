package com.bmo.downloadmanager.core.database.di

import android.content.Context
import androidx.room.Room
import com.bmo.downloadmanager.core.database.AppDatabase
import com.bmo.downloadmanager.core.database.dao.BrowserHistoryDao
import com.bmo.downloadmanager.core.database.dao.BrowserTabDao
import com.bmo.downloadmanager.core.database.dao.DownloadDao
import com.bmo.downloadmanager.core.database.dao.DownloadSegmentDao
import com.bmo.downloadmanager.core.database.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideDownloadSegmentDao(database: AppDatabase): DownloadSegmentDao =
        database.downloadSegmentDao()

    @Provides
    fun provideBrowserHistoryDao(database: AppDatabase): BrowserHistoryDao =
        database.browserHistoryDao()

    @Provides
    fun provideBrowserTabDao(database: AppDatabase): BrowserTabDao = database.browserTabDao()

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()
}
