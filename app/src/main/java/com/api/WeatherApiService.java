package com.api;

import com.model.GeoLocation;
import com.model.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    // 1. Погода: запрашиваем температуру и давление
    @GET("v1/forecast")
    Call<WeatherResponse> getWeather(
            @Query("latitude") double lat,
            @Query("longitude") double lon,
            @Query("current") String currentParams // "temperature_2m,surface_pressure"
    );

    // 2. Поиск города: count - кол-во результатов, language - язык
    @GET("v1/search")
    Call<GeoLocation.Response> searchCity(
            @Query("name") String name,
            @Query("count") int count,
            @Query("language") String lang,
            @Query("format") String format
    );
}