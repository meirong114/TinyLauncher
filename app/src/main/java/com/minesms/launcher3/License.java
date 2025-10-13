package com.minesms.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
public class License extends Activity {


    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建并显示包含 WebView 的提示框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_license, null);
        builder.setView(dialogView);

        WebView licenseWebView = dialogView.findViewById(R.id.licenseWebView);
        licenseWebView.loadUrl("file:///android_asset/LICENSE.txt");

        builder.setTitle("开源许可证")
                .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 关闭当前Activity
                        finish();
                    }
                })
                .create()
                .show();
    }
}
