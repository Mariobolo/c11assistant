package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.leapmotor.c11assistant.model.Action;
import com.leapmotor.c11assistant.model.CarEvent;
import com.leapmotor.c11assistant.model.Task;
import com.leapmotor.c11assistant.model.Trigger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 自定义任务管理器
 * 负责任务的加载、保存、匹配和执行
 */
public class TaskManager {
    private static final String TAG = "TaskManager";

    private static TaskManager instance;
    private static final String TASKS_DIR = "C11Assistant/tasks";
    private static final String TASK_FILE_EXT = ".json";

    private Context appContext;
    private List<Task> tasks;
    private HandlerThread workThread;
    private Handler workHandler;

    /**
     * 单例模式
     */
    public static synchronized TaskManager get(Context context) {
        if (instance == null) {
            instance = new TaskManager(context.getApplicationContext());
        }
        return instance;
    }

    private TaskManager(Context context) {
        this.appContext = context;
        this.tasks = new ArrayList<>();
        initWorkThread();
        loadAllTasks();
    }

    /**
     * 初始化工作线程
     */
    private void initWorkThread() {
        workThread = new HandlerThread("TaskWorker");
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
    }

    // ==================== 任务持久化 ====================

    /**
     * 获取任务保存目录
     */
    private File getTasksDirectory() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dir = new File(appContext.getExternalFilesDir(null), TASKS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir;
        }
        // 降级到内部存储
        File dir = new File(appContext.getFilesDir(), TASKS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取任务文件
     */
    private File getTaskFile(String taskId) {
        return new File(getTasksDirectory(), taskId + TASK_FILE_EXT);
    }

    /**
     * 保存单个任务到文件
     */
    public boolean saveTask(Task task) {
        if (task == null || TextUtils.isEmpty(task.getId())) return false;

        try {
            File file = getTaskFile(task.getId());
            JSONObject json = task.toJson();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(json.toString(4)); // 格式化输出，便于查看
            writer.close();
            Log.d(TAG, "任务已保存: " + task.getName());
            return true;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "保存任务失败: " + task.getName(), e);
            return false;
        }
    }

