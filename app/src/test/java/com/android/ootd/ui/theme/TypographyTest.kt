package com.android.ootd.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert
import org.junit.Test

/** Unit tests to verify that the Bodoni font family is correctly applied to Typography styles. */
class TypographyTest {

  @Test
  fun bodoni_fontFamily_isNotNull() {
    // Verify that the Bodoni font family is properly initialized
    Assert.assertNotNull("Bodoni font family should not be null", NotoSans)
  }

  @Test
  fun typography_displayLarge_usesBodoniFont() {
    val displayLarge = Typography.displayLarge

    Assert.assertEquals(
        "displayLarge should use Bodoni font family", NotoSans, displayLarge.fontFamily)
    // displayLarge was made bold in the app theme
    Assert.assertEquals(
        "displayLarge should use Bold weight", FontWeight.Companion.Bold, displayLarge.fontWeight)
    Assert.assertEquals("displayLarge should have 36sp font size", 36.sp, displayLarge.fontSize)
  }

  @Test
  fun typography_titleLarge_usesBodoniFont() {
    val titleLarge = Typography.titleLarge

    Assert.assertEquals("titleLarge should use Bodoni font family", NotoSans, titleLarge.fontFamily)
    Assert.assertEquals(
        "titleLarge should use Normal weight", FontWeight.Companion.Normal, titleLarge.fontWeight)
    Assert.assertEquals("titleLarge should have 20sp font size", 20.sp, titleLarge.fontSize)
  }

  @Test
  fun typography_bodyLarge_hasExpectedSizeAndLineHeight() {
    val bodyLarge = Typography.bodyLarge

    Assert.assertEquals("bodyLarge should use Bodoni font family", NotoSans, bodyLarge.fontFamily)
    Assert.assertEquals(
        "bodyLarge should use Normal weight", FontWeight.Companion.Normal, bodyLarge.fontWeight)
    Assert.assertEquals("bodyLarge should have 16sp font size", 16.sp, bodyLarge.fontSize)
    Assert.assertEquals("bodyLarge should have 24sp line height", 24.sp, bodyLarge.lineHeight)
  }

  @Test
  fun typography_bodySmall_usesBodoniFont() {
    val bodySmall = Typography.bodySmall

    Assert.assertEquals("bodySmall should use Bodoni font family", NotoSans, bodySmall.fontFamily)
    Assert.assertEquals(
        "bodySmall should use Normal weight", FontWeight.Companion.Normal, bodySmall.fontWeight)
    Assert.assertEquals("bodySmall should have 13sp font size", 13.sp, bodySmall.fontSize)
  }

  @Test
  fun typography_headlineMedium_usesBodoniFont() {
    val headlineMedium = Typography.headlineMedium

    Assert.assertEquals(
        "headlineMedium should use Bodoni font family", NotoSans, headlineMedium.fontFamily)
    Assert.assertEquals(
        "headlineMedium should use Bold weight",
        FontWeight.Companion.Bold,
        headlineMedium.fontWeight)
    Assert.assertEquals("headlineMedium should have 48sp font size", 48.sp, headlineMedium.fontSize)
  }

  @Test
  fun typography_allBodoniStyles_useSameFontFamily() {
    val displayLarge = Typography.displayLarge
    val titleLarge = Typography.titleLarge
    val bodyLarge = Typography.bodyLarge
    val bodySmall = Typography.bodySmall

    Assert.assertEquals(
        "All typography styles should use the same Bodoni font family",
        displayLarge.fontFamily,
        titleLarge.fontFamily)
    Assert.assertEquals(
        "All typography styles should use the same Bodoni font family",
        titleLarge.fontFamily,
        bodyLarge.fontFamily)
    Assert.assertEquals(
        "All typography styles should use the same Bodoni font family",
        bodyLarge.fontFamily,
        bodySmall.fontFamily)
  }

  @Test
  fun typography_fontSizes_areCorrect() {
    Assert.assertEquals(
        "displayLarge font size should be 36sp", 36.sp, Typography.displayLarge.fontSize)
    Assert.assertEquals(
        "titleLarge font size should be 20sp", 20.sp, Typography.titleLarge.fontSize)
    Assert.assertEquals("bodyLarge font size should be 16sp", 16.sp, Typography.bodyLarge.fontSize)
    Assert.assertEquals("bodySmall font size should be 13sp", 13.sp, Typography.bodySmall.fontSize)
    Assert.assertEquals(
        "headlineMedium font size should be 48sp", 48.sp, Typography.headlineMedium.fontSize)
  }

  @Test
  fun typography_fontWeights_areAsConfigured() {
    Assert.assertEquals(
        "displayLarge should use Bold weight",
        FontWeight.Companion.Bold,
        Typography.displayLarge.fontWeight)
    Assert.assertEquals(
        "titleLarge should use Normal weight",
        FontWeight.Companion.Normal,
        Typography.titleLarge.fontWeight)
    Assert.assertEquals(
        "bodyLarge should use Normal weight",
        FontWeight.Companion.Normal,
        Typography.bodyLarge.fontWeight)
    Assert.assertEquals(
        "bodySmall should use Normal weight",
        FontWeight.Companion.Normal,
        Typography.bodySmall.fontWeight)
    Assert.assertEquals(
        "headlineMedium should use Bold weight",
        FontWeight.Companion.Bold,
        Typography.headlineMedium.fontWeight)
  }

  @Test
  fun typography_usesConsistentFontFamily() {
    // Verify that all text styles use Bodoni font
    val styles =
        listOf(
            Typography.displayLarge,
            Typography.titleLarge,
            Typography.bodyLarge,
            Typography.bodySmall,
            Typography.headlineMedium)

    styles.forEach { style ->
      Assert.assertNotNull("Text style should have a font family", style.fontFamily)
      Assert.assertEquals("Text style should use Bodoni font family", NotoSans, style.fontFamily)
    }
  }

  @Test
  fun bodoni_fontFamily_isUsedInTypography() {
    // Verify that the Bodoni font family instance is actually used in Typography
    Assert.assertTrue(
        "Bodoni should be used in displayLarge", Typography.displayLarge.fontFamily == NotoSans)
    Assert.assertTrue(
        "Bodoni should be used in titleLarge", Typography.titleLarge.fontFamily == NotoSans)
    Assert.assertTrue(
        "Bodoni should be used in bodyLarge", Typography.bodyLarge.fontFamily == NotoSans)
    Assert.assertTrue(
        "Bodoni should be used in bodySmall", Typography.bodySmall.fontFamily == NotoSans)
    Assert.assertTrue(
        "Bodoni should be used in headlineMedium", Typography.headlineMedium.fontFamily == NotoSans)
  }
}
