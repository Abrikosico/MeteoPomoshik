package com.model;

import java.util.List;

public class GeoLocation {
    // Вспомогательный класс для ответа поиска
    public static class Response {
        public List<GeoLocation> results;
    }

    public String name;
    public double latitude;
    public double longitude;
    public String country;
    public String admin1; // Область/Регион (для уточнения)
}