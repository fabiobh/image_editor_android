package com.uaialternativa.imageeditor.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException

class LoadImageUseCaseTest {
    
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var loadImageUseCase: LoadImageUseCase
    private lateinit var mockUri: Uri
    private lateinit var mockBitmap: Bitmap
    
    @Before
    fun setup() {
        context = mockk()
        contentResolver = mockk()
        loadImageUseCase = LoadImageUseCase(context)
        mockUri = mockk()
        mockBitmap = mockk()
        
        every { context.contentResolver } returns contentResolver
        
        // Mock BitmapFactory static methods
        mockkStatic(BitmapFactory::class)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(BitmapFactory::class)
    }
    
    @Test
    fun `invoke should load image successfully from URI`() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
        every { contentResolver.openInputStream(mockUri) } returns inputStream
        every { BitmapFactory.decodeStream(inputStream, null, any()) } returns mockBitmap
        
        // When
        val result = loadImageUseCase(mockUri)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockBitmap, result.getOrNull())
    }
    
    @Test
    fun `invoke should return failure when input stream cannot be opened`() = runTest {
        // Given
        every { contentResolver.openInputStream(mockUri) } returns null
        
        // When
        val result = loadImageUseCase(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
    }
    
    @Test
    fun `invoke should return failure when bitmap decoding fails`() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
        every { contentResolver.openInputStream(mockUri) } returns inputStream
        every { BitmapFactory.decodeStream(inputStream, null, any()) } returns null
        
        // When
        val result = loadImageUseCase(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
    
    @Test
    fun `invoke should handle SecurityException`() = runTest {
        // Given
        every { contentResolver.openInputStream(mockUri) } throws SecurityException("Permission denied")
        
        // When
        val result = loadImageUseCase(mockUri)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
    
    @Test
    fun `invoke should load image with max dimensions when specified`() = runTest {
        // Given
        val maxWidth = 1280
        val maxHeight = 720
        val inputStream1 = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
        val inputStream2 = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
        
        every { contentResolver.openInputStream(mockUri) } returnsMany listOf(inputStream1, inputStream2)
        
        // Mock the options for dimension checking
        val options = BitmapFactory.Options().apply {
            outWidth = 1920
            outHeight = 1080
        }
        every { BitmapFactory.decodeStream(inputStream1, null, match { it.inJustDecodeBounds }) } answers {
            val opts = thirdArg<BitmapFactory.Options>()
            opts.outWidth = 1920
            opts.outHeight = 1080
            null
        }
        every { BitmapFactory.decodeStream(inputStream2, null, match { !it.inJustDecodeBounds }) } returns mockBitmap
        
        // When
        val result = loadImageUseCase(mockUri, maxWidth, maxHeight)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockBitmap, result.getOrNull())
    }
    
    @Test
    fun `loadFromPath should load image successfully from file path`() = runTest {
        // Given
        val filePath = "/path/to/image.jpg"
        every { BitmapFactory.decodeFile(filePath, any()) } returns mockBitmap
        
        // When
        val result = loadImageUseCase.loadFromPath(filePath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockBitmap, result.getOrNull())
    }
    
    @Test
    fun `loadFromPath should return failure when bitmap decoding fails`() = runTest {
        // Given
        val filePath = "/path/to/invalid_image.jpg"
        every { BitmapFactory.decodeFile(filePath, any()) } returns null
        
        // When
        val result = loadImageUseCase.loadFromPath(filePath)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
    
    @Test
    fun `loadFromPath should load image with max dimensions when specified`() = runTest {
        // Given
        val filePath = "/path/to/image.jpg"
        val maxWidth = 1280
        val maxHeight = 720
        
        // Mock the options for dimension checking
        every { BitmapFactory.decodeFile(filePath, match { it.inJustDecodeBounds }) } answers {
            val opts = secondArg<BitmapFactory.Options>()
            opts.outWidth = 1920
            opts.outHeight = 1080
            null
        }
        every { BitmapFactory.decodeFile(filePath, match { !it.inJustDecodeBounds }) } returns mockBitmap
        
        // When
        val result = loadImageUseCase.loadFromPath(filePath, maxWidth, maxHeight)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockBitmap, result.getOrNull())
    }
    
    @Test
    fun `loadFromPath should handle exceptions`() = runTest {
        // Given
        val filePath = "/path/to/image.jpg"
        every { BitmapFactory.decodeFile(filePath, any()) } throws RuntimeException("File system error")
        
        // When
        val result = loadImageUseCase.loadFromPath(filePath)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Error loading image from path") == true)
    }
}