package com.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diary")
public class WellbeingEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;

    // !! НОВОЕ ПОЛЕ !!
    public String cityName; // Где была сделана запись

    public long timestamp;
    public int rating;
    public boolean headache;
    public boolean fatigue;
    public boolean dizziness;
    public String notes;
    public double pressure;
}