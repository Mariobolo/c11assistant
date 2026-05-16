package com.leapmotor.c11assistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import com.leapmotor.c11assistant.ui.MainActivity;

@SuppressWarnings("deprecation")
public class FloatBallService extends Service {
    private static final String CH = "c11_float";
    private WindowManager wm;
    private View ball;
    private LinearLayout menu;
    private WindowManager.LayoutParams ballLp;
    private WindowManager.LayoutParams menuLp;

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
        PendingIntent pi = PendingIntent.getActivity(this, 201, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CH).setSmallIcon(android.R.drawable.presence_online).setContentTitle("C11悬浮球运行中").setContentIntent(pi).setOngoing(true).build();
        startForeground(101, n);
        if (!Settings.canDrawOverlays(this)) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        initBall();
        initMenu();
    }

    private void initBall() {
        Button b = new Button(this);
        b.setText("○");
        b.setTextSize(20f);
        ball = b;
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        ballLp = new WindowManager.LayoutParams(120, 120, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        ballLp.gravity = Gravity.TOP | Gravity.START;
        ballLp.x = 1700;
        ballLp.y = 400;
        wm.addView(ball, ballLp);
        b.setOnClickListener(v -> toggleMenu());
        b.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            long t;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    downX = e.getRawX(); downY = e.getRawY(); startX = ballLp.x; startY = ballLp.y; t = System.currentTimeMillis(); return false;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    ballLp.x = startX + (int) (e.getRawX() - downX);
                    ballLp.y = startY + (int) (e.getRawY() - downY);
                    wm.updateViewLayout(ball, ballLp);
                    if (menu != null && menu.getParent() != null) { menuLp.x = ballLp.x - 260; menuLp.y = ballLp.y; wm.updateViewLayout(menu, menuLp);} return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP && System.currentTimeMillis() - t > 120) {
                    int center = wm.getDefaultDisplay().getWidth() / 2;
                    ballLp.x = ballLp.x < center ? 0 : wm.getDefaultDisplay().getWidth() - ball.getWidth();
                    wm.updateViewLayout(ball, ballLp); return true;
                }
                return false;
            }
        });
    }

    private void initMenu() {
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackgroundColor(0xCC111111);
        addMenuButton("返回桌面", v -> startActivity(homeIntent()));
        addMenuButton("返回上一页", v -> ActionExecutorBridge.back(this));
        addMenuButton("打开副屏桌面", v -> ActionExecutorBridge.openSecondaryHome(this));
        addMenuButton("打开360全景", v -> ActionExecutorBridge.openAround(this));
        addMenuButton("打开设置", v -> startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        menuLp = new WindowManager.LayoutParams(260, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        menuLp.gravity = Gravity.TOP | Gravity.START;
        menuLp.x = 1400; menuLp.y = 400;
    }

    private void addMenuButton(String text, View.OnClickListener click) {
        Button btn = new Button(this); btn.setText(text); btn.setOnClickListener(click); menu.addView(btn);
    }

    private void toggleMenu() {
        if (menu.getParent() == null) wm.addView(menu, menuLp); else wm.removeView(menu);
    }

    private Intent homeIntent() { Intent i = new Intent(Intent.ACTION_MAIN); i.addCategory(Intent.CATEGORY_HOME); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); return i; }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CH, "C11悬浮球", NotificationManager.IMPORTANCE_MIN));
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        if (ball != null && ball.getParent() != null) wm.removeView(ball);
        if (menu != null && menu.getParent() != null) wm.removeView(menu);
        super.onDestroy();
    }

    public static class ActionExecutorBridge {
        public static void back(Service s) { com.leapmotor.c11assistant.manager.ActionExecutor.execute(s, "GLOBAL_BACK", ""); }
        public static void openSecondaryHome(Service s) { com.leapmotor.c11assistant.manager.MultiScreenManager.get(s).launchOnSecondary("com.android.launcher"); }
        public static void openAround(Service s) { com.leapmotor.c11assistant.manager.MultiScreenManager.get(s).launchPackage("com.leapmotor.aroundview"); }
    }
}
