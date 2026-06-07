package com.leapmotor.c11assistant.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.leapmotor.c11assistant.R;
import com.leapmotor.c11assistant.manager.TaskManager;
import com.leapmotor.c11assistant.model.Task;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class TaskListActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private LinearLayout llEmpty;
    private TaskManager taskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        taskManager = TaskManager.get(this);

        initViews();
        loadTasks();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rv_tasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, null, this);
        rvTasks.setAdapter(adapter);

        llEmpty = findViewById(R.id.ll_empty);

        Button btnAddTask = findViewById(R.id.btn_add_task);
        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskEditorActivity.class);
            startActivity(intent);
        });

        Button btnImport = findViewById(R.id.btn_import);
        btnImport.setOnClickListener(v -> importTasks());

        Button btnExport = findViewById(R.id.btn_export);
        btnExport.setOnClickListener(v -> exportTasks());

        Button btnCreateSample = findViewById(R.id.btn_create_sample);
        btnCreateSample.setOnClickListener(v -> createSampleTasks());
    }

    private void loadTasks() {
        List<Task> tasks = taskManager.getAllTasks();
        adapter.updateList(tasks);
        llEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static final int REQUEST_CODE_IMPORT = 1001;

    private void importTasks() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择任务文件"), REQUEST_CODE_IMPORT);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "未找到文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                importTasksFromUri(uri);
            }
        }
    }

    private void importTasksFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            String importPath = getExternalFilesDir(null) + "/temp_import.json";
            java.io.FileWriter writer = new java.io.FileWriter(importPath);
            writer.write(content.toString());
            writer.close();

            int count = taskManager.importTasks(importPath);
            loadTasks();
            Toast.makeText(this, "成功导入 " + count + " 个任务", Toast.LENGTH_SHORT).show();

            new java.io.File(importPath).delete();
        } catch (Exception e) {
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportTasks() {
        String exportPath = getExternalFilesDir(null) + "/tasks_export.json";
        boolean success = taskManager.exportTasks(exportPath);
        if (success) {
            Toast.makeText(this, "任务已导出到: " + exportPath, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void createSampleTasks() {
        taskManager.createSampleTasks();
        loadTasks();
        Toast.makeText(this, "示例任务已创建", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(this, TaskEditorActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskToggle(Task task, boolean enabled) {
        task.setEnabled(enabled);
        taskManager.updateTask(task);
        Toast.makeText(this, enabled ? "任务已启用" : "任务已禁用", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskDelete(final Task task) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除任务 '" + task.getName() + "' 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    taskManager.deleteTask(task);
                    loadTasks();
                    Toast.makeText(TaskListActivity.this, "任务已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onTaskCopy(Task task) {
        Task copy = new Task();
        copy.setName(task.getName() + " (副本)");
        copy.setDescription(task.getDescription());
        copy.setPriority(task.getPriority());
        copy.setDebounceMs(task.getDebounceMs());
        copy.setConditionMode(task.getConditionMode());

        for (int i = 0; i < task.getTriggers().size(); i++) {
            copy.addTrigger(task.getTriggers().get(i));
        }
        for (int i = 0; i < task.getActions().size(); i++) {
            copy.addAction(task.getActions().get(i));
        }

        taskManager.addTask(copy);
        loadTasks();
        Toast.makeText(this, "任务已复制", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }
}
