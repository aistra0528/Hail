# 雹 Hail

雹是一款通过 DPM / Root “冻结”应用的自由软件。

## 运行模式

雹支持以`DPM - 隐藏`和`Root - 停用`模式运行。

### DPM - 隐藏

此模式通过 ADB 将雹设置为 Device Owner，调用`DevicePolicyManager`中的`setApplicationHidden`方法隐藏应用。

```java
package android.app.admin;

public class DevicePolicyManager {
    public boolean setApplicationHidden(@NonNull ComponentName admin, String packageName, boolean hidden) {
        // ...
    }
}
```

#### 特征

被隐藏的应用不可使用，也不会出现在启动器和已安装应用列表中。（通过`PackageManager.MATCH_UNINSTALLED_PACKAGES`，雹仍可获取被隐藏应用的信息）

#### 优点

- 无需 Root。

#### 缺点

- 设置 Device Owner 较为繁琐。
- 设置为 Device Owner 的应用需要移除 Device Owner 后才能卸载。
- 取消隐藏应用时，已授予应用的权限会被重置，需要重新授予。

#### 通过 ADB 将雹设置为 Device Owner

在雹的设置页面将运行模式设置为`DPM - 隐藏`，然后在开发者选项中启用 USB 调试。

输入命令：

```shell
adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver
```

设置成功后会输出：

```
Success: Device owner set to package com.aistra.hail
Active admin set to component {com.aistra.hail/com.aistra.hail.receiver.DeviceAdminReceiver}
```

如输出其他内容，请使用搜索引擎自行查阅与解决。

#### 移除雹的 Device Owner

在雹的应用页面找到雹，点按后在弹出的选项中选择卸载。

### Root - 停用

此模式通过授予雹 Root 权限，调用`pm`中的`disable`命令停用应用。

以停用 HTML Viewer 为例：

```shell
pm disable com.android.htmlviewer
```

#### 特征

被停用的应用不可使用，也不会出现在启动器中。在已安装应用列表中应用会显示已停用状态。

## TODO

雹还处于早期开发阶段。

- 应用搜索与排序
- 快捷方式
