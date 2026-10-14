package com.deudom.deudom.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizedText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
    color: Color = style.color,
    fontWeight: FontWeight? = FontWeight.Black
) {
    var resizedTextStyle by remember { mutableStateOf(style) }
    var shouldDraw by remember { mutableStateOf(false) }

    val defaultFontSize = style.fontSize

    Text(
        text = text,
        color = color,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        softWrap = false,
        style = resizedTextStyle.copy(fontWeight = fontWeight),
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                if (style.fontSize.isUnspecified) {
                    resizedTextStyle = resizedTextStyle.copy(
                        fontSize = defaultFontSize
                    )
                }
                // Add a minimum font size to prevent infinite loops/crashes
                if (resizedTextStyle.fontSize > 8.sp) {
                    resizedTextStyle = resizedTextStyle.copy(
                        fontSize = resizedTextStyle.fontSize * 0.95
                    )
                } else {
                    shouldDraw = true // Stop resizing even if it overflows
                }
            } else {
                shouldDraw = true
            }
        }
    )
}