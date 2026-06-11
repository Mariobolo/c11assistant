# C11 Assistant（零跑 C11 车机助手）

## 1. 项目概述
C11 Assistant 是面向零跑 C11 车机场景的 Android 9（API 28）辅助应用，提供基础 UI、前台守护服务、Logcat 事件监控、自动化联动、悬浮球操作和副屏启动能力。

- 应用包名：`com.leapmotor.c11assistant`
- 目标系统：Android 9.0 / API 28
- 构建体系：Gradle 8.2 + JDK 17（代码兼容 Java 8 语法级别）
- 依赖体系：Android Support 28.0.0（不使用 AndroidX）

---

## 2. 当前代码结构

### 2.1 核心入口
- `app/src/main/java/com/leapmotor/c11assistant/ui/MainActivity.java`
  - 主界面（快捷动作 / 设置 / 关于 / 帮助）
  - 应用启动时拉起 `LogcatMonitorService`
  - 根据开关拉起 `FloatBallService`
  - 快捷动作支持副屏桌面启动

### 2.2 服务（Service）
- `C11ForegroundService`
  - 前台常驻服务
  - 负责基础后台守护流程
- `LogcatMonitorService`
  - 前台日志监控服务
  - 过滤指定车机标签并解析事件
  - 发布自动化事件广播
- `FloatBallService`
  - 前台悬浮球服务
  - 支持拖动、吸边、菜单快捷动作
- `C11AccessibilityService`
  - 无障碍服务入口（预留车机场景能力）

### 2.3 广播接收器（Receiver）
- `BootReceiver`
  - 监听 `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED`
  - 根据 `auto_start` 开关启动关键服务
- `AutomationEventReceiver`
  - 接收 `com.leapmotor.c11assistant.EVENT_TRIGGERED`
  - 转发给 `AutomationManager` 执行规则

### 2.4 管理器（Manager）
- `AutomationManager`
  - 自动化规则中心
  - 处理转向灯、锁车、360按键等规则
- `MultiScreenManager`
  - 多屏/副屏启动管理
  - 使用 `ActivityOptions#setLaunchDisplayId` 启动到副屏
- `ConfigManager`
  - 读取与持久化配置 JSON
- `SharedPreferencesUtils`
  - 统一开关与参数存储
- `ActionExecutor`
  - 执行动作统一出口
- `TaskManager`
  - 自定义任务管理器
  - 负责任务的加载、保存、匹配和执行
- `C11CarControlManager`
  - 零跑C11官方车控接口封装
- `TtsManager`
  - 讯飞TTS语音播报管理器
- `CarEventProcessor`
  - 车辆事件处理器
- `LogCollector`
  - 日志收集器（待实现）

### 2.5 模型（Model）
- `ScreenConfig`
  - 屏幕配置模型
- `ActionItem`
  - 动作项模型
- `CarEvent`
  - 车辆事件枚举（支持30+种车辆状态）
- `Task`
  - 自定义任务数据模型
- `Trigger`
  - 触发条件模型（支持日志事件、属性变化、时间、应用状态）
- `Action`
  - 动作模型（支持语音、系统控制、启动应用等）

### 2.6 用户界面（UI）
- `TaskListActivity`
  - 自定义任务列表界面，支持新建、编辑、删除、复制、导入、导出任务
- `TaskEditorActivity`
  - 可视化任务编辑界面，支持配置触发条件和动作序列
- `TaskAdapter`
  - 任务列表适配器
- `LogViewerActivity`（待实现）
  - 日志查看和调试界面
- `PermissionGuideActivity`（待实现）
  - 权限引导界面

---

## 3. 关键功能说明

### 3.1 Logcat 实时监控
`LogcatMonitorService` 仅监听以下标签：
- `C11CarSomeIp:D`
- `C11AirConditioner:D`
- `C11CarXml:D`
- `AroundService:I`
- `BleControlService:D`
- `TripService:I`

并识别以下事件（含 500ms 防抖）：
- 档位：`GEAR_R / GEAR_N / GEAR_D`
- 方向盘 360：`WHEEL_360`
- 锁止：`LOCK / UNLOCK`
- 转向灯：`TURN_LEFT_ON/OFF`、`TURN_RIGHT_ON/OFF`
- 车门：`DOOR_9123` ~ `DOOR_9128`

事件通过广播动作下发：
- `com.leapmotor.c11assistant.EVENT_TRIGGERED`

