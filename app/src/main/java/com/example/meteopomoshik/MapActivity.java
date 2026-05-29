package com.example.meteopomoshik;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.api.RetrofitClient;
import com.api.WeatherApiService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.model.WeatherResponse;
import com.utils.Constants;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity {
    private MapView map;
    private ImageButton btnBack, btnLayers, btnZoomIn, btnZoomOut, btnMyLocation;
    private TextView tvLegend;
    private static final String OWM_API_KEY = "0c30f77226ae20d24907801c37e0bf06";

    private static final int LAYER_PRESSURE = 0;
    private static final int LAYER_RAIN = 1;
    private static final int LAYER_WIND = 2;

    private int currentLayer = LAYER_PRESSURE;
    private TilesOverlay weatherOverlay;
    private Marker currentMarker;

    private GeoPoint homePoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        loadHomeLocation();

        initViews();
        setupListeners();
        setupBaseMap();
        setupMapClickListener();
        updateWeatherLayer(LAYER_PRESSURE);
    }

    private void loadHomeLocation() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        float lat = prefs.getFloat("lat", 55.75f);
        float lon = prefs.getFloat("lon", 37.61f);
        homePoint = new GeoPoint((double)lat, (double)lon);
    }

    private void initViews() {
        map = findViewById(R.id.map);
        btnBack = findViewById(R.id.btnBack);
        btnLayers = findViewById(R.id.btnLayers);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        tvLegend = findViewById(R.id.tvLegend);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnLayers.setOnClickListener(v -> showLayerDialog());

        btnZoomIn.setOnClickListener(v -> map.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> map.getController().zoomOut());

        btnMyLocation.setOnClickListener(v -> {
            if (homePoint != null) {
                map.getController().animateTo(homePoint);
                map.getController().setZoom(10.0);
            }
        });
    }

    private void setupBaseMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);

        map.getController().setZoom(10.0);

        if (homePoint != null) {
            map.getController().setCenter(homePoint);

            Marker homeMarker = new Marker(map);
            homeMarker.setPosition(homePoint);
            homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            homeMarker.setTitle("Мой город");
            map.getOverlays().add(homeMarker);
        }
    }

    private void setupMapClickListener() {
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                updateMarker(p);
                fetchWeatherForPoint(p);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(receiver);
        map.getOverlays().add(eventsOverlay);
    }

    private void updateMarker(GeoPoint p) {
        if (currentMarker != null) map.getOverlays().remove(currentMarker);

        currentMarker = new Marker(map);
        currentMarker.setPosition(p);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle("Выбрано");

        map.getOverlays().add(currentMarker);
        map.invalidate();
    }

    private void fetchWeatherForPoint(GeoPoint p) {
        Toast.makeText(this, "Анализ погоды...", Toast.LENGTH_SHORT).show();

        WeatherApiService service = RetrofitClient.getWeatherClient(Constants.BASE_URL_WEATHER).create(WeatherApiService.class);
        service.getWeather(p.getLatitude(), p.getLongitude(), "temperature_2m,surface_pressure,wind_speed_10m").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().current != null) {
                    showBeautifulWeatherDialog(
                            response.body().current.temperature,
                            response.body().current.pressure,
                            response.body().current.windSpeed,
                            p
                    );
                } else {
                    Toast.makeText(MapActivity.this, "Нет данных", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MapActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBeautifulWeatherDialog(double temp, double pressureHpa, double wind, GeoPoint p) {
        double pressureMmHg = Constants.hPaToMmHg(pressureHpa);

        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);

        View view = getLayoutInflater().inflate(R.layout.layout_weather_bottom_sheet, null);

        TextView tvTemp = view.findViewById(R.id.tvBsTemp);
        TextView tvCoords = view.findViewById(R.id.tvBsCoordinates);
        TextView tvPressure = view.findViewById(R.id.tvBsPressure);
        TextView tvWind = view.findViewById(R.id.tvBsWind);
        View btnClose = view.findViewById(R.id.btnBsClose);

        tvTemp.setText(String.format("%+.0f°", temp));
        tvCoords.setText(String.format("%.4f, %.4f", p.getLatitude(), p.getLongitude()));
        tvPressure.setText(String.format("%.0f", pressureMmHg));
        tvWind.setText(String.format("%.1f", wind));

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void showLayerDialog() {
        String[] options = {"Атмосферное давление", "Осадки (Дождь/Снег)", "Скорость ветра"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите слой карты");
        builder.setSingleChoiceItems(options, currentLayer, (dialog, which) -> {
            currentLayer = which;
            updateWeatherLayer(currentLayer);
            dialog.dismiss();
        });
        builder.show();
    }

    private void updateWeatherLayer(int layerType) {
        if (weatherOverlay != null) map.getOverlays().remove(weatherOverlay);

        String layerCode;
        String layerName;

        switch (layerType) {
            case LAYER_RAIN: layerCode = "precipitation_new"; layerName = "Слой: Осадки"; break;
            case LAYER_WIND: layerCode = "wind_new"; layerName = "Слой: Ветер"; break;
            case LAYER_PRESSURE: default: layerCode = "pressure_new"; layerName = "Слой: Давление"; break;
        }

        tvLegend.setText(layerName);

        OnlineTileSourceBase tileSource = new OnlineTileSourceBase(
                "OWM_" + layerCode, 0, 19, 256, "",
                new String[] { "https://tile.openweathermap.org/map/" + layerCode + "/" }
        ) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/"
                        + MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex)
                        + ".png?appid=" + OWM_API_KEY;
            }
        };

        weatherOverlay = new TilesOverlay(new org.osmdroid.tileprovider.MapTileProviderBasic(getApplicationContext(), tileSource), getApplicationContext());
        weatherOverlay.setLoadingBackgroundColor(android.graphics.Color.TRANSPARENT);

        map.getOverlays().add(weatherOverlay);
        if(currentMarker != null) {
            map.getOverlays().remove(currentMarker);
            map.getOverlays().add(currentMarker);
        }
        map.invalidate();
    }

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause() { super.onPause(); map.onPause(); }
}