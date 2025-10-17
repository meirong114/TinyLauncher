package com.minesms.launcher3;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AntiACEServiceManager {
    private static final String TAG = "AntiACEServiceManager";

    /**
     * 启动反作弊检测服务
     */
    public static void startService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, AntiACEService.class);
            context.startService(serviceIntent);
            Log.d(TAG, "反作弊检测服务启动成功");
        } catch (Exception e) {
            Log.e(TAG, "启动反作弊检测服务失败: " + e.getMessage());
        }
    }

    /**
     * 停止反作弊检测服务
     */
    public static void stopService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, AntiACEService.class);
            context.stopService(serviceIntent);
            Log.d(TAG, "反作弊检测服务停止成功");
        } catch (Exception e) {
            Log.e(TAG, "停止反作弊检测服务失败: " + e.getMessage());
        }
    }
}
