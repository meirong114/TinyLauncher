package com.minesms.launcher3;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AppMonitorService extends Service {
    private static final String TAG = "AppMonitorService";
    private static final long CHECK_INTERVAL = 10000; // 10秒

    private Handler handler;
    private Runnable checkRunnable;
    private PackageManager packageManager;
    private Set<String> currentApps = new HashSet<>();
    private AppUpdateListener appUpdateListener;

    public interface AppUpdateListener {
        void onAppsUpdated(List<ResolveInfo> newApps);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AppMonitorService created");

        handler = new Handler();
        packageManager = getPackageManager();

        // 初始化当前应用列表
        updateCurrentApps();

        // 注册应用安装/卸载的广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        registerReceiver(appChangeReceiver, intentFilter);

        startPeriodicCheck();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AppMonitorService started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AppMonitorService destroyed");

        // 清理资源
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }

        try {
            unregisterReceiver(appChangeReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAppUpdateListener(AppUpdateListener listener) {
        this.appUpdateListener = listener;
    }

    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkForNewApps();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.postDelayed(checkRunnable, CHECK_INTERVAL);
    }

    private void checkForNewApps() {
        List<ResolveInfo> newApps = getInstalledApps();
        Set<String> newAppPackages = new HashSet<>();

        for (ResolveInfo app : newApps) {
            newAppPackages.add(app.activityInfo.packageName);
        }

        // 检查是否有新应用
        if (!newAppPackages.equals(currentApps)) {
            Log.d(TAG, "Detected app changes");
            if (appUpdateListener != null) {
                appUpdateListener.onAppsUpdated(newApps);
            }
            currentApps = newAppPackages;
        }
    }

    private List<ResolveInfo> getInstalledApps() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            return packageManager.queryIntentActivities(intent, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
            return new ArrayList<>();
        }
    }

    private void updateCurrentApps() {
        List<ResolveInfo> apps = getInstalledApps();
        currentApps.clear();
        for (ResolveInfo app : apps) {
            currentApps.add(app.activityInfo.packageName);
        }
    }

    // 广播接收器，监听应用安装/卸载事件
    private final BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action) || 
                Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

                Log.d(TAG, "App change detected: " + action);
                // 立即检查应用变化
                handler.post(new Runnable() {
                        @Override
                        public void run() {
                            checkForNewApps();
                        }
                    });
            }
        }
    };
}

