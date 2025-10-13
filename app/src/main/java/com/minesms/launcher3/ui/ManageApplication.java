package com.minesms.launcher3.ui;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ManageApplication extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建 Intent 以打开应用信息界面
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:com.minesms.launcher3"));
        startActivity(intent);

        // 结束当前 Activity
        finish();
    }
}
