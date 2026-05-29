package com.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.api.KpApiService;
import com.api.RetrofitClient;
import com.api.WeatherApiService;
import com.example.meteopomoshik.MainActivity;
import com.example.meteopomoshik.R;
import com.model.KpIndex;
import com.model.WeatherResponse;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

public class DailyForecastWorker extends Worker {

    public DailyForecastWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // 1. Проверяем, включены ли уведомления
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notificationsEnabled) return Result.success();

        // 2. Получаем координаты
        float lat = prefs.getFloat("lat", 0);
        float lon = prefs.getFloat("lon", 0);
        if (lat == 0 && lon == 0) return Result.failure();

        try {
            // 3. Синхронный запрос погоды (в фоновом потоке это можно)
            WeatherApiService weatherService = RetrofitClient.getWeatherClient(Constants.BASE_URL_WEATHER).create(WeatherApiService.class);
            Response<WeatherResponse> weatherResponse = weatherService.getWeather(lat, lon, "temperature_2m,surface_pressure").execute();

            KpApiService kpService = RetrofitClient.getWeatherClient(Constants.BASE_URL_NOAA).create(KpApiService.class);
            Response<List<KpIndex>> kpResponse = kpService.getKpIndex().execute();

            if (weatherResponse.isSuccessful() && weatherResponse.body() != null &&
                    kpResponse.isSuccessful() && kpResponse.body() != null && !kpResponse.body().isEmpty()) {

                double pressure = Constants.hPaToMmHg(weatherResponse.body().current.pressure);

                List<KpIndex> list = kpResponse.body();
                int kp = list.get(list.size() - 1).kpIndex;

                boolean sensitivePressure = prefs.getBoolean("sensitiveToPressure", true);
                boolean sensitiveGeomag = prefs.getBoolean("sensitiveToGeomag", true);

                // 4. Анализируем риск
                String risk = Constants.getRiskLevel(pressure, kp, sensitivePressure, sensitiveGeomag);

                // Если есть риск (Средний или Высокий) — шлем уведомление
                if (risk.contains("Высокий") || risk.contains("Средний")) {
                    String message = "Внимание! " + risk + ". Давление: " + (int)pressure + ", Kp: " + kp;
                    sendNotification(context, "Предупреждение о погоде", message);
                }
            }

            return Result.success();

        } catch (IOException e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void sendNotification(Context context, String title, String message) {
        String channelId = "weather_alerts";

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_risk_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);


        try {
            notificationManager.notify(1001, builder.build());
        } catch (SecurityException e) {

        }
    }
}