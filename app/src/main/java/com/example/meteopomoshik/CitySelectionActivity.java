package com.example.meteopomoshik;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.api.RetrofitClient;
import com.api.WeatherApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.model.GeoLocation;
import com.utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CitySelectionActivity extends AppCompatActivity {
    private AutoCompleteTextView etCityName;
    private Button btnSearch, btnUseLocation;
    private ImageButton btnBack;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Используем наш новый адаптер
    private CityAdapter adapter;
    private List<GeoLocation> foundLocations = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_selection);

        etCityName = findViewById(R.id.etCityName);
        btnSearch = findViewById(R.id.btnSearchCity);
        btnUseLocation = findViewById(R.id.btnUseCurrentLocation);
        btnBack = findViewById(R.id.btnBack);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnBack.setOnClickListener(v -> finish());

        // Настраиваем красивый выпадающий список
        etCityName.setDropDownBackgroundResource(R.drawable.bg_popup_dark); // Наш темный фон
        etCityName.setDropDownVerticalOffset(16); // Отступ вниз

        setupAutocomplete();

        btnSearch.setOnClickListener(v -> {
            String city = etCityName.getText().toString().trim();
            if (!city.isEmpty()) saveCityAndFinish(city);
            else Toast.makeText(this, "Введите город", Toast.LENGTH_SHORT).show();
        });

        btnUseLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation());
    }

    private void setupAutocomplete() {
        etCityName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    searchRunnable = () -> searchCities(s.toString());
                    handler.postDelayed(searchRunnable, 500); // Ждем 500мс после ввода
                }
            }
        });

        // Обработка клика по элементу из списка
        etCityName.setOnItemClickListener((parent, view, position, id) -> {
            GeoLocation selected = adapter.getItem(position);
            if (selected != null) {
                // Заполняем поле красиво "Москва" (а не "Москва, RU")
                etCityName.setText(selected.name);
                etCityName.setSelection(selected.name.length()); // Курсор в конец
                saveLocationAndFinish(selected);
            }
        });
    }

    private void searchCities(String query) {
        WeatherApiService service = RetrofitClient.getWeatherClient(Constants.BASE_URL_GEOCODING).create(WeatherApiService.class);
        // "count=10" - просим больше вариантов, чтобы компенсировать ошибки ввода
        service.searchCity(query, 10, "ru", "json").enqueue(new Callback<GeoLocation.Response>() {
            @Override
            public void onResponse(Call<GeoLocation.Response> call, Response<GeoLocation.Response> response) {
                if (response.isSuccessful() && response.body() != null && response.body().results != null) {
                    foundLocations = response.body().results;

                    // Обновляем адаптер
                    adapter = new CityAdapter(CitySelectionActivity.this, foundLocations);
                    etCityName.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override public void onFailure(Call<GeoLocation.Response> call, Throwable t) {
                // Ошибки тихо игнорируем при автопоиске
            }
        });
    }

    // --- Логика Геолокации и Сохранения (Без изменений) ---
    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        Toast.makeText(this, "Определяем местоположение...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    String cityName = "Моё местоположение";
                    if (addresses != null && !addresses.isEmpty()) {
                        cityName = addresses.get(0).getLocality();
                    }
                    getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .putString("city", cityName)
                            .putFloat("lat", (float) location.getLatitude())
                            .putFloat("lon", (float) location.getLongitude())
                            .apply();
                    finish();
                } catch (IOException e) {
                    Toast.makeText(this, "Ошибка определения адреса", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Включите GPS", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    private void saveLocationAndFinish(GeoLocation loc) {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putString("city", loc.name)
                .putFloat("lat", (float) loc.latitude)
                .putFloat("lon", (float) loc.longitude)
                .apply();
        finish();
    }

    private void saveCityAndFinish(String city) {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putString("city", city).apply();
        finish();
    }
}