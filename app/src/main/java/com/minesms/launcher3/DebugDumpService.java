package com.minesms.launcher3;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugDumpService extends Service {
    private static final String TAG = "DebugDumpService";
    private Process logcatProcess;
    private FileOutputStream logOutputStream;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLogcatCapture();
        return START_STICKY;
    }

    private void startLogcatCapture() {
        new Thread(new Runnable() {
                @Override
                public void run() {
                    captureLogcat();
                }
            }).start();
    }

    private void captureLogcat() {
        try {
            // 创建日志目录
            File logDir = new File("/sdcard/Android/media/com.minesms.launcher3");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 生成带时间戳的文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = timeStamp + "_log.log";
            File logFile = new File(logDir, fileName);

            // 启动 logcat 进程
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("logcat", "-b", "all", "-v", "time", "-f", logFile.getAbsolutePath());
            Runtime.getRuntime().exec("su -c 'logcat > /sdcard/Android/com.minesms.launcher3_logcat.log'");
            logcatProcess = processBuilder.start();

            Log.d(TAG, "Logcat capture started: " + logFile.getAbsolutePath());

            // 等待进程结束（实际上会一直运行直到服务停止）
            logcatProcess.waitFor();

        } catch (Exception e) {
            Log.e(TAG, "Logcat capture failed: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLogcatCapture();
    }

    private void stopLogcatCapture() {
        try {
            if (logcatProcess != null) {
                // 停止 logcat 进程
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("logcat", "-c"); // 清空日志缓冲区
                Process clearProcess = processBuilder.start();
                clearProcess.waitFor();

                logcatProcess.destroy();
                logcatProcess = null;
                Log.d(TAG, "Logcat capture stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop logcat capture failed: " + e.getMessage());
        }
    }
}
