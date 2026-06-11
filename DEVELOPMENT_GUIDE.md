# 零跑C11车机自动化助手 - 开发指南

## 项目架构概述

### 目录结构

```
app/src/main/java/com/leapmotor/c11assistant/
├── manager/                    # 核心管理器层
│   ├── ActionExecutor.java    # 动作执行引擎
│   ├── ActionSequenceExecutor.java # 动作序列执行器
│   ├── AutomationManager.java # 自动化规则管理
│   ├── CarControlManager.java # 官方车控接口封装
│   ├── CarEventProcessor.java # 车辆事件处理器
│   ├── ConfigManager.java     # 配置文件管理
│   ├── LogCollector.java      # 日志收集器 (新增)
│   ├── MultiScreenManager.java # 多屏管理
│   ├── SharedPreferencesUtils.java # 偏好设置工具
│   ├── TaskManager.java       # 自定义任务管理器
│   └── TtsManager.java        # 讯飞TTS管理器
├── model/                     # 数据模型层
│   ├── Action.java            # 动作数据模型
│   ├── ActionItem.java        # 动作项模型
│   ├── BuiltInRule.java       # 内置规则数据模型 (新增)
│   ├── CarEvent.java          # 车辆事件枚举
│   ├── LogEntry.java          # 日志条目模型 (新增)
│   ├── ScreenConfig.java      # 屏幕配置
│   ├── Task.java              # 自定义任务数据模型
│   └── Trigger.java           # 触发条件数据模型
├── service/                   # 服务层
│   ├── C11AccessibilityService.java # 无障碍服务
│   ├── C11ForegroundService.java    # 前台服务保活
│   ├── FloatBallService.java         # 悬浮球服务
│   └── LogcatMonitorService.java     # Logcat监听服务
├── receiver/                  # 广播接收层
│   ├── AutomationEventReceiver.java # 自动化事件接收器
│   └── BootReceiver.java      # 开机自启动接收器
└── ui/                       # 用户界面层
    ├── ActionAdapter.java     # 动作列表适配器
    ├── BuiltInRuleAdapter.java # 内置规则列表适配器 (新增)
    ├── LogAdapter.java        # 日志列表适配器 (新增)
    ├── MainActivity.java      # 主界面
    ├── PermissionGuideActivity.java # 权限引导界面 (新增)
    ├── TaskAdapter.java       # 任务列表适配器 (新增)
    ├── TaskEditorActivity.java # 任务编辑界面 (新增)
    ├── TaskListActivity.java  # 任务列表界面 (新增)
    └── LogViewerActivity.java # 日志查看界面 (新增)
```

## 核心模块说明

### 1. 车辆事件系统 (CarEvent)

**位置**: `model/CarEvent.java`

**功能**: 定义所有可监听的车辆状态事件及其映射关系

**使用示例**:

```java
// 枚举所有支持的事件
CarEvent[] allEvents = CarEvent.values();

// 用日志行匹配事件
String logLine = "C11CarSomeIp: eventId: 1110 value: 3";
CarEvent event = CarEvent.matchLogLine(logLine);
if (event != null) {
    String eventId = event.getEventId();
    String displayName = event.getDisplayName();
}

// 获取所有需要监听的日志标签
String[] logTags = CarEvent.getAllLogTags();
```

### 2. 讯飞TTS管理器 (TtsManager)

**位置**: `manager/TtsManager.java`

**功能**: 封装讯飞TTS接口，提供语音播报功能

**使用示例**:

```java
// 获取实例
TtsManager tts = TtsManager.get(context);

// 基本语音播报
tts.speak("你好，零跑");

// 条件播报（根据偏好设置）
tts.speakIfEnabled("倒车请注意", "rule_tts_gear_r", true);

// 延迟播报
tts.speakWithDelay("欢迎上车", 1000);

// 禁用原车语音
tts.setMuteOriginalTts(true);
```

### 3. 车辆事件处理器 (CarEventProcessor)

**位置**: `manager/CarEventProcessor.java`

**功能**: 处理车辆事件，执行预设的自动化规则

**内置规则**:
- 挂入R挡 → 播报"倒车请注意"
- 挂入D挡 → 播报"起飞"
- 开启转向灯 → 打开360环视
- 锁车 → 开启童锁
- 解锁 → 关闭童锁
- 方控360按钮 → 打开360环视

