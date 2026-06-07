package com.leapmotor.c11assistant.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 自定义任务数据模型
 * 包含任务名称、触发条件列表、动作序列列表和配置选项
 */
public class Task {
    private static final String TAG = "Task";

    // JSON字段名常量
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_DEBOUNCE_MS = "debounce_ms";
    private static final String KEY_TRIGGERS = "triggers";
    private static final String KEY_ACTIONS = "actions";
    private static final String KEY_CONDITION_MODE = "condition_mode";
    private static final String KEY_MAX_EXECUTIONS = "max_executions";
    private static final String KEY_EXECUTION_COUNT = "execution_count";
    private static final String KEY_LAST_EXECUTION = "last_execution";

    // 条件模式常量
    public static final String CONDITION_MODE_ANY = "any";      // 任一触发条件满足
    public static final String CONDITION_MODE_ALL = "all";      // 所有触发条件都满足

    private String id;                      // 任务唯一ID
    private String name;                    // 任务名称
    private String description;             // 任务描述
    private boolean enabled;                // 是否启用
    private int priority;                   // 优先级（数字越大优先级越高）
    private long debounceMs;                // 防抖时间（毫秒）
    private String conditionMode;           // 条件模式（any/all）
    private int maxExecutions;              // 最大执行次数（-1为无限）
    private int executionCount;             // 已执行次数
    private long lastExecutionTime;         // 上次执行时间
    private List<Trigger> triggers;         // 触发条件列表
    private List<Action> actions;           // 动作序列列表

    /**
     * 默认构造函数
     */
    public Task() {
        this.id = generateId();
        this.name = "新任务";
        this.description = "";
        this.enabled = true;
        this.priority = 100;
        this.debounceMs = 500;
        this.conditionMode = CONDITION_MODE_ANY;
        this.maxExecutions = -1;
        this.executionCount = 0;
        this.lastExecutionTime = 0;
        this.triggers = new ArrayList<>();
        this.actions = new ArrayList<>();
    }

    /**
     * 通过JSON构造
     */
    public Task(JSONObject json) {
        this();
        fromJson(json);
    }

    /**
     * 生成唯一ID
     */
    private String generateId() {
        return "task_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // ==================== 从JSON解析 ====================

    public void fromJson(JSONObject json) {
        if (json == null) return;
        this.id = json.optString(KEY_ID, generateId());
        this.name = json.optString(KEY_NAME, "新任务");
        this.description = json.optString(KEY_DESCRIPTION, "");
        this.enabled = json.optBoolean(KEY_ENABLED, true);
        this.priority = json.optInt(KEY_PRIORITY, 100);
        this.debounceMs = json.optLong(KEY_DEBOUNCE_MS, 500);
        this.conditionMode = json.optString(KEY_CONDITION_MODE, CONDITION_MODE_ANY);
        this.maxExecutions = json.optInt(KEY_MAX_EXECUTIONS, -1);
        this.executionCount = json.optInt(KEY_EXECUTION_COUNT, 0);
        this.lastExecutionTime = json.optLong(KEY_LAST_EXECUTION, 0);

        // 解析触发条件
        this.triggers.clear();
        JSONArray triggersArray = json.optJSONArray(KEY_TRIGGERS);
        if (triggersArray != null) {
            for (int i = 0; i < triggersArray.length(); i++) {
                JSONObject triggerJson = triggersArray.optJSONObject(i);
                if (triggerJson != null) {
                    this.triggers.add(new Trigger(triggerJson));
                }
            }
        }

        // 解析动作序列
        this.actions.clear();
        JSONArray actionsArray = json.optJSONArray(KEY_ACTIONS);
        if (actionsArray != null) {
            for (int i = 0; i < actionsArray.length(); i++) {
                JSONObject actionJson = actionsArray.optJSONObject(i);
                if (actionJson != null) {
                    Action action = new Action(actionJson);
                    this.actions.add(action);
                }
            }
        }

        // 按order排序动作
        sortActionsByOrder();
    }

    // ==================== 转换为JSON ====================

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_ID, id);
            json.put(KEY_NAME, name);
            json.put(KEY_DESCRIPTION, description);
            json.put(KEY_ENABLED, enabled);
            json.put(KEY_PRIORITY, priority);
            json.put(KEY_DEBOUNCE_MS, debounceMs);
            json.put(KEY_CONDITION_MODE, conditionMode);
            json.put(KEY_MAX_EXECUTIONS, maxExecutions);
            json.put(KEY_EXECUTION_COUNT, executionCount);
            json.put(KEY_LAST_EXECUTION, lastExecutionTime);

