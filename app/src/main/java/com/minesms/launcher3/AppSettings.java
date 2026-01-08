package com.minesms.launcher3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.minesms.launcher3.R;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.widget.Toast;
import java.io.IOException;


public class AppSettings extends Activity {



    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher); // 设置布局文件

        // 跳转到 MainActivity
        Intent intent = new Intent(AppSettings.this, Settings.class);
        startActivity(intent);
        copyRishFromAssets();
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

            try {
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {}
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
