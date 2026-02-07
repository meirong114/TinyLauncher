package com.minesms.launcher3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.content.DialogInterface;
import android.app.AlertDialog;

public class UpdateChecker {

    private static final String UPDATE_JSON_URL = "https://fastly.jsdelivr.net/gh/meirong114/TinyLauncher@latest/update.json";
    private static final String UPDATE_CLEAR_URL = "https://purge.jsdelivr.net/gh/meirong114/TinyLauncher@latest/update.json";
    private Context context;

    public UpdateChecker(Context context) {
        this.context = context;
    }

    public void checkForUpdate() {
        // 检查上下文是否有效
        if (context == null || (context instanceof Activity && ((Activity) context).isFinishing())) {
            Log.e("UpdateChecker", "Context is invalid or activity is finishing");
            return;
        }
        new CheckUpdateTask().execute();
    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, String> {

        private Exception exception;
        private String currentVersion;

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(UPDATE_JSON_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                }
            } catch (Exception e) {
                Log.e("UpdateChecker", "Error checking for update", e);
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // 检查上下文是否仍然有效
            if (context == null || (context instanceof Activity && ((Activity) context).isFinishing())) {
                Log.e("UpdateChecker", "Context is invalid, skipping dialog display");
                return;
            }

            if (result != null) {
                try {
                    JSONObject updateJson = new JSONObject(result);
                    String latestVersion = updateJson.getString("version");
                    currentVersion = getCurrentVersion();

                    if (isNewVersionAvailable(currentVersion, latestVersion)) {
                        showUpdateDialog(updateJson);
                    } else {
                        // 显示已是最新版本提示
                        showNoUpdateDialog();
                    }
                } catch (Exception e) {
                    Log.e("UpdateChecker", "Error parsing update JSON", e);
                    showErrorDialog("解析更新信息失败", e);
                }
            } else {
                showErrorDialog("网络连接失败", exception);
            }
        }

        private void showErrorDialog(String title, Exception e) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);

                StringBuilder message = new StringBuilder();
                message.append("当前版本: ").append(getCurrentVersion()).append("\n\n");

                if (e != null) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("Canary")) {
                        message.append("Canary通道无法检查更新，请到GitHub查看最新版本。");
                    } else {
                        message.append("错误信息: ").append(e.getMessage()).append("\n\n");
                        message.append("请检查网络连接后重试。");
                    }
                } else {
                    message.append("请检查网络连接后重试。");
                }

                builder.setMessage(message.toString());
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                // 只有在Activity未销毁时才显示对话框
                if (!((Activity) context).isFinishing()) {
                    builder.show();
                }
            } catch (Exception dialogException) {
                Log.e("UpdateChecker", "Error showing error dialog", dialogException);
            }
        }

        private void showNoUpdateDialog() {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("检查更新");
                builder.setMessage("当前已是最新版本: " + getCurrentVersion());
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                if (!((Activity) context).isFinishing()) {
                    builder.show();
                }
            } catch (Exception e) {
                Log.e("UpdateChecker", "Error showing no update dialog", e);
            }
        }
    }

    private String getCurrentVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UpdateChecker", "Error getting current version", e);
            return "1.0.0";
        }
    }

    private boolean isNewVersionAvailable(String currentVersion, String latestVersion) {
        try {
            // 清理版本号，移除非数字字符（如 "7-Canary" -> "7"）
            String cleanCurrent = currentVersion.replaceAll("[^0-9.]", "");
            String cleanLatest = latestVersion.replaceAll("[^0-9.]", "");

            // 如果清理后为空，使用原始版本号
            if (cleanCurrent.isEmpty()) cleanCurrent = currentVersion;
            if (cleanLatest.isEmpty()) cleanLatest = latestVersion;

            String[] currentParts = cleanCurrent.split("\\.");
            String[] latestParts = cleanLatest.split("\\.");

            int maxLength = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = 0;
                int latestPart = 0;

                // 安全解析每个版本部分
                if (i < currentParts.length) {
                    try {
                        currentPart = Integer.parseInt(currentParts[i]);
                    } catch (NumberFormatException e) {
                        currentPart = 0; // 解析失败时设为0
                    }
                }

                if (i < latestParts.length) {
                    try {
                        latestPart = Integer.parseInt(latestParts[i]);
                    } catch (NumberFormatException e) {
                        latestPart = 0; // 解析失败时设为0
                    }
                }

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e("UpdateChecker", "Error comparing versions: " + currentVersion + " vs " + latestVersion, e);
            return false; // 比较失败时认为不需要更新
        }
    }

    private void showUpdateDialog(final JSONObject updateJson) {
        try {
            String title = updateJson.optString("title", "发现新版本");
            String content = updateJson.optString("content", "有新版本可用，建议更新以获得更好的体验。");
            final String downloadUrl = updateJson.optString("downloadUrl", "");
            final boolean forceUpdate = updateJson.optBoolean("forceUpd", false);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title);
            builder.setMessage(content);
            builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!downloadUrl.isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Log.e("UpdateChecker", "Error opening download URL", e);
                        }
                    }
                }
            });

            if (forceUpdate) {
                builder.setCancelable(false);
            } else {
                builder.setNegativeButton("稍后再说", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            }

            if (!((Activity) context).isFinishing()) {
                builder.show();
            }
        } catch (Exception e) {
            Log.e("UpdateChecker", "Error showing update dialog", e);
        }
    }
}