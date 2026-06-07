package com.leapmotor.c11assistant.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.leapmotor.c11assistant.R;
import com.leapmotor.c11assistant.manager.TaskManager;
import com.leapmotor.c11assistant.model.Action;
import com.leapmotor.c11assistant.model.CarEvent;
import com.leapmotor.c11assistant.model.Task;
import com.leapmotor.c11assistant.model.Trigger;

import java.util.ArrayList;
import java.util.List;

public class TaskEditorActivity extends AppCompatActivity {

    private EditText etTaskName;
    private EditText etTaskDesc;
    private EditText etPriority;
    private EditText etDebounce;
    private RadioGroup rgConditionMode;
    private RecyclerView rvTriggers;
    private RecyclerView rvActions;

    private Task task;
    private TaskManager taskManager;
    private List<Trigger> triggers;
    private List<Action> actions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor);

        taskManager = TaskManager.get(this);
        triggers = new ArrayList<>();
        actions = new ArrayList<>();

        initViews();
        loadTask();
    }

    private void initViews() {
        etTaskName = findViewById(R.id.et_task_name);
        etTaskDesc = findViewById(R.id.et_task_desc);
        etPriority = findViewById(R.id.et_priority);
        etDebounce = findViewById(R.id.et_debounce);
        rgConditionMode = findViewById(R.id.rg_condition_mode);
        rvTriggers = findViewById(R.id.rv_triggers);
        rvActions = findViewById(R.id.rv_actions);

        rvTriggers.setLayoutManager(new LinearLayoutManager(this));
        rvActions.setLayoutManager(new LinearLayoutManager(this));

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveTask());

        Button btnAddTrigger = findViewById(R.id.btn_add_trigger);
        btnAddTrigger.setOnClickListener(v -> showTriggerPicker());

        Button btnAddAction = findViewById(R.id.btn_add_action);
        btnAddAction.setOnClickListener(v -> showActionPicker());
    }

    private void loadTask() {
        String taskId = getIntent().getStringExtra("taskId");
        if (taskId != null && !taskId.isEmpty()) {
            task = taskManager.getTaskById(taskId);
            if (task != null) {
                etTaskName.setText(task.getName());
                etTaskDesc.setText(task.getDescription());
                etPriority.setText(String.valueOf(task.getPriority()));
                etDebounce.setText(String.valueOf(task.getDebounceMs()));
                rgConditionMode.check(Task.CONDITION_MODE_ALL.equals(task.getConditionMode())
                        ? R.id.rb_all : R.id.rb_any);
                triggers.addAll(task.getTriggers());
                actions.addAll(task.getActions());
            }
        }
        if (task == null) {
            task = new Task();
        }
    }

    private void showTriggerPicker() {
        CarEvent[] events = CarEvent.values();
        String[] eventNames = new String[events.length];
        for (int i = 0; i < events.length; i++) {
            eventNames[i] = events[i].getDisplayName();
        }

        new AlertDialog.Builder(this)
                .setTitle("选择触发事件")
                .setItems(eventNames, (dialog, which) -> {
                    CarEvent event = events[which];
                    Trigger trigger = Trigger.createLogEventTrigger(
                            event.getEventId(), "", event.getDisplayName());
                    triggers.add(trigger);
                    Toast.makeText(this, "已添加触发条件: " + event.getDisplayName(), Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showActionPicker() {
        String[] actionTypes = {
                "语音播报",
                "启动应用",
                "设置温度",
                "打开360环视",
                "返回桌面",
                "延迟执行"
        };

        new AlertDialog.Builder(this)
                .setTitle("选择动作类型")
                .setItems(actionTypes, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showSpeechInput();
                            break;
                        case 1:
                            showLaunchAppInput();
                            break;
                        case 2:
                            showSetTempInput();
                            break;
                        case 3:
                            add360Action();
                            break;
                        case 4:
                            addHomeAction();
                            break;
                        case 5:
                            showDelayInput();
                            break;
                    }
                })
                .show();
    }

    private void showSpeechInput() {
        final EditText input = new EditText(this);
        input.setHint("请输入播报内容");

        new AlertDialog.Builder(this)
                .setTitle("语音播报")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        Action action = Action.createSpeechAction(text, "语音播报: " + text);
                        actions.add(action);
                        Toast.makeText(this, "已添加语音播报动作", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showLaunchAppInput() {
        final EditText input = new EditText(this);
        input.setHint("请输入应用包名（如 com.autonavi.minimap）");

        new AlertDialog.Builder(this)
                .setTitle("启动应用")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String packageName = input.getText().toString().trim();
                    if (!packageName.isEmpty()) {
                        Action action = Action.createLaunchAction(packageName, "启动: " + packageName);
                        actions.add(action);
                        Toast.makeText(this, "已添加启动应用动作", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showSetTempInput() {
        final EditText input = new EditText(this);
        input.setHint("请输入温度（16-30）");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("设置主驾温度")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        int temp = Integer.parseInt(input.getText().toString().trim());
                        if (temp >= 16 && temp <= 30) {
                            Action action = new Action();
                            action.setType(Action.TYPE_SET_HVAC_DRIVER_TEMP);
                            action.setParam(Action.PARAM_TEMPERATURE, temp);
                            action.setDescription("设置主驾温度: " + temp + "℃");
                            actions.add(action);
                            Toast.makeText(this, "已添加设置温度动作", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "温度范围应为16-30℃", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void add360Action() {
        Action action = new Action();
        action.setType(Action.TYPE_AROUND_TOGGLE_VIEW);
        action.setDescription("打开360环视");
        actions.add(action);
        Toast.makeText(this, "已添加360环视动作", Toast.LENGTH_SHORT).show();
    }

    private void addHomeAction() {
        Action action = new Action();
        action.setType(Action.TYPE_GLOBAL_HOME);
        action.setDescription("返回桌面");
        actions.add(action);
        Toast.makeText(this, "已添加返回桌面动作", Toast.LENGTH_SHORT).show();
    }

    private void showDelayInput() {
        final EditText input = new EditText(this);
        input.setHint("请输入延迟时间（毫秒）");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("延迟执行")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        long delay = Long.parseLong(input.getText().toString().trim());
                        if (delay >= 0 && delay <= 10000) {
                            Action action = Action.createDelayAction(delay, "延迟 " + delay + "ms");
                            actions.add(action);
                            Toast.makeText(this, "已添加延迟动作", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "延迟时间应为0-10000ms", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }

        if (triggers.isEmpty()) {
            Toast.makeText(this, "请至少添加一个触发条件", Toast.LENGTH_SHORT).show();
            return;
        }

        if (actions.isEmpty()) {
            Toast.makeText(this, "请至少添加一个动作", Toast.LENGTH_SHORT).show();
            return;
        }

        task.setName(name);
        task.setDescription(etTaskDesc.getText().toString().trim());

        try {
            task.setPriority(Integer.parseInt(etPriority.getText().toString().trim()));
        } catch (NumberFormatException e) {
            task.setPriority(100);
        }

        try {
            task.setDebounceMs(Long.parseLong(etDebounce.getText().toString().trim()));
        } catch (NumberFormatException e) {
            task.setDebounceMs(500);
        }

        task.setConditionMode(rgConditionMode.getCheckedRadioButtonId() == R.id.rb_all
                ? Task.CONDITION_MODE_ALL : Task.CONDITION_MODE_ANY);

        task.getTriggers().clear();
        task.getTriggers().addAll(triggers);

        task.getActions().clear();
        task.getActions().addAll(actions);

        taskManager.addTask(task);
        Toast.makeText(this, "任务保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }
}
