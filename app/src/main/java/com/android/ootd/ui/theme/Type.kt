package com.android.ootd.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.ootd.R

val Bodoni =
    FontFamily(
        Font(R.font.bodoni_moda_all, FontWeight.Normal),
        Font(R.font.bodoni_moda_all, FontWeight.Medium),
        Font(R.font.bodoni_moda_all, FontWeight.Bold))
// Set of Material typography styles to start with
val Typography =
    Typography(
        // header 1 - make header bold
        displayLarge =
            TextStyle(fontFamily = Bodoni, fontWeight = FontWeight.Bold, fontSize = 36.sp),
        // header 2
        titleLarge =
            TextStyle(fontFamily = Bodoni, fontWeight = FontWeight.Normal, fontSize = 20.sp),
        // body - input & large body text 16/24
        bodyLarge =
            TextStyle(
                fontFamily = Bodoni,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp),
        // small body
        bodySmall =
            TextStyle(fontFamily = Bodoni, fontWeight = FontWeight.Normal, fontSize = 13.sp),
        headlineLarge =
            TextStyle(fontFamily = Bodoni, fontWeight = FontWeight.Normal, fontSize = 64.sp))
