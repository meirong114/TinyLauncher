package com.minesms.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import android.widget.Toast;

public class Settings extends Activity {
    private static final String UPDATE_CLEAR_URL = "https://purge.jsdelivr.net/gh/meirong114/TinyLauncher@latest/update.json";
    private long firstBackTime;
    @Override
    public void onBackPressed() {
        finish();
    }
    
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        try {
            Runtime.getRuntime().exec(getFilesDir().getAbsolutePath() + "/rish");
        } catch (IOException e) {
            Toast.makeText(getApplication(), "无法执行 rish: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // 获取版本号字符串并设置到 TextView
        TextView versionTextView = findViewById(R.id.versionTextView);
        versionTextView.setText(getString(R.string.version));
        
        Button btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intentAbout = new Intent(Settings.this, AboutActivity.class);
                    startActivity(intentAbout);
                }
                
            
        });

        // 设置按钮点击事件
        Button licenseButton = findViewById(R.id.licenseButton);
        licenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicenseDialog();
            }
        });
        
        Button exitBtn = findViewById(R.id.exitBtn);
        exitBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finishAffinity();
                }
                
            
        });
        
        Button startAppBtn = findViewById(R.id.startAppBtn);
        startAppBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent startIntent = new Intent(Settings.this,Launcher.class);
                    startActivity(startIntent);
                    finish();
                }
            });
            
        Button backToLauncher = findViewById(R.id.backToLauncher);
        backToLauncher.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
                    finishActivity(0);
                    finish();
                }
                
            
        });
        
        Button devBtn = findViewById(R.id.devBtn);
        devBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intentDev = new Intent("com.minesms.launcher3.DEVSET");
                    startActivity(intentDev);
                    
                }
                
            
        });
        
        Button clearUpdateCache = findViewById(R.id.clearUpdateCache);
        clearUpdateCache.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //URL clearUrl = new URL(UPDATE_CLEAR_URL);
                    try {
                        URL clearUrl = new URL(UPDATE_CLEAR_URL);
                        HttpURLConnection clearConnection = (HttpURLConnection) clearUrl.openConnection();
                        clearConnection.setRequestMethod("GET");
                        clearConnection.setConnectTimeout(5000);
                        clearConnection.setReadTimeout(5000);
                    } catch (IOException e) {}
                    
                }
                
            
        });
            
        Button appInfoBtn = findViewById(R.id.appInfoBtn);
        appInfoBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intentInfo = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intentInfo.setData(Uri.parse("package:com.minesms.launcher3"));
                    startActivity(intentInfo);
                }
                
            
        });
    }

    private void showLicenseDialog() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
        // 创建并显示包含 WebView 的提示框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_license, null);
        builder.setView(dialogView);

        WebView licenseWebView = dialogView.findViewById(R.id.licenseWebView);
        try {
            Thread.sleep(20);
            licenseWebView.loadUrl("file://android_asset/LICENSE.txt");
            Thread.sleep(20);
        } catch (InterruptedException e) {}
        licenseWebView.loadUrl("file:///android_asset/LICENSE.txt");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}

        builder.setTitle("开源许可证")
                .setPositiveButton("关闭", null)
                .create()
                .show();
    }
    

}
