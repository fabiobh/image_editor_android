package com.uaialternativa.imageeditor.data.database

import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Room type converters
 * Note: Tests involving Rect (Android class) are moved to instrumented tests
 */
class ConvertersTest {
    
    private lateinit var converters: Converters
    
    @Before
    fun setup() {
        converters = Converters()
    }
    
    @Test
    fun `convert empty list to JSON and back`() {
        val emptyList = emptyList<ImageOperation>()
        
        val json = converters.fromImageOperationList(emptyList)
        val result = converters.toImageOperationList(json)
        
        assertEquals(emptyList, result)
    }
    
    @Test
    fun `convert resize operation to JSON and back`() {
        val resizeOperation = ImageOperation.Resize(800, 600)
        val operations = listOf(resizeOperation)
        
        val json = converters.fromImageOperationList(operations)
        val result = converters.toImageOperationList(json)
        
        assertEquals(1, result.size)
        assertTrue(result[0] is ImageOperation.Resize)
        val resultResize = result[0] as ImageOperation.Resize
        assertEquals(800, resultResize.width)
        assertEquals(600, resultResize.height)
    }
    
    @Test
    fun `convert filter operation to JSON and back`() {
        val filterOperation = ImageOperation.Filter(FilterType.BRIGHTNESS, 0.8f)
        val operations = listOf(filterOperation)
        
        val json = converters.fromImageOperationList(operations)
        val result = converters.toImageOperationList(json)
        
        assertEquals(1, result.size)
        assertTrue(result[0] is ImageOperation.Filter)
        val resultFilter = result[0] as ImageOperation.Filter
        assertEquals(FilterType.BRIGHTNESS, resultFilter.type)
        assertEquals(0.8f, resultFilter.intensity, 0.001f)
    }
    
    @Test
    fun `convert multiple resize and filter operations to JSON and back`() {
        val operations = listOf(
            ImageOperation.Resize(400, 300),
            ImageOperation.Filter(FilterType.SEPIA, 0.5f),
            ImageOperation.Filter(FilterType.CONTRAST, 1.2f)
        )
        
        val json = converters.fromImageOperationList(operations)
        val result = converters.toImageOperationList(json)
        
        assertEquals(3, result.size)
        
        // Verify resize operation
        assertTrue(result[0] is ImageOperation.Resize)
        val resize = result[0] as ImageOperation.Resize
        assertEquals(400, resize.width)
        assertEquals(300, resize.height)
        
        // Verify first filter operation
        assertTrue(result[1] is ImageOperation.Filter)
        val filter1 = result[1] as ImageOperation.Filter
        assertEquals(FilterType.SEPIA, filter1.type)
        assertEquals(0.5f, filter1.intensity, 0.001f)
        
        // Verify second filter operation
        assertTrue(result[2] is ImageOperation.Filter)
        val filter2 = result[2] as ImageOperation.Filter
        assertEquals(FilterType.CONTRAST, filter2.type)
        assertEquals(1.2f, filter2.intensity, 0.001f)
    }
    
    @Test
    fun `handle invalid JSON gracefully`() {
        val invalidJson = "invalid json string"
        
        val result = converters.toImageOperationList(invalidJson)
        
        assertEquals(emptyList<ImageOperation>(), result)
    }
    
    @Test
    fun `handle empty JSON string`() {
        val emptyJson = ""
        
        val result = converters.toImageOperationList(emptyJson)
        
        assertEquals(emptyList<ImageOperation>(), result)
    }
    
    @Test
    fun `convert all filter types correctly`() {
        val operations = FilterType.values().map { filterType ->
            ImageOperation.Filter(filterType, 0.7f)
        }
        
        val json = converters.fromImageOperationList(operations)
        val result = converters.toImageOperationList(json)
        
        assertEquals(FilterType.values().size, result.size)
        
        result.forEachIndexed { index, operation ->
            assertTrue(operation is ImageOperation.Filter)
            val filter = operation as ImageOperation.Filter
            assertEquals(FilterType.values()[index], filter.type)
            assertEquals(0.7f, filter.intensity, 0.001f)
        }
    }
}