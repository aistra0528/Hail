package com.aistra.hail.services;

interface IDhizukuService {
    void onCreate() = 1;
    void onDestroy() = 2;

    /* Transact codes up to 20 are reserved for future Dhizuku API. */

    boolean lockScreen() = 21;

    boolean isAppHidden(String packageName) = 22;

    boolean setAppHidden(String packageName, boolean hidden) = 23;

    boolean setAppSuspended(String packageName, boolean suspended) = 24;

    boolean uninstallApp(String packageName) = 25;
}