package com.model;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {

    @SerializedName("current")
    public Current current;

    public static class Current {
        // Давление
        @SerializedName("surface_pressure")
        public double pressure;

        // Температура
        @SerializedName("temperature_2m")
        public double temperature;

        // Скорость ветра
        @SerializedName("wind_speed_10m")
        public double windSpeed;
    }
}