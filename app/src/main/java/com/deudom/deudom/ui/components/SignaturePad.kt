package com.deudom.deudom.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.ui.theme.GoldPremium

@Composable
fun SignaturePad(isVerified: Boolean = false, onCapture: (Bitmap?) -> Unit) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, GoldPremium.copy(0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(isVerified) {
                if (!isVerified) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val path = Path().apply { moveTo(offset.x, offset.y) }
                            paths.add(path)
                            currentPath = path
                        },
                        onDrag = { change, _ ->
                            currentPath?.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = { currentPath = null }
                    )
                }
            }
        ) {
            if (isVerified) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Fingerprint, 
                        null, 
                        tint = GoldPremium, 
                        modifier = Modifier.size(120.dp)
                    )
                    Text(
                        "Identidad Verificada", 
                        color = GoldPremium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Huella digital registrada", 
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else if (paths.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Fingerprint, 
                        null, 
                        tint = GoldPremium.copy(0.3f), 
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        "Coloca tu dedo o firma aquí", 
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (!isVerified) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { path ->
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
                TextButton(
                    onClick = { paths.clear() }, 
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                ) {
                    Text("Limpiar", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = if (isVerified) Color(0xFF4CAF50) else GoldPremium, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (isVerified) "Huella digital verificada correctamente" else "Presiona firmemente para registrar huella digital",
                style = MaterialTheme.typography.bodySmall,
                color = if (isVerified) Color(0xFF4CAF50) else GoldPremium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
