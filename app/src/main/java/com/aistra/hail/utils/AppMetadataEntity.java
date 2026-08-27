package com.aistra.hail.utils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_metadata")
public class AppMetadataEntity {
    @PrimaryKey
    @NonNull
    public String packageName = "";
    public String name = "";
    public boolean systemApp;
    public long firstInstallTime;
    public long lastUpdateTime;
    public int flags;
    public boolean enabled;
    public boolean installed;
    @NonNull
    public String sourceSignature = "";
}
