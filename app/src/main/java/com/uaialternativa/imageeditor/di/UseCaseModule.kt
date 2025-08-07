package com.uaialternativa.imageeditor.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for providing use case dependencies
 * 
 * Note: All use cases are already using @Inject constructor, so they are automatically
 * provided by Hilt. This module exists for potential future use case configurations
 * or if we need to provide specific scoping or qualifiers.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    // Use cases are automatically provided by Hilt through @Inject constructor
    // No explicit bindings needed unless we require specific configurations
    
    // Future use case providers can be added here if needed:
    // - Custom scoping
    // - Qualifiers for different implementations
    // - Factory methods for complex use case initialization
}