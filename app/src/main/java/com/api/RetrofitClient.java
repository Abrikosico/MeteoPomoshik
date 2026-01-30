package com.api;

import java.util.HashMap;
import java.util.Map;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Храним список клиентов для разных URL
    private static Map<String, Retrofit> instances = new HashMap<>();

    public static Retrofit getWeatherClient(String baseUrl) {
        // Если клиента для этого URL еще нет — создаем
        if (!instances.containsKey(baseUrl)) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            instances.put(baseUrl, retrofit);
        }
        // Возвращаем нужный клиент
        return instances.get(baseUrl);
    }
}