            // 添加触发条件
            JSONArray triggersArray = new JSONArray();
            for (Trigger trigger : triggers) {
                triggersArray.put(trigger.toJson());
            }
            json.put(KEY_TRIGGERS, triggersArray);

            // 添加动作序列
            JSONArray actionsArray = new JSONArray();
            for (Action action : actions) {
                actionsArray.put(action.toJson());
            }
            json.put(KEY_ACTIONS, actionsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    // ==================== 触发条件操作 ====================

    public void addTrigger(Trigger trigger) {
        if (trigger != null) {
            triggers.add(trigger);
        }
    }

    public void removeTrigger(Trigger trigger) {
        triggers.remove(trigger);
    }

    public void removeTrigger(int index) {
        if (index >= 0 && index < triggers.size()) {
            triggers.remove(index);
        }
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public Trigger getTrigger(int index) {
        if (index >= 0 && index < triggers.size()) {
            return triggers.get(index);
        }
        return null;
    }

    // ==================== 动作序列操作 ====================

    public void addAction(Action action) {
        if (action != null) {
            action.setOrder(actions.size());
            actions.add(action);
        }
    }

    public void insertAction(int index, Action action) {
        if (action != null && index >= 0 && index <= actions.size()) {
            action.setOrder(index);
            actions.add(index, action);
            updateActionsOrder();
        }
    }

    public void removeAction(Action action) {
        actions.remove(action);
        updateActionsOrder();
    }

    public void removeAction(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index);
            updateActionsOrder();
        }
    }

    public void swapActions(int index1, int index2) {
        if (index1 >= 0 && index1 < actions.size() &&
            index2 >= 0 && index2 < actions.size() && index1 != index2) {
            Collections.swap(actions, index1, index2);
            updateActionsOrder();
        }
    }

    public void moveAction(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < actions.size() &&
            toIndex >= 0 && toIndex < actions.size() && fromIndex != toIndex) {
            Action action = actions.remove(fromIndex);
            actions.add(toIndex, action);
            updateActionsOrder();
        }
    }

    public List<Action> getActions() {
        return actions;
    }

    public Action getAction(int index) {
        if (index >= 0 && index < actions.size()) {
            return actions.get(index);
        }
        return null;
    }

    /**
     * 更新所有动作的order字段
     */
    private void updateActionsOrder() {
        for (int i = 0; i < actions.size(); i++) {
            actions.get(i).setOrder(i);
        }
    }

    /**
     * 按order排序动作
     */
    private void sortActionsByOrder() {
        Collections.sort(actions, new Comparator<Action>() {
            @Override
            public int compare(Action a1, Action a2) {
                return Integer.compare(a1.getOrder(), a2.getOrder());
            }
        });
    }

    // ==================== 执行控制 ====================

    /**
     * 检查任务是否可以执行
     */
    public boolean canExecute() {
        // 检查是否启用
        if (!enabled) return false;
        // 检查是否有触发条件和动作
        if (triggers.isEmpty() || actions.isEmpty()) return false;
        // 检查是否超过最大执行次数
        if (maxExecutions >= 0 && executionCount >= maxExecutions) return false;
        // 检查防抖
        long now = System.currentTimeMillis();
        if (now - lastExecutionTime < debounceMs) return false;
        return true;
    }

    /**
     * 记录执行
     */
    public void markExecuted() {
        executionCount++;
        lastExecutionTime = System.currentTimeMillis();
    }

    /**
     * 重置执行计数
     */
    public void resetExecutionCount() {
        executionCount = 0;
        lastExecutionTime = 0;
    }

    // ==================== Getter和Setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getDebounceMs() {
        return debounceMs;
    }

    public void setDebounceMs(long debounceMs) {
        this.debounceMs = debounceMs;
    }

    public String getConditionMode() {
        return conditionMode;
    }

    public void setConditionMode(String conditionMode) {
        this.conditionMode = conditionMode;
    }

    public int getMaxExecutions() {
        return maxExecutions;
    }

    public void setMaxExecutions(int maxExecutions) {
        this.maxExecutions = maxExecutions;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(long lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    /**
     * 获取任务摘要（用于显示）
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(triggers.size()).append("个触发条件");
        summary.append(" → ");
        summary.append(actions.size()).append("个动作");
        if (!TextUtils.isEmpty(description)) {
            summary.append("\n").append(description);
        }
        return summary.toString();
    }
}