    /**
     * 从文件加载单个任务
     */
    public Task loadTask(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(content.toString());
            Task task = new Task(json);
            Log.d(TAG, "任务已加载: " + task.getName());
            return task;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "加载任务失败: " + file.getName(), e);
            return null;
        }
    }

    /**
     * 加载所有任务
     */
    public void loadAllTasks() {
        tasks.clear();
        File dir = getTasksDirectory();
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.getName().endsWith(TASK_FILE_EXT)) {
                Task task = loadTask(file);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }

        sortTasksByPriority();
        Log.i(TAG, "共加载 " + tasks.size() + " 个任务");
    }

    /**
     * 按优先级排序任务
     */
    private void sortTasksByPriority() {
        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                return Integer.compare(t2.getPriority(), t1.getPriority());
            }
        });
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(Task task) {
        if (task == null) return false;
        tasks.remove(task);
        File file = getTaskFile(task.getId());
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    /**
     * 删除任务（通过ID）
     */
    public boolean deleteTask(String taskId) {
        Task task = getTaskById(taskId);
        return deleteTask(task);
    }

    /**
     * 导出所有任务
     */
    public boolean exportTasks(String exportPath) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Task task : tasks) {
                jsonArray.put(task.toJson());
            }

            File file = new File(exportPath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(jsonArray.toString(4));
            writer.close();
            Log.i(TAG, "任务已导出: " + exportPath);
            return true;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "导出任务失败", e);
            return false;
        }
    }

    /**
     * 导入任务
     */
    public int importTasks(String importPath) {
        try {
            File file = new File(importPath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            JSONArray jsonArray = new JSONArray(content.toString());
            int count = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                if (json != null) {
                    Task task = new Task(json);
                    task.setId(task.getId() + "_imported_" + System.currentTimeMillis());
                    addTask(task);
                    count++;
                }
            }
            Log.i(TAG, "导入完成，共导入 " + count + " 个任务");
            return count;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "导入任务失败", e);
            return 0;
        }
    }

    // ==================== 任务管理操作 ====================

    /**
     * 添加任务
     */
    public void addTask(Task task) {
        if (task == null) return;
        tasks.add(task);
        saveTask(task);
        sortTasksByPriority();
    }

    /**
     * 更新任务
     */
    public void updateTask(Task task) {
        if (task == null) return;
        // 查找并替换
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                saveTask(task);
                sortTasksByPriority();
                return;
            }
        }
        addTask(task);
    }

    /**
     * 通过ID获取任务
     */
    public Task getTaskById(String taskId) {
        for (Task task : tasks) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    /**
     * 获取所有任务
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * 获取所有启用的任务
     */
    public List<Task> getEnabledTasks() {
        List<Task> enabledTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.isEnabled()) {
                enabledTasks.add(task);
            }
        }
        return enabledTasks;
    }

    // ==================== 事件匹配和执行 ====================

    /**
     * 处理日志事件，匹配并执行任务
     */
    public void processLogEvent(String eventId, String rawLog) {
        if (TextUtils.isEmpty(eventId)) return;

        List<Task> matchedTasks = findMatchingTasks(eventId);
        for (Task task : matchedTasks) {
            executeTask(task);
        }
    }

    /**
     * 查找匹配日志事件的任务
     */
    private List<Task> findMatchingTasks(String eventId) {
        List<Task> matchedTasks = new ArrayList<>();
        for (Task task : getEnabledTasks()) {
            if (matchTaskToEvent(task, eventId)) {
                matchedTasks.add(task);
            }
        }
        return matchedTasks;
    }

    /**
     * 检查任务是否匹配事件
     */
    private boolean matchTaskToEvent(Task task, String eventId) {
        if (!task.canExecute()) return false;

        String mode = task.getConditionMode();
        List<Trigger> triggers = task.getTriggers();

        if (Task.CONDITION_MODE_ANY.equals(mode)) {
            for (Trigger trigger : triggers) {
                if (matchTriggerToEvent(trigger, eventId)) {
                    return true;
                }
            }
        } else if (Task.CONDITION_MODE_ALL.equals(mode)) {
            boolean allMatch = true;
            for (Trigger trigger : triggers) {
                if (!matchTriggerToEvent(trigger, eventId)) {
                    allMatch = false;
                    break;
                }
            }
            return allMatch;
        }
        return false;
    }

    /**
     * 检查触发条件是否匹配事件
     */
    private boolean matchTriggerToEvent(Trigger trigger, String eventId) {
        if (!Trigger.TYPE_LOG_EVENT.equals(trigger.getType())) return false;
        if (TextUtils.isEmpty(trigger.getEventId())) return false;
        return trigger.getEventId().equals(eventId);
    }

    /**
     * 执行任务
     */
    public void executeTask(Task task) {
        if (task == null || !task.canExecute()) return;

        Log.i(TAG, "执行任务: " + task.getName());
        workHandler.post(new ExecuteTaskRunnable(task));
    }

    /**
     * 任务执行Runnable
     */
    private class ExecuteTaskRunnable implements Runnable {
        private Task task;

        ExecuteTaskRunnable(Task task) {
            this.task = task;
        }

        @Override
        public void run() {
            // 标记任务已执行
            task.markExecuted();
            saveTask(task);

            // 按顺序执行动作
            List<Action> actions = task.getActions();
            for (Action action : actions) {
                executeAction(action);
            }
        }
    }

    /**
     * 执行单个动作
     */
    private void executeAction(Action action) {
        if (action == null) return;

        // 执行延迟
        if (action.getDelayMs() > 0) {
            try {
                Thread.sleep(action.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // 延迟动作特殊处理
        if (Action.TYPE_DELAY.equals(action.getType())) {
            Log.d(TAG, "延迟动作已执行");
            return;
        }

        // 通过ActionExecutor执行动作
        try {
            ActionExecutor.execute(appContext, action.getType(), action.buildPayload());
            Log.d(TAG, "动作执行成功: " + action.getDisplayName());
        } catch (Exception e) {
            Log.e(TAG, "动作执行失败: " + action.getDisplayName(), e);
        }
    }

    /**
     * 创建示例任务（用于测试）
     */
    public void createSampleTasks() {
        Log.i(TAG, "创建示例任务");

        // 示例任务1: 挂入D挡语音提示
        Task task1 = new Task();
        task1.setName("D挡提示");
        task1.setDescription("挂入D挡时语音提示");
        Trigger trigger1 = Trigger.createLogEventTrigger(
                CarEvent.GEAR_D.getEventId(), "", "挂入D挡");
        task1.addTrigger(trigger1);
        Action action1 = Action.createSpeechAction("出发！", "语音提示");
        task1.addAction(action1);
        addTask(task1);

        // 示例任务2: 挂入R挡语音提示+打开360
        Task task2 = new Task();
        task2.setName("R挡提示");
        task2.setDescription("挂入R挡时语音提示并打开360");
        Trigger trigger2 = Trigger.createLogEventTrigger(
                CarEvent.GEAR_R.getEventId(), "", "挂入R挡");
        task2.addTrigger(trigger2);
        Action action2a = Action.createSpeechAction("请注意，倒车！", "语音提示");
        Action action2b = new Action();
        action2b.setType(Action.TYPE_AROUND_TOGGLE_VIEW);
        action2b.setDescription("打开360环视");
        action2b.setDelayMs(500);
        task2.addAction(action2a);
        task2.addAction(action2b);
        addTask(task2);

        Log.i(TAG, "示例任务创建完成");
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (workThread != null && Build.VERSION.SDK_INT >= 18) {
            workThread.quitSafely();
        } else if (workThread != null) {
            workThread.quit();
        }
    }
}
