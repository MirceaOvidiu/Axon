package com.axon.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun AxonTheme(content: @Composable () -> Unit) {
    /**
     * Axon Wear OS theme with Montserrat font.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        typography = MontserratTypography,
        content = content,
    )
}
