package com.deudom.deudom.ui.components

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius

@Composable
fun QRScanner(onCodeScanned: (String) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val lo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cpf = remember { ProcessCameraProvider.getInstance(ctx) }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { c ->
                val pv = PreviewView(c)
                val ex = ContextCompat.getMainExecutor(c)
                cpf.addListener({
                    val cp = cpf.get()
                    val p = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                    val ia = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    val r = MultiFormatReader()
                    ia.setAnalyzer(ex) { ip ->
                        val b = ip.planes[0].buffer
                        val d = ByteArray(b.remaining())
                        b.get(d)
                        try {
                            val res = r.decode(BinaryBitmap(HybridBinarizer(PlanarYUVLuminanceSource(d, ip.width, ip.height, 0, 0, ip.width, ip.height, false))))
                            onCodeScanned(res.text)
                        } catch (e: Exception) {
                        } finally { ip.close() }
                    }
                    try {
                        cp.unbindAll()
                        cp.bindToLifecycle(lo, CameraSelector.DEFAULT_BACK_CAMERA, p, ia)
                    } catch (e: Exception) {
                        Log.e("QRScanner", "Binding failed", e)
                    }
                }, ex)
                pv
            },
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val scanRectSize = canvasWidth * 0.7f
            val left = (canvasWidth - scanRectSize) / 2
            val top = (canvasHeight - scanRectSize) / 2

            // Background with clear rectangle
            with(drawContext.canvas.nativeCanvas) {
                val checkpoint = saveLayer(null, null)
                drawRect(Color.Black.copy(0.7f))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(scanRectSize, scanRectSize),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                restoreToCount(checkpoint)
            }

            // White border
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(scanRectSize, scanRectSize),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}
