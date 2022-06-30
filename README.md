简体中文 | [English](README_EN.md)

# 雹 Hail

雹是一款用于冻结 Android 应用的自由软件，您可自由享用所有功能。

## 冻结

冻结`freeze`是一个营销用语，用于描述使**应用在用户不需要时不可运行**的行为，以此控制设备使用、减少内存占用和节省电量。用户可在需要时解冻`unfreeze`应用。

通过隐藏和停用两种方式，都能达到所谓冻结的效果，但其实现存在差异。

### 隐藏

被隐藏`hide`的应用不可使用，也不会出现在启动器和已安装应用列表中。取消隐藏`unhide`应用即可恢复。

### 停用

被停用`disable`的应用不可使用，也不会出现在启动器中。在已安装应用列表中应用会显示已停用`disabled`状态。启用`enable`应用即可恢复。

## 工作模式

雹支持以`设备所有者 - 隐藏`、`超级用户 - 停用`和`Shizuku - 停用`模式工作。

**由于隐藏和停用的实现存在差异，冻结的应用需要通过相同方式解冻。**

1. 如果您的设备支持无线调试或已 root，推荐选择`Shizuku - 停用`。[关于 Shizuku](https://github.com/RikkaApps/Shizuku)

2. 如果您的设备已 root，可选择`超级用户 - 停用`。**此模式性能相对较差。**

3. 如果您的设备不支持无线调试且未 root，可选择`设备所有者 - 隐藏`。**此模式兼容性相对较差。**

### 设备所有者 - 隐藏

此模式通过将雹设置为设备所有者 (Device Owner)，调用`DevicePolicyManager.setApplicationHidden`方法隐藏应用。

**设置为设备所有者的应用需要移除设备所有者后方可卸载。**

#### 通过 adb 将雹设置为设备所有者

[Android 调试桥 (adb) 指南](https://developer.android.google.cn/studio/command-line/adb)

[下载 Android SDK 平台工具](https://developer.android.google.cn/studio/releases/platform-tools)

通过 adb 发出命令：

```shell
adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver
```

设置成功后会输出以下信息：

```
Success: Device owner set to package com.aistra.hail
Active admin set to component {com.aistra.hail/com.aistra.hail.receiver.DeviceAdminReceiver}
```

如输出其他信息，请使用搜索引擎自行查阅与解决。

#### 移除雹的设备所有者

在雹的应用界面点按雹，在弹出的选项中选择卸载。

### 超级用户 - 停用

此模式通过授予雹超级用户 (Superuser) 权限，执行`pm disable-user`命令停用应用。

### Shizuku - 停用

此模式通过 Shizuku 调用非 SDK 接口`IPackageManager.setApplicationEnabledSetting`方法停用应用。

## 恢复

替换 com.package.name 为目标应用的包名，您可在应用界面长按复制。

### 通过 adb 取消隐藏应用

```shell
adb shell pm unhide com.package.name
```

已 root 设备：

```shell
adb shell su -c pm unhide com.package.name
```

### 通过 adb 启用应用

```shell
adb shell pm enable com.package.name
```

已 root 设备：

```shell
adb shell su -c pm enable com.package.name
```

### 通过提供文件管理功能的恢复模式 (recovery)

访问`/data/system/users/0/package-restrictions.xml`，此文件存储了应用限制相关信息。您可修改、重命名或直接删除此文件。

- 取消隐藏应用：修改`hidden`属性为 true 的值为 false

- 启用应用：修改`enabled`属性为 2 (DISABLED) 或 3 (DISABLED_USER) 的值为 1 (ENABLED)

### 通过恢复模式清除数据 (wipe data)

**希望您能谨慎地选择冻结应用，以免陷入此窘境。**

## API

替换 com.package.name 为目标应用的包名，您可在应用界面长按复制。

Java

```java
public class MainActivity extends AppCompatActivity {
    private void launchApp() {
        try {
            Intent intent = new Intent();
            intent.setAction("com.aistra.hail.action.LAUNCH");
            intent.putExtra("package", "com.package.name");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Hail not installed", Toast.LENGTH_SHORT).show();
        }
    }
}
```

Kotlin

```kotlin
class MainActivity : AppCompatActivity() {
    private fun launchApp() {
        try {
            val intent = Intent()
            intent.setAction("com.aistra.hail.action.LAUNCH")
            intent.putExtra("package", "com.package.name")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Hail not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
```

`action`可为：

- `com.aistra.hail.action.LAUNCH`：解冻并启动目标应用。应用未冻结时会直接启动。

- `com.aistra.hail.action.FREEZE`：冻结目标应用。应用需处于首页。

- `com.aistra.hail.action.UNFREEZE`：解冻目标应用。

- `com.aistra.hail.action.FREEZE_ALL`：冻结首页全部应用。无需`extra`。

- `com.aistra.hail.action.UNFREEZE_ALL`：解冻首页全部应用。无需`extra`。

- `com.aistra.hail.action.LOCK`：锁定屏幕。无需`extra`。

- `com.aistra.hail.action.LOCK_FREEZE`：冻结首页全部应用并锁定屏幕。无需`extra`。

## Help Translate

Translate `app/src/main/res/values/strings.xml` and put it in the corresponding path.

or

1. Create an issue about which language you want to translate into.

2. We will create a string resource file in the corresponding path.

3. Translate it and create a pull request.

Thank you!

- 日本語 [@AokiFuru](https://github.com/AokiFuru)
- русский [@tommynok](https://github.com/tommynok)
- Español [@cyanwolfg](https://github.com/cyanwolfg)

## License

    Hail - Freeze Android apps
    Copyright (C) 2021-2022 Aistra

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.