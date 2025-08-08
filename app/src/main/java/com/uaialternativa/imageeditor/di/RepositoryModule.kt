package com.uaialternativa.imageeditor.di

import com.uaialternativa.imageeditor.data.processing.ImageProcessingRepositoryImpl
import com.uaialternativa.imageeditor.data.repository.ImageRepositoryImpl
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Binds ImageRepositoryImpl to ImageRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindImageRepository(
        imageRepositoryImpl: ImageRepositoryImpl
    ): ImageRepository
    
    /**
     * Binds ImageProcessingRepositoryImpl to ImageProcessingRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindImageProcessingRepository(
        imageProcessingRepositoryImpl: ImageProcessingRepositoryImpl
    ): ImageProcessingRepository
}