**使用示例**:

```java
CarEventProcessor processor = CarEventProcessor.get(context);
processor.processEvent("GEAR_R", rawLogLine);
```

### 4. Logcat监听服务 (LogcatMonitorService)

**位置**: `service/LogcatMonitorService.java`

**增强功能**:
- 完整支持CarEvent枚举中的所有事件
- 智能事件匹配
- 防抖机制防止重复触发
- 自动重启机制防止服务挂掉

### 5. 自定义任务系统 (Custom Task System)

**位置**: 
- `model/Task.java` - 任务数据模型
- `model/Trigger.java` - 触发条件模型
- `model/Action.java` - 动作数据模型
- `manager/TaskManager.java` - 任务管理器

**功能**:
- 支持用户自定义自动化任务
- 可视化任务编辑（待实现UI）
- 多条件触发（任一/全部模式）
- 动作序列执行
- 任务持久化存储（JSON格式）
- 支持任务导入/导出

**核心组件**:

**Task（任务模型）**:
- 任务名称、描述、启用状态、优先级
- 触发条件列表（Trigger）
- 动作序列列表（Action）
- 防抖时间、执行次数限制

**Trigger（触发条件模型）**:
- 支持日志事件触发（TYPE_LOG_EVENT）
- 支持系统属性变化触发（TYPE_PROPERTY_CHANGE）
- 支持时间触发（TYPE_TIME）
- 支持应用状态变化触发（TYPE_APP_STATE）

**Action（动作模型）**:
- 支持语音播报（SPEECH_SPEAK）
- 支持启动应用（LAUNCH_PACKAGE）
- 支持设置系统属性（SET_GLOBAL_INT）
- 支持发送广播（OFFICIAL_BROADCAST）
- 支持360环视切换（AROUND_TOGGLE_VIEW）
- 支持延迟执行（DELAY）
- 支持点击位置（CLICK_AT_POSITION）

**使用示例**:

```java
// 获取TaskManager实例
TaskManager taskManager = TaskManager.get(context);

// 创建任务
Task task = new Task();
task.setName("D挡语音提示");
task.setDescription("挂入D挡时语音播报");

// 添加触发条件（挂入D挡）
Trigger trigger = Trigger.createLogEventTrigger(
        CarEvent.GEAR_D.getEventId(), "", "挂入D挡");
task.addTrigger(trigger);

// 添加动作（语音播报）
Action action = Action.createSpeechAction("出发！", "语音提示");
task.addAction(action);

// 保存任务
taskManager.addTask(task);

// 创建示例任务（用于测试）
taskManager.createSampleTasks();
```

**任务持久化**:
- 任务保存路径: `/sdcard/C11Assistant/tasks/{taskId}.json`
- 支持导入导出任务文件
- 应用启动时自动加载所有任务

## 开发任务列表

### 第一阶段：核心功能完善 (已完成 ✅)

- [x] 创建完整的车辆事件枚举 `CarEvent.java`
- [x] 实现讯飞TTS语音播报管理器 `TtsManager.java`
- [x] 实现车辆事件处理器 `CarEventProcessor.java`
- [x] 增强Logcat监听服务支持所有事件
- [x] 修正包名问题，统一为 `com.leapmotor.c11assistant`

### 第二阶段：功能增强 (进行中 ⏳)

- [x] 完善 ActionExecutor 中的 SPEECH_SPEAK 动作支持
- [x] 在 ActionExecutor 中集成 TtsManager
- [x] 实现自定义任务系统核心模块
  - [x] 创建任务数据模型 `Task.java`
  - [x] 创建触发条件模型 `Trigger.java`
  - [x] 创建动作模型 `Action.java`
  - [x] 实现任务管理器 `TaskManager.java`
  - [x] 集成任务执行引擎到 `CarEventProcessor`
