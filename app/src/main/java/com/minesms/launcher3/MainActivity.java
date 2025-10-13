package com.minesms.launcher3;

import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.minesms.launcher3.UpdateChecker;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.minesms.launcher3.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import android.view.LayoutInflater;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.webkit.WebView;
import android.Manifest;

public class MainActivity extends Activity {
    private GridView gridView;
    private List<ResolveInfo> apps;
    private PackageManager pm;
    private List<HashMap<String, Object>> originalData = new ArrayList<>();

    private static final int REQUEST_CODE_MEDIA_PERMISSIONS = 0;

    private int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION;

    //private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {}
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {}
        
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

        // 设置墙纸为背景
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        getWindow().setBackgroundDrawable(wallpaperDrawable);

        setContentView(R.layout.activity_main);

        gridView = findViewById(R.id.gridView);
        pm = getPackageManager();

        // 获取已安装的应用列表
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = pm.queryIntentActivities(intent, 0);

        // 准备数据
        for (ResolveInfo app : apps) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("icon", app.loadIcon(pm));
            map.put("name", app.loadLabel(pm).toString());
            originalData.add(map);
        }

        // 添加设置图标
        HashMap<String, Object> settingsItem = new HashMap<>();
        settingsItem.put("icon", getResources().getDrawable(R.drawable.icon_settings));
        settingsItem.put("name", getString(R.string.app_settings));
        originalData.add(settingsItem);

        // 创建适配器
        String[] from = {"icon", "name"};
        int[] to = {R.id.icon, R.id.name};
        SimpleAdapter adapter = new SimpleAdapter(this, originalData, R.layout.grid_item, from, to);
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data, String textRepresentation) {
                    if (view instanceof ImageView && data instanceof android.graphics.drawable.Drawable) {
                        ((ImageView) view).setImageDrawable((android.graphics.drawable.Drawable) data);
                        return true;
                    }
                    return false;
                }
            });

        gridView.setAdapter(adapter);

        // 设置单击事件
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position == apps.size()) { // 判断是否点击了设置图标
                        //Intent settingsIntent = new Intent("com.minesms.launcher3.action.SETTINGS");
                        Intent settingsIntent = new Intent(MainActivity.this, AppSettings.class);
                        startActivity(settingsIntent);
                    } else {
                        ResolveInfo app = apps.get(position);
                        if (app.activityInfo.exported) {
                            Intent launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                            if (launchIntent != null) {
                                startActivity(launchIntent);
                            } else {
                                Intent appIntent = new Intent(Intent.ACTION_MAIN);
                                appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                                appIntent.setClassName(app.activityInfo.packageName, app.activityInfo.name);
                                startActivity(appIntent);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "该应用的Activity未导出，无法启动", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

        // 设置长按事件
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position == apps.size()) { // 判断是否长按了设置图标
                        showSettingsDialog();
                    } else {
                        ResolveInfo app = apps.get(position);
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + app.activityInfo.packageName));
                        startActivity(intent);
                        return true;
                    }
                    return true;
                }
            });
    }

    private void showSettingsDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.settings_dialog);

        ImageView icon = dialog.findViewById(R.id.dialog_icon);
        TextView name = dialog.findViewById(R.id.dialog_name);
        TextView copyright = dialog.findViewById(R.id.dialog_copyright);

        icon.setImageDrawable(getResources().getDrawable(R.drawable.icon));
        name.setText(getString(R.string.app_name));
        copyright.setText("Copyright 2018-2099 Whirity404");

        // 添加点击事件监听器
        icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 随机生成旋转角度（0-200度）
                    Random random = new Random();
                    int angle = random.nextInt(501); // 0到200的随机数
                    // 随机选择顺时针或逆时针旋转
                    if (random.nextBoolean()) {
                        angle = -angle; // 逆时针旋转
                    }
                    // 执行旋转动画
                    v.animate().rotationBy(angle).setDuration(150).start();
                }
            });

        dialog.show();
    }
    
    public void onCopyrightClick(View view) {
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
                    dialog.dismiss();
                }
            })
            .create()
            .show();
    }
    
    public void checkUpdTxt(View view) {
        new UpdateChecker(MainActivity.this).checkForUpdate();
    }
    
    public void onIconClick(View view) {
        showSettingsDialog();
    }
    
    public void onNameClick(View view) {
        new UpdateChecker(MainActivity.this).checkForUpdate();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        startActivity(Intent.createChooser(intent, "选择壁纸"));
    }

    @Override
    public void onLowMemory() {
        new UpdateChecker(MainActivity.this).checkForUpdate();
        finishAffinity();
        super.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
}

