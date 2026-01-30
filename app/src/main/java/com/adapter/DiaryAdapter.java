package com.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meteopomoshik.R;
import com.model.WellbeingEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.ViewHolder> {
    private Context context;
    private List<WellbeingEntry> entries;

    public DiaryAdapter(Context context, List<WellbeingEntry> entries) {
        this.context = context;
        this.entries = entries;
    }

    public void updateData(List<WellbeingEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_diary_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WellbeingEntry entry = entries.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
        holder.tvDate.setText(sdf.format(new Date(entry.timestamp)));

        // Оценка
        holder.tvRating.setText(entry.rating + "/10");
        if (entry.rating >= 8) holder.tvRating.setTextColor(Color.parseColor("#2ecc71"));
        else if (entry.rating >= 4) holder.tvRating.setTextColor(Color.parseColor("#f1c40f"));
        else holder.tvRating.setTextColor(Color.parseColor("#e74c3c"));

        // !! ОТОБРАЖЕНИЕ ГОРОДА !!
        if (entry.cityName != null && !entry.cityName.isEmpty()) {
            holder.tvCityEntry.setText(entry.cityName);
            holder.tvCityEntry.setVisibility(View.VISIBLE);
        } else {
            // Если это старая запись без города
            holder.tvCityEntry.setVisibility(View.GONE);
        }

        // Симптомы
        StringBuilder symptoms = new StringBuilder();
        if (entry.headache) symptoms.append("Головная боль, ");
        if (entry.fatigue) symptoms.append("Усталость, ");
        if (entry.dizziness) symptoms.append("Головокружение, ");

        if (symptoms.length() > 0) {
            symptoms.setLength(symptoms.length() - 2);
            holder.tvSymptoms.setText("Симптомы: " + symptoms.toString());
            holder.tvSymptoms.setVisibility(View.VISIBLE);
        } else {
            holder.tvSymptoms.setText("Симптомов нет");
        }

        // Заметки
        if (entry.notes != null && !entry.notes.isEmpty()) {
            holder.tvNotes.setText(entry.notes);
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvRating, tvSymptoms, tvNotes, tvCityEntry; // Добавили tvCityEntry

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvCityEntry = itemView.findViewById(R.id.tvCityEntry); // Нашли View
        }
    }
}