- [x] 优化360环视逻辑，支持左右转向切换对应视角
- [x] 完善童锁控制逻辑（基于官方广播）
- [x] 修复代码问题：
  - [x] 修正 `ACTION_VOICE_WARM_TIP` 常量拼写错误 (aufofly → autofly)
  - [x] 实现 `CHILD_LOCK_ON/OFF` 动作的官方广播接口
  - [x] 修复 `shellEscapeInputText` 方法中的空格转义错误
  - [x] 修复 `getAllLogTags` 重复添加 TripService 问题
  - [x] 修复 `VEHICLE_UNLOCK/LOCK` eventId 大小写不一致问题
  - [x] `C11CarControlManager` 添加单例模式
- [x] 实现任务列表界面 `TaskListActivity.java`
- [x] 实现任务编辑界面 `TaskEditorActivity.java`
- [x] 实现任务导入/导出功能
- [ ] 添加日志查看和调试功能 (`LogCollector.java`, `LogViewerActivity.java`)
- [ ] 实现内置规则的可视化管理 (`BuiltInRule.java`)
- [ ] 实现权限引导界面 (`PermissionGuideActivity.java`)
- [ ] 应用保活与异常恢复优化
- [ ] 车机环境适配和性能优化

### 第三阶段：高级功能 (待开发)

- [ ] 实现上车自动开空调（根据温度传感器）
- [ ] 实现雨刮自动关窗联动
- [ ] 实现熄火自动关窗锁车（系统自带，延后）
- [ ] 添加OTA升级功能（延后）
- [ ] 实现数据统计功能

## 开发要点

### 1. 添加新的车辆事件

在 `CarEvent.java` 中添加新的枚举项：

```java
NEW_EVENT("NEW_EVENT", "新事件描述", "LogTag", "匹配字符串"),
```

### 2. 添加新的自动化规则

在 `CarEventProcessor.java` 的 `initBuiltInRules()` 方法中添加：

```java
addRule(CarEvent.NEW_EVENT.getEventId(), "rule_pref_key", true, 500,
    newEventAction("ACTION_TYPE", "payload"));
```

### 3. 实现新的动作类型

在 `ActionExecutor.java` 中添加新的动作处理：

```java
case "NEW_ACTION":
    // 执行新动作
    return ActionResult.ok(normalized, "success");
```

## 权限要求

### 必须的权限

```xml
<!-- 前台服务 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- 开机自启动 -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 系统设置 -->
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />

<!-- 悬浮窗 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 其他应用信息 -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### 需要用户授予的权限

1. **无障碍权限**: 用于全局手势、查找UI节点等
2. **Shizuku权限**: 用于执行需要更高权限的Shell命令（可选）
3. **读取日志权限**: 通过ADB授予 `android.permission.READ_LOGS`

## 编译和调试

### 基本编译

```bash
./gradlew assembleDebug
```

### 安装到车机

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 授予权限

```bash
# 授予读取日志权限（重要！）
adb shell pm grant com.leapmotor.c11assistant android.permission.READ_LOGS
```

### 查看应用日志

```bash
adb logcat -s LogcatMonitorService:V CarEventProcessor:V TtsManager:V AutomationManager:V *:E
```

## 常见问题

### Q: Logcat监听不工作？

A: 确保已授予READ_LOGS权限：
```bash
adb shell pm grant com.leapmotor.c11assistant android.permission.READ_LOGS
```

### Q: 语音播报没有声音？

A: 检查：
1. 车机音量是否正常
2. 讯飞语音服务是否正常运行
3. 查看 `TtsManager` 的日志输出

### Q: 如何调试事件匹配？

A: 查看Logcat日志，过滤标签：
```
LogcatMonitorService: 显示匹配到的事件
CarEventProcessor: 显示规则执行情况
```

## 扩展开发建议

### 1. 支持自定义语音提示文本

在MainActivity中添加界面，允许用户自定义每个事件的语音提示文本，通过SharedPreferences存储。

### 2. 实现规则优先级管理

支持多个规则匹配同一事件时的优先级控制。

### 3. 添加规则测试功能

在调试界面中可以手动触发事件测试规则是否正确执行。

## 注意事项

1. 必须使用Android Support Library，不能使用AndroidX
2. 目标API为28，不使用更高版本API
3. 保持代码简洁，避免过度依赖第三方库
4. 所有硬编码的配置项应支持通过SharedPreferences自定义
5. 车机环境可能有特殊限制，需要做好降级处理
