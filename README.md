简体中文 | [English](README_EN.md)

# 雹 Hail

[![Android CI status](https://github.com/aistra0528/Hail/workflows/Android%20CI/badge.svg)](https://github.com/aistra0528/Hail/actions)
[![Downloads](https://img.shields.io/github/downloads/aistra0528/Hail/total.svg)](https://github.com/aistra0528/Hail/releases)
[![License](https://img.shields.io/github/license/aistra0528/Hail)](LICENSE)

雹是一款用于冻结 Android 应用的自由软件，您可自由享用所有功能。

[<img src="coolapk-badge.png" alt="Get it on CoolApk" height="80">](https://www.coolapk.com/apk/com.aistra.hail)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.aistra.hail/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.aistra.hail)

酷安版签名与 [GitHub Releases](https://github.com/aistra0528/Hail/releases) 一致，F-Droid 版由 F-Droid 签名。

Google Play 版为 [@purofle](https://github.com/purofle) 的 [Fork](https://github.com/purofle/Hail)，由
Google 签名。

<img src="fastlane/metadata/android/zh-CN/images/phoneScreenshots/1.png" width="32%" /> <img src="fastlane/metadata/android/zh-CN/images/phoneScreenshots/2.png" width="32%" /> <img src="fastlane/metadata/android/zh-CN/images/phoneScreenshots/3.png" width="32%" />

## 冻结

冻结`freeze`是一个营销用语，用于描述使**应用在用户不需要时不可运行**的行为，以此控制设备使用、减少内存占用和节省电量。用户可在需要时解冻`unfreeze`应用。

在一般情况下，“冻结”是指停用，此外雹也可以通过隐藏和暂停来“冻结”应用。

### 停用

被停用`disable`的应用不会出现在启动器中。在已安装应用列表中会显示已停用`disabled`状态。启用`enable`应用即可恢复。

### 隐藏

被隐藏`hide`的应用不会出现在启动器和已安装应用列表中。取消隐藏`unhide`应用即可恢复。

> 在这种状态下，软件包几乎处于卸载状态，无法使用，但并没有删除数据或实际的软件包文件。

### 暂停

被暂停`suspend`的应用在启动器中会显示为灰度图标。取消暂停`unsuspend`应用即可恢复。

> 在这种状态下，应用程序的通知将被隐藏，任何启动活动将被停止，不能弹出提示、对话框或播放音频。
> 当用户试图启动一个暂停的应用程序时，系统将向用户显示一个对话框，告知他们在暂停状态下不能使用这个应用程序。

## 工作模式

雹支持以`设备所有者`、`超级用户` (Root) 和`Shizuku` (含 Sui) 模式工作。

**冻结的应用需要通过相同工作模式解冻。**

1. 如果您的设备支持无线调试或已 root，推荐选择`Shizuku`。[关于 Shizuku](https://github.com/RikkaApps/Shizuku)

2. 如果您的设备已 root，可选择`超级用户`。**此模式速度相对较慢。**

3. 如果您的设备不支持无线调试且未 root，可选择`设备所有者`。**此模式设置较为繁琐。**

### 设备所有者 - 隐藏 / 暂停

此模式通过将雹设置为设备所有者 (Device Owner)，调用：

- `DevicePolicyManager.setApplicationHidden`方法隐藏应用。

- `DevicePolicyManager.setPackagesSuspended`方法暂停应用。

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

### 超级用户 - 停用 / 暂停

此模式通过授予雹超级用户 (Superuser) 权限，执行：

- `pm disable`命令停用应用。

- `pm suspend`命令暂停应用。

### Shizuku - 停用 / 隐藏 / 暂停

此模式通过 Shizuku 调用非 SDK 接口：

- `IPackageManager.setApplicationEnabledSetting`方法停用应用。

- `IPackageManager.setApplicationHiddenSettingAsUser`方法隐藏应用。

- `IPackageManager.setPackagesSuspendedAsUser`方法暂停应用。

## 恢复

### 通过 adb (可能需要 root)

替换 com.package.name 为目标应用的包名。

```shell
# 启用应用
adb shell pm enable com.package.name
# 取消隐藏应用
adb shell pm unhide com.package.name
# 取消暂停应用
adb shell pm unsuspend com.package.name
```

### 修改文件

访问`/data/system/users/0/package-restrictions.xml`，此文件存储了应用限制相关信息。您可修改、重命名或直接删除此文件。

- 启用应用：修改`enabled`属性为 2 (DISABLED) 或 3 (DISABLED_USER) 的值为 1 (ENABLED)

- 取消隐藏应用：修改`hidden`属性为 true 的值为 false

- 取消暂停应用：修改`suspended`属性为 true 的值为 false

### 通过恢复模式清除数据 (wipe data)

**希望您能谨慎地选择冻结应用，以免陷入此窘境。**

## API

Java

```java
public class MainActivity extends AppCompatActivity {
    private void hailAction(String action, String name, String value) {
        try {
            Intent intent = new Intent();
            intent.setAction(action);
            intent.putExtra(name, value);
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
    private fun hailAction(action: String, name: String, value: String) {
        try {
            val intent = Intent()
            intent.setAction(action)
            intent.putExtra(name, value)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Hail not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
```

`action`可为：

- `com.aistra.hail.action.LAUNCH`：解冻并启动目标应用。应用未冻结时会直接启动。`name="package"` `value="com.package.name"`

- `com.aistra.hail.action.FREEZE`：冻结目标应用。应用需处于首页。`name="package"` `value="com.package.name"`

- `com.aistra.hail.action.UNFREEZE`：解冻目标应用。`name="package"` `value="com.package.name"`

- `com.aistra.hail.action.FREEZE_TAG`：冻结目标标签中的全部非白名单应用。`name="tag"` `value="标签名"`

- `com.aistra.hail.action.UNFREEZE_TAG`：解冻目标标签中的全部应用。`name="tag"` `value="标签名"`

- `com.aistra.hail.action.FREEZE_ALL`：冻结首页全部应用。无需`extra`。

- `com.aistra.hail.action.UNFREEZE_ALL`：解冻首页全部应用。无需`extra`。

- `com.aistra.hail.action.FREEZE_NON_WHITELISTED`：冻结首页全部非白名单应用。无需`extra`。

- `com.aistra.hail.action.LOCK`：锁定屏幕。无需`extra`。

- `com.aistra.hail.action.LOCK_FREEZE`：冻结首页全部应用并锁定屏幕。无需`extra`。

## 协助翻译

翻译 `app/src/main/res/values/strings.xml` 并放在相应语言的路径中。

或

1. 创建一个你想翻译成哪种语言的问题。

2. 我们将在相应语言的路径中创建一个字符串资源文件。

3. 翻译它并创建一个拉取请求。

谢谢你啦！

- 繁體中文 [@cracky5322](https://github.com/cracky5322)
- 日本語 [@AokiFuru](https://github.com/AokiFuru) [@404potato](https://github.com/404potato)
- Русский [@tommynok](https://github.com/tommynok)
- Español [@cyanwolfg](https://github.com/cyanwolfg)
- Deutsch [@Enkidu70](https://github.com/Enkidu70)
- Українська [@Operator404](https://github.com/Operator404)

## 许可证

    Hail - Freeze Android apps
    Copyright (C) 2021-2023 Aistra

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
