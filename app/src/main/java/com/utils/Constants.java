package com.utils;

public class Constants {
    // Погода (Open-Meteo)
    public static final String BASE_URL_WEATHER = "https://api.open-meteo.com/";
    // Поиск городов (Open-Meteo)
    public static final String BASE_URL_GEOCODING = "https://geocoding-api.open-meteo.com/";
    // Магнитные бури (NOAA)
    public static final String BASE_URL_NOAA = "https://services.swpc.noaa.gov/";

    public static double hPaToMmHg(double hPa) {
        return hPa * 0.75006;
    }

    // Обновленная логика риска
    public static String getRiskLevel(double pressure, int kpIndex, boolean isSensitiveToPressure, boolean isSensitiveToGeomag) {
        boolean highKp = kpIndex >= 5; // Буря
        boolean lowPressure = pressure < 740; // Низкое давление

        if ((isSensitiveToPressure && lowPressure) && (isSensitiveToGeomag && highKp)) {
            return "🔴 Очень высокий риск (Комбо)";
        } else if ((isSensitiveToPressure && lowPressure) || (isSensitiveToGeomag && highKp)) {
            return "🟠 Средний риск";
        } else {
            return "🟢 Низкий риск";
        }
    }

    public static String getAdvice(double pressure, int kpIndex, boolean isSensitiveToPressure, boolean isSensitiveToGeomag) {
        StringBuilder advice = new StringBuilder();

        if (isSensitiveToPressure && pressure < 740) {
            advice.append("Ожидается низкое давление. Пейте больше воды, возможна мигрень. ");
        }

        if (isSensitiveToGeomag && kpIndex >= 5) {
            advice.append("Сильная магнитная буря! Избегайте стрессов и физнагрузок.");
        }

        if (advice.length() == 0) {
            return "Сегодня отличный день! Погода на вашей стороне.";
        }

        return advice.toString();
    }
}