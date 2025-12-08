package com.android.ootd.model.ai

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

/**
 * Interface for AI model operations.
 *
 * This abstraction allows for easy testing and mocking of AI functionality.
 */
interface AIModel {
  /**
   * Generates a description for an image.
   *
   * @param bitmap The image to generate a description for.
   * @param prompt The prompt to use for generation.
   * @return The generated text description.
   */
  suspend fun generateText(bitmap: Bitmap, prompt: String): String?
}

/** Production implementation of AIModel using Firebase AI. */
class FirebaseAIModel : AIModel {
  override suspend fun generateText(bitmap: Bitmap, prompt: String): String? {
    val model =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")

    val response =
        model.generateContent(
            content {
              image(bitmap)
              text(prompt)
            })

    return response.text
  }
}

/** Mock implementation for testing that doesn't use Firebase AI. */
class MockAIModel : AIModel {
  var mockResponse: String? = "Mock AI generated description"
  var shouldThrowError: Boolean = false
  var errorMessage: String = "Mock AI error"

  override suspend fun generateText(bitmap: Bitmap, prompt: String): String? {
    if (shouldThrowError) {
      throw Exception(errorMessage)
    }
    return mockResponse
  }
}

/**
 * Provider for AI model instances.
 *
 * Uses the Provider pattern to allow dependency injection and easy testing.
 */
object AIModelProvider {
  private var _model: AIModel = FirebaseAIModel()

  val model: AIModel
    get() = _model

  fun useFirebase() {
    _model = FirebaseAIModel()
  }

  fun useMock() {
    _model = MockAIModel()
  }

  fun setModel(customModel: AIModel) {
    _model = customModel
  }

  fun reset() {
    _model = FirebaseAIModel()
  }
}
