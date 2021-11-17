# 雹 Hail

雹是一款用于冻结 Android 应用的自由软件，点按启动应用，长按冻结/解冻。

## 冻结

“冻结”（Freeze）是一个营销用语，用于描述使**应用在用户不需要时不可运行**的行为，以此减少内存占用并节省电量。用户可在需要时“解冻”（Unfreeze）应用。通过“停用”和“隐藏”两种方式，都能达到所谓“冻结”的效果，但两者之间也存在些许差异。

### 停用

被“停用”（Disable）的应用不可使用，也不会出现在启动器中。在已安装应用列表中应用会显示“已停用”（Disabled）状态。“启用”（Enable）应用即可恢复。

### 隐藏

被“隐藏”（Hide）的应用不可使用，也不会出现在启动器和已安装应用列表中。“取消隐藏”（Unhide）应用即可恢复，但**已授予应用的权限会被重置，需要重新授予**。

## 运行模式

雹支持以`超级用户 - 停用`和`设备所有者 - 隐藏`模式运行。

### 超级用户 - 停用

此模式通过授予雹“超级用户”（Superuser）权限，执行`pm disable`命令停用应用。

### 设备所有者 - 隐藏

此模式通过将雹设置为“设备所有者”（Device Owner），调用`DevicePolicyManager`的`setApplicationHidden`方法隐藏应用。

**设置为设备所有者的应用需要移除设备所有者后方可卸载。**

#### 通过 ADB 将雹设置为设备所有者

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

#### 移除雹的设备所有者

在雹的“应用”页面点按雹，在弹出的选项中选择“卸载”。

## 待办

雹还处于早期开发阶段，我们计划通过更新提升其易用性：

1. “设置”页面更多设置
2. “首页”页面更多选项
3. 启动器快捷方式