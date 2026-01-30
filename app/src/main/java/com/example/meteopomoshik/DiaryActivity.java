package com.example.meteopomoshik;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adapter.DiaryAdapter;
import com.database.AppDatabase;
import com.database.WellbeingDao;
import com.google.android.material.slider.Slider;
import com.model.WellbeingEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class DiaryActivity extends AppCompatActivity {
    private Slider sliderRating;
    private TextView tvRatingValue;
    private CheckBox cbHeadache, cbFatigue, cbDizziness;
    private EditText etNotes;
    private Button btnSave;
    private RecyclerView recyclerView;

    private DiaryAdapter adapter;
    private AppDatabase db;
    private WellbeingDao dao;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            finish();
            return;
        }

        initViews();
        db = AppDatabase.getDatabase(this);
        dao = db.wellbeingDao();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadEntries();
        btnSave.setOnClickListener(v -> saveEntry());
    }

    private void initViews() {
        sliderRating = findViewById(R.id.sliderRating);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        cbHeadache = findViewById(R.id.cbHeadache);
        cbFatigue = findViewById(R.id.cbFatigue);
        cbDizziness = findViewById(R.id.cbDizziness);
        etNotes = findViewById(R.id.etNotes);
        btnSave = findViewById(R.id.btnSaveEntry);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DiaryAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        sliderRating.addOnChangeListener((slider, value, fromUser) -> {
            tvRatingValue.setText(String.valueOf((int) value));
        });
    }

    private void loadEntries() {
        new Thread(() -> {
            List<WellbeingEntry> entries = dao.getLastEntries(currentUserId);
            new Handler(Looper.getMainLooper()).post(() -> adapter.updateData(entries));
        }).start();
    }

    private void saveEntry() {
        int rating = (int) sliderRating.getValue();
        boolean headache = cbHeadache.isChecked();
        boolean fatigue = cbFatigue.isChecked();
        boolean dizziness = cbDizziness.isChecked();
        String notes = etNotes.getText().toString().trim();

        // !! ПОЛУЧАЕМ ТЕКУЩИЙ ГОРОД ИЗ НАСТРОЕК !!
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String city = prefs.getString("city", "Неизвестно");

        WellbeingEntry entry = new WellbeingEntry();
        entry.userId = currentUserId;
        entry.cityName = city; // !! СОХРАНЯЕМ ГОРОД
        entry.timestamp = System.currentTimeMillis();
        entry.rating = rating;
        entry.headache = headache;
        entry.fatigue = fatigue;
        entry.dizziness = dizziness;
        entry.notes = notes;
        entry.pressure = 0;

        new Thread(() -> {
            dao.insert(entry);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "Запись сохранена", Toast.LENGTH_SHORT).show();
                loadEntries();
                clearForm();
            });
        }).start();
    }

    private void clearForm() {
        sliderRating.setValue(5);
        tvRatingValue.setText("5");
        cbHeadache.setChecked(false);
        cbFatigue.setChecked(false);
        cbDizziness.setChecked(false);
        etNotes.setText("");
    }
}