package com.axon.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.presentation.theme.ThinFont

@Composable
fun MinimalisticAxonLogo(
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    size: LogoSize = LogoSize.LARGE
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AXON",
            fontFamily = ThinFont,
            fontWeight = FontWeight.Thin, // Use Thin weight for minimalistic look
            fontSize = size.fontSize,
            color = textColor,
            textAlign = TextAlign.Center,
            letterSpacing = size.letterSpacing
        )

        if (size == LogoSize.LARGE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recovery Analytics",
                fontFamily = ThinFont,
                fontWeight = FontWeight.ExtraLight, // Even lighter for subtitle
                fontSize = size.subTextSize,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}

enum class LogoSize(
    val fontSize: TextUnit,
    val subTextSize: TextUnit,
    val letterSpacing: TextUnit
) {
    SMALL(32.sp, 12.sp, 4.sp),
    MEDIUM(48.sp, 14.sp, 6.sp),
    LARGE(64.sp, 16.sp, 8.sp),
    EXTRA_LARGE(80.sp, 18.sp, 10.sp)
}
