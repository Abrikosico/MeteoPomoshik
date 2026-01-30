package com.example.meteopomoshik;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.database.AppDatabase;
import com.database.WellbeingDao;
import com.google.firebase.auth.FirebaseAuth;
import com.model.Article;
import com.model.WellbeingEntry;
import com.utils.AdviceManager;
import java.util.List;

public class AdviceActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ImageButton btnBack;

    // Новые элементы UI
    private LinearLayout layoutLoading;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advice);

        initViews();

        btnBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Запускаем загрузку
        loadAdvice();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnBack = findViewById(R.id.btnBack);
        layoutLoading = findViewById(R.id.layoutLoading);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void loadAdvice() {
        // 1. Показываем загрузку, скрываем контент
        layoutLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        float currentPressureHpa = prefs.getFloat("last_pressure", 1013);
        int currentKp = prefs.getInt("last_kp", 2);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        new Thread(() -> {
            // Имитация бурной деятельности (чтобы пользователь успел увидеть красивый ProgressBar)
            // В реальности это не обязательно, но для UX иногда полезно 500мс задержки
            try { Thread.sleep(600); } catch (InterruptedException e) {}

            AppDatabase db = AppDatabase.getDatabase(this);
            WellbeingDao dao = db.wellbeingDao();
            List<WellbeingEntry> history = dao.getLastEntries(uid);

            List<Article> adviceList = AdviceManager.getPersonalizedAdvice(this, history, currentPressureHpa, currentKp);

            new Handler(Looper.getMainLooper()).post(() -> {
                // 2. Скрываем загрузку
                layoutLoading.setVisibility(View.GONE);

                if (adviceList != null && !adviceList.isEmpty()) {
                    // Данные есть -> Показываем список
                    AdviceAdapter adapter = new AdviceAdapter(adviceList);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    // Данных нет -> Показываем текст "Пусто"
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
}