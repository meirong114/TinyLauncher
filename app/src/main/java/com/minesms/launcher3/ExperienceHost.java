package com.minesms.launcher3;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class ExperienceHost extends Service {
    private static final String TAG = "ExperienceHost";
    private static final String MUSIC_URL = "https://meirong114.github.io/TinyLauncher/ep16.mp3";
    private static final String FILE_NAME = "ep16.mp3";
    private static final String CACHE_DIR = "cache";

    private MediaPlayer mediaPlayer;
    private String filePath;

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建缓存目录
        File cacheDir = new File(getFilesDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        filePath = cacheDir.getAbsolutePath() + File.separator + FILE_NAME;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new DownloadAndPlayTask().execute();
        return START_STICKY;
    }

    private class DownloadAndPlayTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                File file = new File(filePath);

                // 如果本地文件存在，检查MD5
                if (file.exists()) {
                    String localMd5 = calculateMD5(file);
                    String serverMd5 = getServerMD5();

                    if (localMd5.equals(serverMd5)) {
                        Log.d(TAG, "使用本地缓存文件");
                        return true;
                    }
                }

                Log.d(TAG, "开始下载音乐文件...");
                URL url = new URL(MUSIC_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "服务器响应错误: " + connection.getResponseCode());
                    return false;
                }

                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(filePath);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();
                connection.disconnect();

                Log.d(TAG, "音乐文件下载完成");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "下载失败: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                playMusic();
            } else {
                // 静默使用本地缓存，不提示用户
                useLocalCache();
            }
        }
    }

    private void playMusic() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "开始播放音乐");
        } catch (Exception e) {
            Log.e(TAG, "播放失败: " + e.getMessage());
            // 静默使用本地缓存
            useLocalCache();
        }
    }

    private void useLocalCache() {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
                Log.d(TAG, "使用本地缓存音乐播放");
            }
        } catch (Exception e) {
            Log.e(TAG, "本地缓存播放失败: " + e.getMessage());
            // 完全静默，不进行任何用户提示
        }
    }

    private String calculateMD5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream input = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            input.close();
            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "MD5计算失败");
            return "";
        }
    }

    private String getServerMD5() {
        // 返回服务器MD5，这里需要你实际实现
        return "a4bfc75aeca513a8b21cac128ce973ed";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
