package com.minesms.launcher3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.minesms.launcher3.UpdateChecker;
import com.minesms.launcher3.R;
import android.Manifest;


public class Launcher extends Activity {

    private static final int REQUEST_CODE_MEDIA_PERMISSIONS = 0;

    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 0;



    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher); // 设置布局文件
        
        String[] permissions_media = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        };
        //requestPermissions(permissions_media, REQUEST_CODE_MEDIA_PERMISSIONS);
        
        String[] permissions_noMedia = {
            Manifest.permission.READ_EXTERNAL_STORAGE
        };
        requestPermissions(permissions_noMedia, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);
        
        new UpdateChecker(Launcher.this).checkForUpdate();

        // 跳转到 MainActivity
        Intent intent = new Intent(Launcher.this, MainActivity.class);
        startActivity(intent);
        finish(); // 可选：关闭当前 Activity
    }
}
