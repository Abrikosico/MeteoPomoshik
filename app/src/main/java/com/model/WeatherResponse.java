package com.model;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("current")
    public Current current;

    public static class Current {
        @SerializedName("temperature_2m")
        public double temp;

        @SerializedName("surface_pressure")
        public double pressure; // гПа
    }
}