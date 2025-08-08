package com.uaialternativa.imageeditor.di

import com.uaialternativa.imageeditor.data.processing.ImageProcessingRepositoryImpl
import com.uaialternativa.imageeditor.data.repository.ImageRepositoryImpl
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import org.junit.Test
import org.junit.Assert.assertTrue

/**
 * Test class to verify Hilt module configurations
 */
class HiltModulesTest {
    
    @Test
    fun `RepositoryModule binds correct implementations`() {
        // Verify that the implementations extend/implement the correct interfaces
        assertTrue(ImageRepositoryImpl::class.java.interfaces.contains(ImageRepository::class.java))
        assertTrue(ImageProcessingRepositoryImpl::class.java.interfaces.contains(ImageProcessingRepository::class.java))
    }
    
    @Test
    fun `All repository implementations have Singleton annotation`() {
        // Verify that repository implementations are properly annotated
        val imageRepoAnnotations = ImageRepositoryImpl::class.java.annotations
        val processingRepoAnnotations = ImageProcessingRepositoryImpl::class.java.annotations
        
        assertTrue(imageRepoAnnotations.any { it.annotationClass.simpleName == "Singleton" })
        assertTrue(processingRepoAnnotations.any { it.annotationClass.simpleName == "Singleton" })
    }
    
    @Test
    fun `All repository implementations have Inject constructor`() {
        // Verify that repository implementations have @Inject constructors
        val imageRepoConstructors = ImageRepositoryImpl::class.java.constructors
        val processingRepoConstructors = ImageProcessingRepositoryImpl::class.java.constructors
        
        assertTrue(imageRepoConstructors.isNotEmpty())
        assertTrue(processingRepoConstructors.isNotEmpty())
        
        // Check that constructors have @Inject annotation
        val imageRepoHasInject = imageRepoConstructors.any { constructor ->
            constructor.annotations.any { it.annotationClass.simpleName == "Inject" }
        }
        val processingRepoHasInject = processingRepoConstructors.any { constructor ->
            constructor.annotations.any { it.annotationClass.simpleName == "Inject" }
        }
        
        assertTrue(imageRepoHasInject)
        assertTrue(processingRepoHasInject)
    }
}