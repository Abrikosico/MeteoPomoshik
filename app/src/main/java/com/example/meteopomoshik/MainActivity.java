package com.example.meteopomoshik;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.api.KpApiService;
import com.api.RetrofitClient;
import com.api.WeatherApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.model.GeoLocation;
import com.model.KpIndex;
import com.model.WeatherResponse;
import com.utils.Constants;
import com.utils.DailyForecastWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvCity, tvRiskLevel, tvAdvice, tvPressureInfo, tvKpInfo, tvSensitivityInfo;
    private Button btnChangeCity, btnOpenDiary;

    private ImageButton btnAdvice, btnSettings, btnMap;

    private View cardRiskBg;
    private ImageView ivRiskIcon;

    private double currentPressure = 0;
    private int currentKpIndex = 0;
    private boolean isSensitiveToPressure = true;
    private boolean isSensitiveToGeomag = true;

    private static final int REQUEST_CODE_NOTIFICATIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        createNotificationChannel();
        checkNotificationPermission();
        scheduleWorker();

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDataAndWeather();
    }

    private void initViews() {
        tvCity = findViewById(R.id.tvCity);
        tvRiskLevel = findViewById(R.id.tvRiskLevel);
        tvAdvice = findViewById(R.id.tvAdvice);
        tvPressureInfo = findViewById(R.id.tvPressureInfo);
        tvKpInfo = findViewById(R.id.tvKpInfo);
        tvSensitivityInfo = findViewById(R.id.tvSensitivityInfo);

        btnChangeCity = findViewById(R.id.btnChangeCity);
        btnOpenDiary = findViewById(R.id.btnOpenDiary);
        btnAdvice = findViewById(R.id.btnAdvice);
        btnSettings = findViewById(R.id.btnSettings);

        // !! Находим кнопку карты
        btnMap = findViewById(R.id.btnMap);

        cardRiskBg = findViewById(R.id.cardRiskBg);
        ivRiskIcon = findViewById(R.id.ivRiskIcon);
    }

    private void setupListeners() {
        btnChangeCity.setOnClickListener(v -> startActivity(new Intent(this, CitySelectionActivity.class)));
        btnOpenDiary.setOnClickListener(v -> startActivity(new Intent(this, DiaryActivity.class)));
        btnAdvice.setOnClickListener(v -> startActivity(new Intent(this, AdviceActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // !! Переход на карту
        btnMap.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
    }

    private void loadUserDataAndWeather() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        isSensitiveToPressure = prefs.getBoolean("sensitiveToPressure", true);
        isSensitiveToGeomag = prefs.getBoolean("sensitiveToGeomag", true);
        updateSensitivityText();

        String cityName = prefs.getString("city", "Выберите город");
        tvCity.setText(cityName);

        float lat = prefs.getFloat("lat", 0);
        float lon = prefs.getFloat("lon", 0);

        if (lat != 0 && lon != 0) {
            fetchWeatherData(lat, lon);
        } else {
            if (!cityName.equals("Выберите город")) {
                findCityCoordinates(cityName);
            } else {
                tvAdvice.setText("Пожалуйста, выберите город для получения прогноза.");
            }
        }
    }

    private void findCityCoordinates(String cityName) {
        WeatherApiService service = RetrofitClient.getWeatherClient(Constants.BASE_URL_GEOCODING).create(WeatherApiService.class);
        service.searchCity(cityName, 1, "ru", "json").enqueue(new Callback<GeoLocation.Response>() {
            @Override
            public void onResponse(Call<GeoLocation.Response> call, Response<GeoLocation.Response> response) {
                if (response.isSuccessful() && response.body() != null && response.body().results != null && !response.body().results.isEmpty()) {
                    GeoLocation loc = response.body().results.get(0);
                    getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .putString("city", loc.name)
                            .putFloat("lat", (float) loc.latitude)
                            .putFloat("lon", (float) loc.longitude)
                            .apply();

                    tvCity.setText(loc.name);
                    fetchWeatherData(loc.latitude, loc.longitude);
                } else {
                    tvCity.setText("Не найден");
                }
            }
            @Override
            public void onFailure(Call<GeoLocation.Response> call, Throwable t) {}
        });
    }

    private void fetchWeatherData(double lat, double lon) {
        WeatherApiService service = RetrofitClient.getWeatherClient(Constants.BASE_URL_WEATHER).create(WeatherApiService.class);
        service.getWeather(lat, lon, "temperature_2m,surface_pressure").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().current != null) {
                    currentPressure = response.body().current.pressure;
                    getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            .edit()
                            .putFloat("last_pressure", (float) currentPressure)
                            .apply();
                    fetchKpIndex();
                }
            }
            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                tvAdvice.setText("Ошибка загрузки погоды.");
            }
        });
    }

    private void fetchKpIndex() {
        KpApiService kpService = RetrofitClient.getWeatherClient(Constants.BASE_URL_NOAA).create(KpApiService.class);
        kpService.getKpIndex().enqueue(new Callback<List<KpIndex>>() {
            @Override
            public void onResponse(Call<List<KpIndex>> call, Response<List<KpIndex>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<KpIndex> list = response.body();
                    currentKpIndex = list.get(list.size() - 1).kpIndex;
                    getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            .edit()
                            .putInt("last_kp", currentKpIndex)
                            .apply();
                    updateUI();
                }
            }
            @Override
            public void onFailure(Call<List<KpIndex>> call, Throwable t) {
                currentKpIndex = 0;
                updateUI();
            }
        });
    }

    private void updateUI() {
        double pressureMmHg = Constants.hPaToMmHg(currentPressure);
        tvPressureInfo.setText(String.format("%.0f мм рт.ст.", pressureMmHg));

        String stormStatus;
        if (currentKpIndex < 3) stormStatus = "Спокойно";
        else if (currentKpIndex < 5) stormStatus = "Возбужденное";
        else if (currentKpIndex < 7) stormStatus = "Буря (G1-G2)";
        else stormStatus = "Сильная буря (G3+)";

        tvKpInfo.setText(String.format("%s (Kp=%d)", stormStatus, currentKpIndex));

        String riskLevel = Constants.getRiskLevel(pressureMmHg, currentKpIndex, isSensitiveToPressure, isSensitiveToGeomag);
        String advice = Constants.getAdvice(pressureMmHg, currentKpIndex, isSensitiveToPressure, isSensitiveToGeomag);

        tvRiskLevel.setText(riskLevel);
        tvAdvice.setText(advice);

        if (riskLevel.contains("Высокий") || riskLevel.contains("Комбо")) {
            cardRiskBg.setBackgroundResource(R.drawable.bg_gradient_red);
            ivRiskIcon.setColorFilter(Color.WHITE);
        } else if (riskLevel.contains("Средний")) {
            cardRiskBg.setBackgroundResource(R.drawable.bg_gradient_orange);
            ivRiskIcon.setColorFilter(Color.WHITE);
        } else {
            cardRiskBg.setBackgroundResource(R.drawable.bg_gradient_green);
            ivRiskIcon.setColorFilter(Color.WHITE);
        }
    }

    private void updateSensitivityText() {
        String sensitivityText = "Вы ";
        if (!isSensitiveToPressure && !isSensitiveToGeomag) {
            sensitivityText += "не чувствительны к изменениям";
        } else {
            if (isSensitiveToPressure) sensitivityText += "чувствительны к давлению";
            if (isSensitiveToPressure && isSensitiveToGeomag) sensitivityText += " и ";
            if (isSensitiveToGeomag) sensitivityText += "магнитным бурям";
        }
        tvSensitivityInfo.setText(sensitivityText);
    }

    private void scheduleWorker() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DailyForecastWorker.class, 6, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_forecast",
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Погодные предупреждения";
            String description = "Уведомления о магнитных бурях и скачках давления";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("weather_alerts", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATIONS);
            }
        }
    }
}