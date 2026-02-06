package com.minesms.launcher3;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.Arrays;
import java.util.List;

public class WSADetector {
    // WSA特有的系统应用包名
    private static final List<String> WSA_SYSTEM_APPS = Arrays.asList(
        "com.microsoft.windows.userapp",
        "com.microsoft.windows.systemapp", 
        "com.microsoft.windows.homeapp",
        "com.microsoft.wsa",
        "com.microsoft.android.surface"
    );

    // 检测是否为WSA设备
    public static boolean isWSA(Context context) {
        return checkBySystemApps(context);
    }

    // 通过检查Microsoft系统应用来检测WSA
    private static boolean checkBySystemApps(Context context) {
        PackageManager pm = context.getPackageManager();

        for (String packageName : WSA_SYSTEM_APPS) {
            if (isSystemAppInstalled(pm, packageName)) {
                return true;
            }
        }
        return false;
    }

    // 检查特定包名的应用是否存在且为系统应用
    private static boolean isSystemAppInstalled(PackageManager pm, String packageName) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            // 检查是否为系统应用
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            // 应用不存在
            return false;
        }
    }

    // 获取检测到的WSA应用信息
    public static String getDetectedWSAApp(Context context) {
        PackageManager pm = context.getPackageManager();

        for (String packageName : WSA_SYSTEM_APPS) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    return packageName + " (系统应用)";
                }
            } catch (PackageManager.NameNotFoundException e) {
                // 继续检查下一个
            }
        }
        return "未检测到WSA应用";
    }
}

