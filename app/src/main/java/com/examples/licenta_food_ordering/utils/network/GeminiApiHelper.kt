package com.examples.licenta_food_ordering.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

object GeminiApiHelper {
    private val client = OkHttpClient()
    private const val apiKey =
        "AIzaSyDkpOJsP8eK4QKQfMZRxWPqPOyV4Kztyt8" // Replace with your actual Gemini API key

    fun getGeminiResponse(userMessage: String, callback: (String) -> Unit) {
        // Update the instruction to handle multi-word product names like "Pizza pepperoni"
        val instruction = """
Răspunde strict în formatul: "<nume produs complet> cu valoarea de maxim <valoare> ron".
Acceptă produse compuse sau cu arome, de exemplu: "pizza pepperoni cu valoarea de maxim 30 ron".
Nu adăuga explicații sau alt text. Mesajul utilizatorului: "$userMessage"
""".trimIndent().replace("\"", "\\\"")

        // Create the JSON request body
        val json = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "$instruction"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        // Make the network request to Gemini API
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Nu s-a putut contacta serverul.")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    // Parse the response to get the generated text
                    val rawResponse = JSONObject(body)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()

                    // Improve the regex to handle multi-word product names and price
                    val pattern = Pattern.compile(
                        "\"?(.+?) cu valoarea de maxim (\\d+) ron\"?",
                        Pattern.CASE_INSENSITIVE
                    )
                    val matcher = pattern.matcher(rawResponse)

                    if (matcher.find()) {
                        callback(
                            matcher.group().lowercase()
                        ) // Return the matched product and price info
                    } else {
                        callback("Te rog reformulează cererea în formatul: \"produs cu valoarea de maxim X ron\".")
                    }
                } catch (e: Exception) {
                    callback("Eroare la interpretarea răspunsului.")
                }
            }
        })
    }
}