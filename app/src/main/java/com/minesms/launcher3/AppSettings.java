package com.minesms.launcher3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.minesms.launcher3.R;


public class AppSettings extends Activity {



    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher); // 设置布局文件

        // 跳转到 MainActivity
        Intent intent = new Intent(AppSettings.this, Settings.class);
        startActivity(intent);
        finish(); // 可选：关闭当前 Activity
    }
}
