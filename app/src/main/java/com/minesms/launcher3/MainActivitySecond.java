package com.minesms.launcher3;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.minesms.launcher3.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivitySecond extends Activity {
    private GridView gridView;
    private List<ResolveInfo> apps;
    private PackageManager pm;
    private List<ResolveInfo> pinnedApps = new ArrayList<>(); // 存储置顶的应用
    private List<HashMap<String, Object>> originalData = new ArrayList<>();

    private static final int REQUEST_OVERLAY_PERMISSION = 0; // 存储原始数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {}
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {}

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

        // 加载置顶应用
        loadPinnedApps();

        // 准备数据
        for (ResolveInfo app : apps) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("icon", app.loadIcon(pm));
            map.put("name", app.loadLabel(pm).toString());
            originalData.add(map);
        }

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
                    ResolveInfo app = apps.get(position);
                    if (app.activityInfo.exported) {
                        Intent launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                        if (launchIntent != null) {
                            startActivity(launchIntent);
                        } else {
                            // 如果没有主类，启动对应的 Activity
                            Intent appIntent = new Intent(Intent.ACTION_MAIN);
                            appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            appIntent.setClassName(app.activityInfo.packageName, app.activityInfo.name);
                            startActivity(appIntent);
                        }
                    } else {
                        // 处理未导出的 Activity，例如显示一个提示
                        Toast.makeText(MainActivitySecond.this, "该应用的Activity未导出，无法启动，如果您要了解，请找：百度", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        // 设置长按事件
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    ResolveInfo app = apps.get(position);
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + app.activityInfo.packageName));
                    startActivity(intent);
                    return true;
                }
            });

        if (!Settings.canDrawOverlays(this)) {
            Intent intentaa = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intentaa, REQUEST_OVERLAY_PERMISSION);
        }
        if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, KeepOnService.class));
        } else {
            Intent intentaa = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intentaa, REQUEST_OVERLAY_PERMISSION);
        }
    }

    private void updateGridView() {
        List<HashMap<String, Object>> data = new ArrayList<>(originalData); // 复制原始数据
        List<ResolveInfo> updatedApps = new ArrayList<>(apps); // 复制原始应用列表

        // 添加置顶的应用到列表的前面
        for (ResolveInfo app : pinnedApps) {
            for (int i = 0; i < originalData.size(); i++) {
                HashMap<String, Object> map = originalData.get(i);
                if (map.get("name").equals(app.loadLabel(pm).toString())) {
                    data.remove(map);
                    data.add(0, map);
                    updatedApps.remove(app);
                    updatedApps.add(0, app);
                    break;
                }
            }
        }

        apps = updatedApps; // 更新应用列表

        String[] from = {"icon", "name"};
        int[] to = {R.id.icon, R.id.name};
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.grid_item, from, to);
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
    }

    private void savePinnedApps() {
        SharedPreferences prefs = getSharedPreferences("pinned_apps", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        HashSet<String> pinnedAppSet = new HashSet<>();
        for (ResolveInfo app : pinnedApps) {
            pinnedAppSet.add(app.activityInfo.packageName);
        }
        editor.putStringSet("pinned_apps", pinnedAppSet);
        editor.apply();
    }

    private void loadPinnedApps() {
        SharedPreferences prefs = getSharedPreferences("pinned_apps", MODE_PRIVATE);
        Set<String> pinnedAppSet = prefs.getStringSet("pinned_apps", new HashSet<>());
        for (ResolveInfo app : apps) {
            if (pinnedAppSet.contains(app.activityInfo.packageName)) {
                pinnedApps.add(app);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        startActivity(Intent.createChooser(intent, "选择壁纸"));
    }
}

