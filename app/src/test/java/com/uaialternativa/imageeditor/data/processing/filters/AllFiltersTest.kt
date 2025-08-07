package com.uaialternativa.imageeditor.data.processing.filters

import com.uaialternativa.imageeditor.data.processing.ImageFilter
import com.uaialternativa.imageeditor.domain.model.FilterType
import org.junit.Test
import org.junit.Assert.*

class AllFiltersTest {
    
    @Test
    fun `all filter classes can be instantiated`() {
        // Given & When
        val brightnessFilter = BrightnessFilter()
        val contrastFilter = ContrastFilter()
        val saturationFilter = SaturationFilter()
        val blurFilter = BlurFilter()
        val sharpenFilter = SharpenFilter()
        val sepiaFilter = SepiaFilter()
        val grayscaleFilter = GrayscaleFilter()
        
        // Then - all filters should be created successfully
        assertNotNull(brightnessFilter)
        assertNotNull(contrastFilter)
        assertNotNull(saturationFilter)
        assertNotNull(blurFilter)
        assertNotNull(sharpenFilter)
        assertNotNull(sepiaFilter)
        assertNotNull(grayscaleFilter)
    }
    
    @Test
    fun `all filter classes implement ImageFilter interface`() {
        // Given
        val filters = listOf(
            BrightnessFilter(),
            ContrastFilter(),
            SaturationFilter(),
            BlurFilter(),
            SharpenFilter(),
            SepiaFilter(),
            GrayscaleFilter()
        )
        
        // When & Then
        for (filter in filters) {
            assertTrue("Filter ${filter::class.simpleName} should implement ImageFilter", 
                filter is ImageFilter)
        }
    }
    
    @Test
    fun `filter type enum has correct number of values`() {
        // Given
        val filterTypes = FilterType.values()
        
        // When & Then
        assertEquals("Should have 7 filter types", 7, filterTypes.size)
        
        // Verify all expected filter types exist
        assertTrue(filterTypes.contains(FilterType.BRIGHTNESS))
        assertTrue(filterTypes.contains(FilterType.CONTRAST))
        assertTrue(filterTypes.contains(FilterType.SATURATION))
        assertTrue(filterTypes.contains(FilterType.BLUR))
        assertTrue(filterTypes.contains(FilterType.SHARPEN))
        assertTrue(filterTypes.contains(FilterType.SEPIA))
        assertTrue(filterTypes.contains(FilterType.GRAYSCALE))
    }
}