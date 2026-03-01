package com.axon.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Typography
import com.axon.R

// Selima Font Family
val Selima = FontFamily(
    Font(R.font.selima, FontWeight.Normal)
)

// Wear OS Typography with Selima
val SelimaTypography = Typography(
    display1 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp
    ),
    display2 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp
    ),
    display3 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp
    ),
    title1 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    title2 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    title3 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body1 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    caption1 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    caption2 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    caption3 = TextStyle(
        fontFamily = Selima,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)
