package com.example.expensetracker.core.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Placeholder RetrofitClient for Phase 3 (Deferred Syncing & AI Integration).
 * This will be expanded when implementing the sync service and AI assistant.
 */
public class RetrofitClient {

    private static final String BASE_URL = "https://api.example.com/";
    private static volatile Retrofit instance;

    private RetrofitClient() {
        // Private constructor to prevent instantiation
    }

    public static Retrofit getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return instance;
    }
}
