package com.deudom.deudom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deudom.deudom.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Log

sealed class AIAction {
    data class AddExpense(val amount: Double, val description: String, val category: String) : AIAction()
    data class AddLoan(val amount: Double, val description: String, val category: String, val friendName: String, val isLender: Boolean) : AIAction()
    data class AddPayment(val amount: Double, val friendName: String, val isPayingThem: Boolean) : AIAction()
    data class ChatResponse(val message: String) : AIAction()
    object Unknown : AIAction()
}

sealed class AIUiState {
    object Idle : AIUiState()
    object Loading : AIUiState()
    data class Success(val action: AIAction, val originalText: String, val isFinal: Boolean = true) : AIUiState()
    data class Error(val message: String) : AIUiState()
}

class AIAssistantViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AIUiState>(AIUiState.Idle)
    val uiState: StateFlow<AIUiState> = _uiState

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            responseMimeType = "application/json"
        }
    )

    private var chatSession: Chat? = null

    private fun getSystemInstruction(friendsContext: String = "") = """Eres DeudomIA, el asistente de élite de Deudom. Tu personalidad es la de Jarvis (Iron Man): sofisticado, eficiente y proactivo.

TU MISIÓN:
Registrar finanzas en RD$ mediante conversación. Debes extraer datos exactos para el JSON.

CONOCIMIENTO DE AMIGOS:
$friendsContext

REGLAS DE ORO:
1. MONEDA: Usa ÚNICAMENTE Pesos Dominicanos (RD$). Ignora otras monedas.
2. DIRECCIÓN DEL DINERO:
   - "Le presté a [Nombre]": isLender = true.
   - "[Nombre] me prestó": isLender = false.
   - "Pagué a [Nombre]": isPayingThem = true (ADD_PAYMENT).
   - "[Nombre] me pagó": isPayingThem = false (ADD_PAYMENT).
3. DATOS FALTANTES: Si falta el monto o el nombre del amigo, PREGUNTA amablemente antes de poner is_final = true.
4. JSON: Responde EXCLUSIVAMENTE con el objeto JSON.

FORMATO:
{
  "action": "ADD_EXPENSE" | "ADD_LOAN" | "ADD_PAYMENT" | "CHAT",
  "is_final": boolean (true solo si tienes todos los datos),
  "amount": número,
  "description": "string",
  "category": "string",
  "friendName": "string",
  "isLender": boolean,
  "isPayingThem": boolean,
  "message": "Tu respuesta como Jarvis"
}""".trimIndent()

    fun processInput(text: String, friendsList: List<String> = emptyList()) {
        val friendsContext = if (friendsList.isNotEmpty()) {
            "Tus amigos: ${friendsList.joinToString(", ")}."
        } else ""

        viewModelScope.launch {
            _uiState.value = AIUiState.Loading
            try {
                if (chatSession == null) {
                    chatSession = generativeModel.startChat(
                        history = listOf(
                            content(role = "user") { text(getSystemInstruction(friendsContext)) },
                            content(role = "model") { text("{ \"action\": \"CHAT\", \"message\": \"Sistemas en línea. ¿En qué puedo ayudarle hoy?\", \"is_final\": false }") }
                        )
                    )
                }
                
                val response = chatSession?.sendMessage(text)
                handleResponse(response?.text ?: "", text)
            } catch (e: Exception) {
                _uiState.value = AIUiState.Error("Error: ${e.message}")
            }
        }
    }

    private fun handleResponse(jsonString: String, originalText: String) {
        try {
            val cleanJson = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonObject = JSONObject(cleanJson)
            val action = jsonObject.optString("action", "UNKNOWN")
            val isFinal = jsonObject.optBoolean("is_final", true)
            val message = jsonObject.optString("message", "")

            val parsedAction = when (action) {
                "ADD_EXPENSE" -> AIAction.AddExpense(
                    amount = jsonObject.optDouble("amount", 0.0),
                    description = jsonObject.optString("description", "Gasto"),
                    category = jsonObject.optString("category", "Otros")
                )
                "ADD_LOAN" -> AIAction.AddLoan(
                    amount = jsonObject.optDouble("amount", 0.0),
                    description = jsonObject.optString("description", "Préstamo"),
                    category = jsonObject.optString("category", "Otros"),
                    friendName = jsonObject.optString("friendName", ""),
                    isLender = jsonObject.optBoolean("isLender", true)
                )
                "CHAT" -> AIAction.ChatResponse(message = message)
                else -> AIAction.Unknown
            }
            
            if (!isFinal || action == "CHAT") {
                _uiState.value = AIUiState.Success(AIAction.ChatResponse(message), originalText, isFinal = false)
            } else {
                _uiState.value = AIUiState.Success(parsedAction, originalText, isFinal = true)
            }
        } catch (e: Exception) {
             _uiState.value = AIUiState.Error("No pude procesar la instrucción.")
        }
    }

    fun resetState() { _uiState.value = AIUiState.Idle }
    fun clearChat() { chatSession = null }
}