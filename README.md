# 雹

雹是一款自由软件。

ADB 设置 Device Owner

```shell
adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver
```

DPM - 隐藏

```java
package android.app.admin;

public class DevicePolicyManager {
    public boolean setApplicationHidden(@NonNull ComponentName admin, String packageName, boolean hidden) {
        // ...
    }
}
```

Root - 停用

```shell
pm disable com.example.myapplication
```

## TODO

雹还处于早期开发阶段。

- 应用搜索
- 快捷方式