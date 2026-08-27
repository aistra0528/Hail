package com.aistra.hail.utils;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {AppMetadataEntity.class}, version = 2, exportSchema = false)
public abstract class AppMetadataDatabase extends RoomDatabase {
    public abstract AppMetadataDao appMetadataDao();
}