### 3.2 自动化联动
`AutomationManager` 当前内置规则：
- 转向灯开启 -> 启动 360（`com.leapmotor.aroundview`）
- 转向灯关闭 -> 返回之前应用（按当前记录执行）
- 上锁 -> 执行 `CHILD_LOCK_ON`
- 解锁 -> 执行 `CHILD_LOCK_OFF`
- 方向盘 360 按键 -> 执行 `AROUND_TOGGLE_VIEW`

规则开关通过 `SharedPreferencesUtils` 管理。

### 3.3 悬浮球
`FloatBallService` 提供：
- 可拖动圆形悬浮球
- 自动吸附屏幕边缘
- 点击展开菜单：
  - 返回桌面
  - 返回上一页
  - 打开副屏桌面
  - 打开 360 全景
  - 打开设置

### 3.4 自定义任务系统
`TaskManager` 提供自定义自动化任务功能：
- **触发条件类型**：
  - 日志事件触发（如档位变化、车门状态、转向灯等）
  - 系统属性变化触发（如车辆上锁、屏幕熄灭）
  - 时间触发（如每天固定时间、定时任务）
  - 应用启动/关闭触发
- **动作类型**：
  - 语音播报（TTS）
  - 系统属性设置（温度、灯光、氛围灯等）
  - 发送系统广播（打开近光灯、切换驾驶模式）
  - 启动应用（360环视、高德地图）
  - UI交互（点击位置、手势操作）
  - 延迟执行（100ms-10000ms）
- **任务管理**：
  - 支持启用/禁用任务
  - 支持任务优先级设置
  - 支持防抖时间配置
  - 支持任务导入/导出
- **持久化**：
  - 任务以JSON格式保存到 `/sdcard/C11Assistant/tasks/`
  - 应用启动时自动加载所有任务

### 3.5 副屏能力
`MultiScreenManager` 提供：
- 副屏 displayId 探测
- 指定应用拉起到副屏
- 普通应用拉起能力

---

## 4. 权限与清单声明
`AndroidManifest.xml` 已声明关键权限：
- `RECEIVE_BOOT_COMPLETED`
- `FOREGROUND_SERVICE`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `SYSTEM_ALERT_WINDOW`
- `QUERY_ALL_PACKAGES`

并注册核心组件：
- `MainActivity`
- `BootReceiver`
- `AutomationEventReceiver`
- `C11ForegroundService`
- `LogcatMonitorService`
- `FloatBallService`
- `C11AccessibilityService`

---

## 5. 构建配置

### 5.1 app/build.gradle
- `compileSdk 28`
- `minSdk 28`
- `targetSdk 28`
- Java 编译级别：`VERSION_1_8`
- Support 依赖：
  - `appcompat-v7:28.0.0`
  - `recyclerview-v7:28.0.0`
  - `design:28.0.0`
  - `constraint-layout:1.1.3`
  - `gridlayout-v7:28.0.0`

### 5.2 gradle.properties
- `android.useAndroidX=false`
- `android.enableJetifier=false`

---

## 6. 本地编译命令
```bash
./gradlew clean --no-daemon assembleDebug
```

若本地环境提示 Android SDK 路径问题，请在项目根目录配置 `local.properties`：
```properties
sdk.dir=/path/to/Android/Sdk
```

---

## 7. 实车配置指南（同步维护）

### 7.1 首次安装
1. 使用 `adb install` 或文件管理器安装 APK。
2. 首次进入应用后，确认主页底部反馈栏出现“系统就绪，等待操作”。
3. 进入“设置”页，按需开启“开机自动启动服务”。

### 7.2 必配权限
1. 页面路径：**快捷动作 → 开启悬浮窗权限**。
2. 在系统页面允许“悬浮窗/在其他应用上层显示”。
3. 建议在车机系统设置中将本应用加入后台保活/电池优化白名单。

### 7.3 常见故障排查
- 悬浮球不显示：检查“设置 → 启用悬浮球”是否开启，并重新授予悬浮窗权限。
- 自动化无响应：先点“快捷动作 → 启动后台服务”，再检查日志关键字是否与当前 ROM 匹配。
- 开机未自动启动：检查“设置 → 开机自动启动服务”开关与车机自启动白名单。

### 7.4 文案与页面路径对齐清单
- 标签页：`快捷动作` / `设置` / `关于` / `帮助`。
- 快捷动作页按钮：`启动后台服务`、`执行主屏任务`、`执行副屏任务`、`开启悬浮窗权限`、`导出配置`、`导入配置`。
- 设置页开关：`开机自动启动服务`（key: `auto_start`）、`显示操作反馈提示`（key: `show_feedback`）、`启用悬浮球`（key: `float_ball`）、`启用侧边手势`（key: `edge_gesture`）。
- 帮助页分区：`安装指南`、`权限设置`、`常见故障`。

