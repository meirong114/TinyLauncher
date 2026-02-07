package com.minesms.launcher3;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.*;

public class ApkExtractor {

    private static final String TAG = "ApkExtractor";

    /**
     * 从assets释放APK到外部存储目录
     *
     * @param context 应用上下文
     * @param assetFileName assets目录中的APK文件名
     * @param targetPath 目标路径（如Android/data/your.package.name/files/）
     */
    public static void extractApk(Context context, String assetFileName, String targetPath) {
        // 检查存储权限
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "没有存储权限");
            return;
        }

        // 检查外部存储是否可用
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "外部存储不可用");
            return;
        }

        // 创建目标目录
        File targetDir = new File(targetPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 定义目标文件路径
        File targetFile = new File(targetDir, assetFileName);

        try {
            // 从assets获取输入流
            InputStream inputStream = context.getAssets().open(assetFileName);
            // 创建输出流
            OutputStream outputStream = new FileOutputStream(targetFile);

            // 复制文件
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "APK文件已成功释放到：" + targetFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "释放APK文件失败", e);
        }
    }
}

