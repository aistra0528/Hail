package com.aistra.hail.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppMetadataDao {
    @Query("SELECT * FROM app_metadata")
    List<AppMetadataEntity> loadAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<AppMetadataEntity> entries);

    @Query("DELETE FROM app_metadata")
    void deleteAll();

    @Query("UPDATE app_metadata SET installed = 0")
    void markAllUninstalled();

    @androidx.room.Transaction
    default void replaceAll(List<AppMetadataEntity> entries) {
        deleteAll();
        upsertAll(entries);
    }
}