---

## 8. 运行验证建议（车机）
1. 安装 APK 到零跑 C11 设备
2. 打开应用并授予悬浮窗权限
3. 验证悬浮球显示、拖动、吸边、菜单动作
4. 验证 Logcat 事件触发与自动化规则执行
5. 验证副屏快捷动作启动
6. 重启设备验证开机自启流程

---

## 9. 注意事项
- 项目固定为 Android Support 体系，不可混用 AndroidX。
- 车机 ROM 对后台与权限策略可能有定制，建议在实车中完成权限白名单配置。
- 自动化规则依赖车机日志格式，若系统升级导致日志字段变化，需要同步更新匹配规则。

---

## 10. 动作执行系统

`ActionExecutor` 现在提供统一的 `executeWithResult(Context, action, payload)` 入口，返回包含成功状态、动作名、状态码和原因的 `ActionResult`。已实现的主要动作：

- 系统控制：`GLOBAL_BACK`、`GLOBAL_HOME`、`GLOBAL_RECENTS`、`GLOBAL_POWER`
- 音量/媒体：`VOLUME_UP`、`VOLUME_DOWN`、`VOLUME_MUTE`、`MEDIA_NEXT`、`MEDIA_PREVIOUS`、`MEDIA_PLAY_PAUSE`
- 亮度：`BRIGHTNESS_UP`、`BRIGHTNESS_DOWN`（无 `WRITE_SETTINGS` 时会跳转授权页）
- 应用：`LAUNCH_PACKAGE`、`LAUNCH_ON_DISPLAY`、`LAUNCH_FREEFORM`、`BRING_TO_FRONT`
- 车机场景：`CHILD_LOCK_ON`、`CHILD_LOCK_OFF`、`AROUND_TOGGLE_VIEW`、`OPEN_SETTINGS`、`OPEN_MUSIC`、`OPEN_NAVIGATION`
- 高级交互：`CLICK_AT_POSITION`、`SWIPE_GESTURE`、`LONG_PRESS`、`INPUT_TEXT`
- 降级/扩展：`BROADCAST`、`SHELL`、`ROOT_SHELL`

无障碍服务可用时优先通过无障碍全局动作与手势执行；不可用时，部分动作会降级到 `input keyevent/tap/swipe/text` shell 命令。车机深度指令默认以广播/可配置 shell 命令形式接入，实际 ROM 若需要私有服务或系统签名接口，可在 JSON 中覆盖 `broadcastAction`、`targetPackage`、`command` 或 `payload`。

## 11. 配置驱动示例

`c11_config.json` 可使用 `screens[].actions` 或顶层 `actions` 定义动作序列，`automationRules` 定义事件触发规则：

```json
{
  "screens": [
    {
      "displayId": -1,
      "label": "副屏",
      "actions": [
        { "id": "打开高德", "type": "LAUNCH_ON_DISPLAY", "packageName": "com.autonavi.minimap", "delayMs": 300 },
        { "id": "点击搜索", "type": "CLICK_AT_POSITION", "x": 120, "y": 80, "retryCount": 1 },
        { "id": "输入目的地", "type": "INPUT_TEXT", "text": "回家", "delayMs": 200 },
        { "id": "上滑列表", "type": "SWIPE_GESTURE", "startX": 900, "startY": 520, "endX": 900, "endY": 180, "durationMs": 450 }
      ]
    }
  ],
  "automationRules": [
    {
      "id": "reverse_open_around",
      "enabled": true,
      "event": "GEAR_R",
      "priority": 100,
      "conflictGroup": "camera",
      "debounceMs": 800,
      "actions": [
        { "id": "open_360", "type": "AROUND_TOGGLE_VIEW", "retryCount": 1 }
      ]
    }
  ]
}
```

动作通用字段：

- `enabled`：是否启用
- `delayMs`：执行前延迟
- `retryCount` / `retryDelayMs`：失败重试
- `stopOnFailure`：序列失败时停止后续动作
- `conditionScene` / `conditionPackage`：仅当当前无障碍识别场景或包名匹配时执行
- `payload`：任意 JSON，会合并到动作 payload 中

## 12. 零跑 C11 官方接口接入

