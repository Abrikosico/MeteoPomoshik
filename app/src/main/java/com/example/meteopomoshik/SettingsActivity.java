package com.example.meteopomoshik;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utils.DailyForecastWorker;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {
    private SwitchMaterial swNotifications, swPressure, swGeomag;
    private TextView tvUserEmail;
    private MaterialButton btnLogout;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swNotifications = findViewById(R.id.swNotifications);
        swPressure = findViewById(R.id.swPressure);
        swGeomag = findViewById(R.id.swGeomag);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Показываем Email текущего пользователя
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        swNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        swPressure.setChecked(prefs.getBoolean("sensitiveToPressure", true));
        swGeomag.setChecked(prefs.getBoolean("sensitiveToGeomag", true));

        // Слушатель: Вкл/Выкл уведомлений
        swNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            manageWorker(isChecked);
        });

        swPressure.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("sensitiveToPressure", isChecked).apply());

        swGeomag.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("sensitiveToGeomag", isChecked).apply());

        // Выход из аккаунта
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // --- ОБНОВЛЕНО: 6 часов ---
    private void manageWorker(boolean enabled) {
        if (enabled) {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    DailyForecastWorker.class, 6, TimeUnit.HOURS) // 6 часов
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "daily_forecast",
                    ExistingPeriodicWorkPolicy.UPDATE, // Обновляем существующую
                    request);
        } else {
            WorkManager.getInstance(this).cancelUniqueWork("daily_forecast");
        }
    }
}