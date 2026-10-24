package com.example.data.api

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiClient"

object GeminiClient {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text("You are Narayan OS AI, an intelligent, patient, and friendly personal study assistant for school and college students. " +
                     "Explain concepts in simple, structured language. When asked in Hindi (or Hinglish), answer in simple Hindi with English key terms where helpful. " +
                     "Break complex problems down step-by-step. Include formulas, bullet points, and practical real-life examples where relevant.")
            }
        )
    }

    suspend fun askAssistant(
        prompt: String,
        history: List<Pair<String, String>> = emptyList() // Pair(User, AI)
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val reply = withTimeout(60_000L) {
                val chat = model.startChat(
                    history = history.flatMap { (userText, modelText) ->
                        listOf(
                            content("user") { text(userText) },
                            content("model") { text(modelText) }
                        )
                    }
                )
                chat.sendMessage(prompt).text
            }
            if (!reply.isNullOrBlank()) {
                Result.success(reply)
            } else {
                Result.failure(Exception("Received an empty response from AI model."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            Result.failure(e)
        }
    }
}
