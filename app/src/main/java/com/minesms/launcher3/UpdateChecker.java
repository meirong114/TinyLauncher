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
        new CheckUpdateTask().execute();
    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, String> {

        private Exception e;

        private String currentVersionParts;
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(UPDATE_JSON_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                URL clearUrl = new URL(UPDATE_CLEAR_URL);
                HttpURLConnection clearConnection = (HttpURLConnection) clearUrl.openConnection();
                clearConnection.setRequestMethod("GET");
                clearConnection.setConnectTimeout(5000);
                clearConnection.setReadTimeout(5000);

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
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject updateJson = new JSONObject(result);
                    String latestVersion = updateJson.getString("version");
                    String currentVersion = getCurrentVersion();
                    if (isNewVersionAvailable(currentVersion, latestVersion)) {
                        showUpdateDialog(updateJson);
                    }
                } catch (Exception e) {
                    Log.e("UpdateChecker", "Error parsing update JSON", e);
                    showNetworkErrorDialog(e);
                }
            } else {
                showNetworkErrorDialog(e);
            }
        }
        private void showNetworkErrorDialog(Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("网络错误");
            String error = "无法连接到更新服务器，请检查您的网络连接。" + e;
            builder.setMessage("无法连接到更新服务器，请检查您的网络连接。\n\nErroor:  " + e + "(" + error + ")" + "\n\n\nVersion:" + currentVersionParts);
            builder.setPositiveButton("检查网络", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        context.startActivity(intent);
                    }
                });
            builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            builder.setCancelable(false);
            builder.show();
        }
    }

    private String getCurrentVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UpdateChecker", "Error getting current version", e);
            return "1.0.0"; // Default version if error occurs
        }
    }

    private boolean isNewVersionAvailable(String currentVersion, String latestVersion) {
        String[] currentVersionParts = currentVersion.split("\\.");
        String[] latestVersionParts = latestVersion.split("\\.");

        for (int i = 0; i < Math.max(currentVersionParts.length, latestVersionParts.length); i++) {
            int currentPart = i < currentVersionParts.length ? Integer.parseInt(currentVersionParts[i]) : 0;
            int latestPart = i < latestVersionParts.length ? Integer.parseInt(latestVersionParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private void showUpdateDialog(final JSONObject updateJson) {
        String title = updateJson.optString("title", "Update Available");
        String content = updateJson.optString("content", "A new update is available.");
        final String downloadUrl = updateJson.optString("downloadUrl", "");
        final boolean forceUpdate = updateJson.optBoolean("forceUpd", false);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    context.startActivity(intent);
                }
            });

        if (forceUpdate) {
            builder.setCancelable(false);
            builder.setNegativeButton(null, null);
        } else {
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        }

        builder.show();
    }

    
}

