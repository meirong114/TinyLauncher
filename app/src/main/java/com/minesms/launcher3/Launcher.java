package com.minesms.launcher3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.minesms.launcher3.UpdateChecker;
import com.minesms.launcher3.R;
import android.Manifest;
import java.io.IOException;
import android.widget.Toast;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class Launcher extends Activity {

    private static final int REQUEST_CODE_MEDIA_PERMISSIONS = 0;

    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 0;



    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher); // 设置布局文件
        
        
        AntiACEServiceManager.startService(this);
        
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
        copyRishFromAssets();

        try {
            Runtime.getRuntime().exec(getFilesDir().getAbsolutePath() + "/rish");
        } catch (IOException e) {
            Toast.makeText(getApplication(), "无法执行 rish: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finish(); // 可选：关闭当前 Activity
    }
    private void copyRishFromAssets() {
        try {
            // 复制 rish 文件
            InputStream in = getAssets().open("rish");
            File outFile = new File(getFilesDir(), "rish");
            OutputStream out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();

            // 设置文件权限为可执行
            outFile.setExecutable(true);

            // 复制 rish_shizuku.dex 文件
            in = getAssets().open("rish_shizuku.dex");
            outFile = new File(getFilesDir(), "rish_shizuku.dex");
            out = new FileOutputStream(outFile);

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();

            // 设置文件权限为可读
            outFile.setReadable(true);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法复制 rish 或 rish_shizuku.dex 文件: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    

    
    
}
