package com.deudom.deudom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun OutlinedCardCustom(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CardDefaults.shape,
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        border = border,
        color = Color.Transparent
    ) {
        Column(content = content)
    }
}
