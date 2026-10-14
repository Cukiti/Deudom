package com.deudom.deudom.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.deudom.deudom.ui.theme.GoldPremium

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    prefix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        leadingIcon = { Icon(icon, null, tint = GoldPremium) },
        prefix = prefix?.let { { Text(it, color = GoldPremium, fontWeight = FontWeight.Bold) } },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPremium,
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
            focusedLabelColor = GoldPremium,
            cursorColor = GoldPremium
        ),
        shape = MaterialTheme.shapes.medium
    )
}
