package com.minesms.launcher3;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class WallpaperHelper {
    private static final String TAG = "WallpaperHelper";

    // 设置黑色壁纸
    public static boolean setBlackWallpaper(Context context) {
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

            // 创建纯黑色Bitmap
            Bitmap blackBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            blackBitmap.eraseColor(Color.BLACK);

            // 设置壁纸
            wallpaperManager.setBitmap(blackBitmap);

            Log.d(TAG, "WSA环境：成功设置黑色壁纸");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "设置黑色壁纸失败: " + e.getMessage());
            return false;
        }
    }

    // 安全地设置窗口背景（拦截WSA环境的壁纸加载）
    public static void setWindowBackgroundSafely(Context context, android.view.Window window) {
        if (WSADetector.isWSA(context)) {
            Log.w(TAG, "检测到WSA环境，拦截壁纸加载，使用黑色背景");
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        } else {
            try {
                // 正常Android设备：设置壁纸背景
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                window.setBackgroundDrawable(wallpaperDrawable);
                Log.d(TAG, "正常Android环境：设置壁纸背景");
            } catch (Exception e) {
                Log.e(TAG, "设置壁纸背景失败，使用默认背景: " + e.getMessage());
                window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            }
        }
    }

    // 安全地获取壁纸Drawable（在WSA环境中返回黑色背景）
    public static Drawable getWallpaperDrawableSafely(Context context) {
        if (WSADetector.isWSA(context)) {
            Log.d(TAG, "WSA环境：返回黑色背景");
            return new ColorDrawable(Color.BLACK);
        }

        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            Drawable wallpaper = wallpaperManager.getDrawable();
            Log.d(TAG, "正常环境：返回系统壁纸");
            return wallpaper;
        } catch (Exception e) {
            Log.e(TAG, "获取壁纸失败，返回黑色背景: " + e.getMessage());
            return new ColorDrawable(Color.BLACK);
        }
    }
}

