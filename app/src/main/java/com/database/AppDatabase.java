package com.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.model.WellbeingEntry;

// ВЕРСИЯ 4
@Database(entities = {WellbeingEntry.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract WellbeingDao wellbeingDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "meteopomoshnik_db")
                            .fallbackToDestructiveMigration() // Старые данные сотрутся при обновлении
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}