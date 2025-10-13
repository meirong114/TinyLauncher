package com.minesms.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.provider.Settings;
import android.widget.Toast;

import java.io.IOException;

public class DevelopSet extends Activity {
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_develop_set);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(this, PolicyManager.class);

        // 激活设备管理器
        Button activateDeviceAdmin = findViewById(R.id.activateDeviceAdminButton);
        activateDeviceAdmin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activateDeviceAdmin();
                }
            });

        // 清除桌面数据
        Button clearLauncherData = findViewById(R.id.clearDataButton);
        clearLauncherData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearLauncherData();
                }
            });

        // 重启桌面
        Button restartLauncher = findViewById(R.id.restartLauncherButton);
        restartLauncher.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    restartLauncher();
                }
            });

        // 启动原生系统设置
        Button openSystemSettings = findViewById(R.id.launchSystemSettingsButton);
        openSystemSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSystemSettings();
                }
            });

        // 打开“开发者模式”页面
        Button openDeveloperOptions = findViewById(R.id.openDeveloperOptionsButton);
        openDeveloperOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDeveloperOptions();
                }
            });

        // 启用网络ADB
        Button enableNetworkAdb = findViewById(R.id.enableNetworkAdbButton);
        enableNetworkAdb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enableNetworkAdb();
                }
            });
    }

    private void activateDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This will activate the device admin.");
        startActivityForResult(intent, 1);
    }

    private void clearLauncherData() {
        try {
            // 使用 ProcessBuilder 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "pm clear com.minesms.launcher3");
            Process process = processBuilder.start();
            
            Runtime.getRuntime().exec("rm -rf /data/data/com.minesms.launcher3");

            // 等待命令执行完成
            int exitCode = process.waitFor();

    // 检查命令是否成功执行
    if (exitCode == 0) {
    Toast.makeText(this, "Desktop data cleared", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to clear data \n " + exitCode, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to clear data \n " + e, Toast.LENGTH_SHORT).show();
        }
    }
    

    private void restartLauncher() {
        try {
            Runtime.getRuntime().exec("su -c 'am force-stop com.minesms.launcher3'");
            Toast.makeText(this, "Desktop restarted", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to restart" + e, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSystemSettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open system settings" + "\n\n" + e + "(e)", Toast.LENGTH_SHORT).show();
            try {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
                startActivity(intent);
            } catch (Exception f) {
                Toast.makeText(this, "Failed to open system settings" + "\n\n" + f + "(f)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openDeveloperOptions() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open developer options" + "\n\n" + e, Toast.LENGTH_SHORT).show();
        }
    }

    private void enableNetworkAdb() {
        try {
            Runtime.getRuntime().exec("su -c 'setprop service.adb.tcp.port 5555'");
            Runtime.getRuntime().exec("su -c 'stop adbd'");
            Runtime.getRuntime().exec("su -c 'start adbd'");
            Toast.makeText(this, "Network ADB enabled", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to enable network ADB" + "\n\n" + e, Toast.LENGTH_SHORT).show();
        }
    }
}

