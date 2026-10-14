package com.deudom.deudom.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.deudom.deudom.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import android.speech.tts.TextToSpeech
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AIVoiceDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    aiResponse: String? = null,
    isProcessing: Boolean = false,
    isFinalAction: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    var recognizedText by remember { mutableStateOf("Te escucho...") }
    var isListening by remember { mutableStateOf(true) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var lastSpeechTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isJarvisSpeaking by remember { mutableStateOf(false) }

    fun startListening() {
        if (isJarvisSpeaking) return
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer?.destroy()
            isListening = true
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    lastSpeechTime = System.currentTimeMillis()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                override fun onBeginningOfSpeech() {
                    recognizedText = "..."
                    lastSpeechTime = System.currentTimeMillis()
                }
                override fun onRmsChanged(rmsdB: Float) {
                    if (rmsdB > 5.0f) lastSpeechTime = System.currentTimeMillis()
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (isListening && (error == 7 || error == 8)) {
                        recognizer.startListening(intent)
                    } else {
                        isListening = false
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        recognizedText = text
                        if (text.lowercase().contains("listo")) onResult(text.replace("listo", "", true).trim())
                        else lastSpeechTime = System.currentTimeMillis()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recognizedText = matches[0]
                        lastSpeechTime = System.currentTimeMillis()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(intent)
        }
    }

    LaunchedEffect(Unit) {
        startListening()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String?) { isJarvisSpeaking = true }
                    override fun onDone(id: String?) {
                        isJarvisSpeaking = false
                        coroutineScope.launch { if (!isFinalAction) startListening() else onDismiss() }
                    }
                    override fun onError(id: String?) { isJarvisSpeaking = false }
                })
                isTtsReady = true
            }
        }
    }

    LaunchedEffect(aiResponse) {
        if (aiResponse != null) {
            isListening = false
            speechRecognizer?.stopListening()
            if (isTtsReady) {
                val p = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "msg") }
                tts?.speak(aiResponse, TextToSpeech.QUEUE_FLUSH, p, "msg")
            }
        }
    }

    LaunchedEffect(isListening, lastSpeechTime, isJarvisSpeaking) {
        if (isListening && !isJarvisSpeaking) {
            while (true) {
                delay(500)
                if (System.currentTimeMillis() - lastSpeechTime > 8000 && recognizedText != "Te escucho..." && recognizedText != "...") {
                    onResult(recognizedText)
                    isListening = false
                    break
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(24.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, tonalElevation = 12.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.wallet))
                val prog by animateLottieCompositionAsState(comp, iterations = LottieConstants.IterateForever, isPlaying = isListening || isProcessing || isJarvisSpeaking)
                LottieAnimation(comp, prog, modifier = Modifier.size(160.dp))
                Spacer(Modifier.height(24.dp))
                if (aiResponse != null || isProcessing || isJarvisSpeaking) {
                    Text(aiResponse ?: "Procesando...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                }
                Text(text = when {
                    isJarvisSpeaking -> "Jarvis hablando..."
                    isProcessing -> "Pensando..."
                    recognizedText == "..." -> "Escuchando..."
                    else -> recognizedText
                }, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}