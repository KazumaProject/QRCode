package com.kazumaproject7.qrcodescanner.di

import android.content.Context
import androidx.room.Room
import com.kazumaproject7.qrcodescanner.data.local.ScannedResultDatabase
import com.kazumaproject7.qrcodescanner.other.Constants.INSERT_SCANNED_RESULT_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Singleton
    @Provides
    fun provideScannedResultDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(context, ScannedResultDatabase::class.java,INSERT_SCANNED_RESULT_DATABASE_NAME).build()

    @Singleton
    @Provides
    fun provideScannedResultDao(db: ScannedResultDatabase) = db.insertScannedResultDao()

}