package com.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.model.WellbeingEntry;
import java.util.List;

@Dao
public interface WellbeingDao {
    @Insert
    void insert(WellbeingEntry entry);

    // Берем записи ТОЛЬКО текущего пользователя
    @Query("SELECT * FROM diary WHERE userId = :uid ORDER BY timestamp DESC LIMIT 20")
    List<WellbeingEntry> getLastEntries(String uid);
}