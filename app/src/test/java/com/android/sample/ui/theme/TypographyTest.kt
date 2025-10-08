package com.android.sample.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Typography
import org.junit.Assert.*
import org.junit.Test

/** Unit tests to verify that the Bodoni font family is correctly applied to Typography styles. */
class TypographyTest {

  @Test
  fun bodoni_fontFamily_isNotNull() {
    // Verify that the Bodoni font family is properly initialized
    assertNotNull("Bodoni font family should not be null", Bodoni)
  }

  @Test
  fun typography_displayLarge_usesBodoniFont() {
    val displayLarge = Typography.displayLarge

    assertEquals("displayLarge should use Bodoni font family", Bodoni, displayLarge.fontFamily)
    assertEquals(
        "displayLarge should use Normal weight", FontWeight.Normal, displayLarge.fontWeight)
    assertEquals("displayLarge should have 36sp font size", 36.sp, displayLarge.fontSize)
  }

  @Test
  fun typography_titleLarge_usesBodoniFont() {
    val titleLarge = Typography.titleLarge

    assertEquals("titleLarge should use Bodoni font family", Bodoni, titleLarge.fontFamily)
    assertEquals("titleLarge should use Normal weight", FontWeight.Normal, titleLarge.fontWeight)
    assertEquals("titleLarge should have 20sp font size", 20.sp, titleLarge.fontSize)
  }

  @Test
  fun typography_bodySmall_usesBodoniFont() {
    val bodySmall = Typography.bodySmall

    assertEquals("bodySmall should use Bodoni font family", Bodoni, bodySmall.fontFamily)
    assertEquals("bodySmall should use Normal weight", FontWeight.Normal, bodySmall.fontWeight)
    assertEquals("bodySmall should have 13sp font size", 13.sp, bodySmall.fontSize)
  }

  @Test
  fun typography_allBodoniStyles_useSameFontFamily() {
    val displayLarge = Typography.displayLarge
    val titleLarge = Typography.titleLarge
    val bodySmall = Typography.bodySmall

    assertEquals(
        "All typography styles should use the same Bodoni font family",
        displayLarge.fontFamily,
        titleLarge.fontFamily)
    assertEquals(
        "All typography styles should use the same Bodoni font family",
        titleLarge.fontFamily,
        bodySmall.fontFamily)
  }

  @Test
  fun typography_fontSizes_areCorrect() {
    assertEquals("displayLarge font size should be 36sp", 36.sp, Typography.displayLarge.fontSize)
    assertEquals("titleLarge font size should be 20sp", 20.sp, Typography.titleLarge.fontSize)
    assertEquals("bodySmall font size should be 13sp", 13.sp, Typography.bodySmall.fontSize)
  }

  @Test
  fun typography_fontWeights_areNormal() {
    assertEquals(
        "displayLarge should use Normal weight",
        FontWeight.Normal,
        Typography.displayLarge.fontWeight)
    assertEquals(
        "titleLarge should use Normal weight", FontWeight.Normal, Typography.titleLarge.fontWeight)
    assertEquals(
        "bodySmall should use Normal weight", FontWeight.Normal, Typography.bodySmall.fontWeight)
  }

  @Test
  fun typography_usesConsistentFontFamily() {
    // Verify that all text styles use Bodoni font
    val styles = listOf(Typography.displayLarge, Typography.titleLarge, Typography.bodySmall)

    styles.forEach { style ->
      assertNotNull("Text style should have a font family", style.fontFamily)
      assertEquals("Text style should use Bodoni font family", Bodoni, style.fontFamily)
    }
  }

  @Test
  fun bodoni_fontFamily_isUsedInTypography() {
    // Verify that the Bodoni font family instance is actually used in Typography
    assertTrue(
        "Bodoni should be used in displayLarge", Typography.displayLarge.fontFamily == Bodoni)
    assertTrue("Bodoni should be used in titleLarge", Typography.titleLarge.fontFamily == Bodoni)
    assertTrue("Bodoni should be used in bodySmall", Typography.bodySmall.fontFamily == Bodoni)
  }
}