本项目新增 `C11CarControlManager` 统一封装零跑 C11 官方原生接口，优先使用无需 root 的两类接口：

1. `Settings.Global` 属性：例如 `C11_MUSIC`、`C11_NAVI`、`strCar1409`、`strCar1800`。
2. 零跑 speech/IVI 广播：例如 `com.leapmotor.speech.toairconditioner`、`com.leapmotor.speech.tocarcontrol`、`com.leapmotor.speech.tosettings`。

> 注意：这些接口虽然无需 root，但部分 ROM 仍要求系统签名、`WRITE_SECURE_SETTINGS` 或零跑专属广播权限。普通第三方 APK 如权限不足，`ActionResult` 会返回失败原因；请在系统签名包或授权环境中验证。

### 12.1 常用动作类型

- 全局属性：`SET_GLOBAL_INT` / `GET_GLOBAL_INT`
- 音量：`SET_C11_MUSIC_VOLUME`、`SET_C11_NAVI_VOLUME`、`SET_C11_CALL_VOLUME`
- 空调：`HVAC_PANEL_ON/OFF`、`SET_HVAC_DRIVER_TEMP`、`SET_HVAC_PASSENGER_TEMP`、`HVAC_AC_MAX_ON/OFF`
- 氛围灯：`AMBIENT_LIGHT_ON/OFF`、`SET_AMBIENT_COLOR`
- 设置：`DAY_MODE`、`NIGHT_MODE`、`WIFI_ON/OFF`、`BLUETOOTH_ON/OFF`
- 灯光：`LOW_BEAM_ON/OFF`、`REAR_FOG_ON/OFF`、`POSITION_LIGHT_ON/OFF`、`PEDESTRIANS_ALERT_ON/OFF`
- 驾驶/场景：`SET_DRIVER_MODE`、`GUARD_MODE_ON/OFF`、`REST_MODE_ON/OFF`、`CAMPING_MODE_ON/OFF`、`POWER_SAVE_MODE_ON/OFF`、`SENTINEL_MODE_ON/OFF`
- 信息页：`JOURNEY_ENERGY`、`VEHICLE_HEALTH`
- 原始官方广播：`OFFICIAL_BROADCAST`

### 12.2 JSON 示例

```json
{
  "actions": [
    { "id": "主驾24度", "type": "SET_HVAC_DRIVER_TEMP", "value": 24 },
    { "id": "最大制冷", "type": "HVAC_AC_MAX_ON" },
    { "id": "运动模式", "type": "SET_DRIVER_MODE", "mode": 1 },
    { "id": "蓝色氛围灯", "type": "SET_AMBIENT_COLOR", "value": 14 },
    {
      "id": "自定义官方广播",
      "type": "OFFICIAL_BROADCAST",
      "broadcastAction": "com.leapmotor.speech.tocarcontrol",
      "payload": {
        "extras": { "REST_MODE": 1 }
      }
    }
  ]
}
```

### 12.3 ADB 快速验证

```bash
adb shell settings put global C11_NAVI 60
adb shell settings put global strCar1409 24
adb shell am broadcast -a com.leapmotor.speech.toairconditioner --ei HVACACMAXREQ 1
adb shell am broadcast -a com.leapmotor.speech.tocarcontrol --ei MMI_DRIVER_MODE_SET 4
adb shell am broadcast -a com.leapmotor.speech.tosettings --ei bluetooth 1
```

### 12.4 童锁控制接口

童锁控制使用讯飞语音服务的官方广播接口：

```bash
# 开启童锁
adb shell am broadcast -a com.iflytek.autofly.handMessage -p com.leapmotor.leapmotoriflyspeechservice --es value '{"semantic":{"name":"儿童锁","operation":"OPEN","service":"CAR_CONTROL"},"focus":"carControl","messageType":"REQUEST","needResponse":"YES","operationApp":"speech","protocolId":0,"requestCode":"10039","statusCode":0,"version":"v1.0"}'

# 关闭童锁
adb shell am broadcast -a com.iflytek.autofly.handMessage -p com.leapmotor.leapmotoriflyspeechservice --es value '{"semantic":{"name":"儿童锁","operation":"CLOSE","service":"CAR_CONTROL"},"focus":"carControl","messageType":"REQUEST","needResponse":"YES","operationApp":"speech","protocolId":0,"requestCode":"10039","statusCode":0,"version":"v1.0"}'
```

`CHILD_LOCK_ON/OFF` 动作现在通过 `C11CarControlManager.setChildLock()` 实现，使用上述官方广播接口。
