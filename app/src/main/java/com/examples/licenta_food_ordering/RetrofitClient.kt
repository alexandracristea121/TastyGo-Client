package com.examples.licenta_food_ordering

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {

    companion object {
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://8ed5-89-136-45-154.ngrok-free.app") // TODO: change all the time
                .addConverterFactory(GsonConverterFactory.create())
                .client(
                    OkHttpClient.Builder().addInterceptor(LoggingInterceptor()).build()
                )
                .build()
        }

        val apiService: ApiService by lazy {
            retrofit.create(ApiService::class.java)
        }
    }
}

fun LoggingInterceptor(): HttpLoggingInterceptor {
    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY  // Log request and response body

    return loggingInterceptor
}