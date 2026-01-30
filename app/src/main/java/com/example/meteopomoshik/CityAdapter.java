package com.example.meteopomoshik;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.model.GeoLocation;

import java.util.ArrayList;
import java.util.List;

public class CityAdapter extends ArrayAdapter<GeoLocation> {
    private List<GeoLocation> items;

    public CityAdapter(@NonNull Context context, @NonNull List<GeoLocation> items) {
        super(context, 0, items);
        this.items = new ArrayList<>(items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_city_suggestion, parent, false);
        }

        GeoLocation location = getItem(position);

        if (location != null) {
            TextView tvCityName = convertView.findViewById(R.id.tvCityName);
            TextView tvCountry = convertView.findViewById(R.id.tvCountry);

            tvCityName.setText(location.name);

            // Собираем строку: "Область, Страна"
            StringBuilder details = new StringBuilder();
            if (location.admin1 != null && !location.admin1.isEmpty()) {
                details.append(location.admin1);
            }
            if (location.country != null && !location.country.isEmpty()) {
                if (details.length() > 0) details.append(", ");
                details.append(location.country);
            }
            tvCountry.setText(details.toString());
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = items;
                results.count = items.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}