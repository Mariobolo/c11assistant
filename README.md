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
- `app/src/main/java/com/zerorun/c11assistant/ui/MainActivity.java`
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
  - 执行动作统一出口（当前包含基础动作实现）

### 2.5 模型（Model）
- `ScreenConfig`
  - 屏幕配置模型
  - 提供 `fromJson(JSONObject)` 反序列化能力
- `ActionItem`
  - 动作项模型

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

### 3.4 副屏能力
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
