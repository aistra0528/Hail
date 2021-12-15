[简体中文](README.md) | English

**English is not my native language. Help us translate README**

# Hail 雹

Hail is a free software to freeze Android apps. Enjoy all features freely!

## Freeze

Freeze is a word to describe the action of ** forbid apps when they are unnecessary** to use device
in a better way, cut down the usage of ram and save power. User can unfreeze it to revert.

There are two ways to "freeze" apps, hide and disable.

### Hide

Hidden apps will not shown in launcher and installed app list. Unhide them to revert.

### Disable

Disable apps will not shown in launcher. Enable them to revert.

## Working mode

Hail can work with `Device Owner - Hide`, `Superuser - Disable` and `Shizuku - Disable`.

**The way of hide and disable are different, unfreeze app request the same way.**

1. For devices support wifi adb or rooted, `Shizuku - Disable` is
   recommend. [About Shizuku](https://github.com/RikkaApps/Shizuku)

2. For rooted devices, `Superuser - Disable` is alternative. **It is slower.**

3. Select `Device Owner - Hide` otherwise. **It is unstable on some devices.**

### Device Owner - Hide

This mode invoke `DevicePolicyManager.setApplicationHidden` to hide apps.

**You must remove device owner before uninstall**

#### Set device owner by adb

[Android Debug Bridge (adb) Guide](https://developer.android.com/studio/command-line/adb)

[Download Android SDK Platform-Tools](https://developer.android.com/studio/releases/platform-tools)

Issue adb command:

```shell
adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver
```

A message will shown if it has been successfully set:

```
Success: Device owner set to package com.aistra.hail
Active admin set to component {com.aistra.hail/com.aistra.hail.receiver.DeviceAdminReceiver}
```

Search the message by search engine otherwise.

#### Remove device owner

Click Hail at Apps, then select Uninstall in options.

### Superuser - Disable

This mode execute `pm disable-user` to disable apps.

### Shizuku - Disable

This mode invoke non-SDK interface `IPackageManager.setApplicationEnabledSetting` to disable apps.

## Revert

Replace com.package.name to the package name of target app, where you can copy it by long click at
Apps.

### Unhide app by adb

```shell
adb shell pm unhide com.package.name
```

For rooted devices:

```shell
adb shell su -c pm unhide com.package.name
```

### Enable app by adb

```shell
adb shell pm enable com.package.name
```

For rooted devices:

```shell
adb shell su -c pm enable com.package.name
```

### Modify file by recovery

Access `/data/system/users/0/package-restrictions.xml`, this file stores the restrictions about
apps. You can modify, rename or just delete it.

- Unhide app: Modify the value of `hidden` from true to false

- Enable app: Modify the value of `enabled` from 2 (DISABLED) or 3 (DISABLED_USER) to 1 (ENABLED)

### Wipe data by recovery

**None of my business :(**

## API

Replace com.package.name to the package name of target app, where you can copy it by long click at
Apps.

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

`action` can be one of the following constants:

- `com.aistra.hail.action.LAUNCH`: Unfreeze and launch target app. If it is unfrozen, it will launch
  directly.

- `com.aistra.hail.action.FREEZE`: Freeze target app. It must be checked at Home.

- `com.aistra.hail.action.UNFREEZE`: Unfreeze target app.

- `com.aistra.hail.action.FREEZE_ALL`: Freeze all apps at Home. `extra` is not necessary.

- `com.aistra.hail.action.UNFREEZE_ALL`: Unfreeze all apps at Home. `extra` is not necessary.

- `com.aistra.hail.action.LOCK`: Lock screen. `extra` is not necessary.

- `com.aistra.hail.action.LOCK_FREEZE`: Freeze all apps at Home and lock screen. `extra` is not
  necessary.

## Help Translate

Translate `app/src/main/res/values/strings.xml` and put it in the corresponding path.

or

1. Create an issue about which language you want to translate into.

2. We will create a string resource file in the corresponding path.

3. Translate it and create a pull request.

Thank you!

## License

    Hail - Freeze Android apps
    Copyright (C) 2021 AxAistra

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