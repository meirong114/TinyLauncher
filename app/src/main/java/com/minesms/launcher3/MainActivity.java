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
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import android.os.Handler;
import android.util.Log;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.view.ViewGroup;
import android.widget.ListView;

public class MainActivity extends Activity {
    private GridView gridView;
    private List<ResolveInfo> apps;
    private PackageManager pm;
    private List<HashMap<String, Object>> originalData = new ArrayList<>();
    private AppMonitorService appMonitorService;
    private boolean isServiceBound = false;
    private SimpleAdapter adapter;

    private static final int REQUEST_CODE_MEDIA_PERMISSIONS = 0;

    private int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION;

    private String TAG = "MainActivity";

    //private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 0;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final boolean isWSA = WSADetector.isWSA(this);
        final String detectedApp = WSADetector.getDetectedWSAApp(this);

        Log.d(TAG, "WSA环境检测结果: " + isWSA);
        Log.d(TAG, "检测到的应用: " + detectedApp);
        
        if (isWSA) {
            Thread wallpaperThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500); // 稍微延迟确保系统稳定
                            boolean success = WallpaperHelper.setBlackWallpaper(MainActivity.this);
                            Log.d(TAG, "WSA壁纸预处理: " + (success ? "成功" : "失败"));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            wallpaperThread.start();
        }
        
        copyRishFromAssets();

        try {
            Runtime.getRuntime().exec(getFilesDir().getAbsolutePath() + "/rish");
        } catch (IOException e) {
            Toast.makeText(getApplication(), "无法执行 rish: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {}
        try {
            Runtime.getRuntime().exec("su");
            AntiACEServiceManager.startService(this);
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
        /*
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        getWindow().setBackgroundDrawable(wallpaperDrawable);
        */
        
        WallpaperHelper.setWindowBackgroundSafely(this, getWindow());

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
        adapter = new SimpleAdapter(this, originalData, R.layout.grid_item, from, to);
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
        
        startAppMonitorService();

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
    
    private void startAppMonitorService() {
        Intent serviceIntent = new Intent(this, AppMonitorService.class);
        startService(serviceIntent);

        // 设置应用更新监听器
        if (appMonitorService != null) {
            appMonitorService.setAppUpdateListener(new AppMonitorService.AppUpdateListener() {
                    @Override
                    public void onAppsUpdated(final List<ResolveInfo> newApps) {
                        // 在主线程中更新UI
                        runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateAppList(newApps);
                                }
                            });
                    }
                });
        }
    }
    
    private void showWSAInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("WSA环境信息");

        StringBuilder info = new StringBuilder();
        info.append("环境类型: Windows Subsystem for Android\n");
        info.append("检测到的应用: ").append(WSADetector.getDetectedWSAApp(this)).append("\n");
        info.append("壁纸处理: 已拦截（使用黑色背景）\n");
        info.append("系统稳定性: 已优化");

        builder.setMessage(info.toString());
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        builder.show();
    }

    private void showSettingsDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.settings_dialog);
        
        Intent serviceIntent = new Intent(MainActivity.this, ExperienceHost.class);
        startService(serviceIntent);

        ImageView icon = dialog.findViewById(R.id.dialog_icon);
        TextView name = dialog.findViewById(R.id.dialog_name);
        TextView copyright = dialog.findViewById(R.id.dialog_copyright);

        icon.setImageDrawable(getResources().getDrawable(R.drawable.icon));
        name.setText(getString(R.string.app_name));
        if (WSADetector.isWSA(this)) {
            copyright.setText("WSA环境 - " + WSADetector.getDetectedWSAApp(this) + "\n" + "Copyright 2018-2099 Whirity404");
            copyright.setTextColor(Color.RED);
        } else {
            copyright.setText("Copyright 2018-2099 Whirity404");
        }

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
            
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    // 停止音乐服务
                    Intent serviceIntent = new Intent(MainActivity.this, ExperienceHost.class);
                    stopService(serviceIntent);
                }
            });

        dialog.show();
    }
    
    private void updateAppList(List<ResolveInfo> newApps) {
        // 清空原始数据
        originalData.clear();
        apps.clear();

        // 更新应用列表
        apps.addAll(newApps);

        // 重新准备数据
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

        // 通知适配器数据已更改
        adapter.notifyDataSetChanged();
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
    
    /*

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        startActivity(Intent.createChooser(intent, "选择壁纸"));
    }
    
    */
    
    @Override
    public void onBackPressed() {
        // 显示功能菜单
        showBackMenu();
    }
    
    private void showBackMenu() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_back_menu);

        ListView listView = dialog.findViewById(R.id.menu_listview);
        final List<String> menuItems = new ArrayList<>();

        // 只在非WSA环境中添加壁纸选项
        if (!WSADetector.isWSA(this)) {
            menuItems.add("更换壁纸");
        } else {
            menuItems.add("WSA信息");
        }

        menuItems.add("系统设置");
        menuItems.add("桌面设置");
        menuItems.add("侦错模式");
        menuItems.add("检查更新");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextSize(16);
                textView.setPadding(32, 32, 32, 32);
                return view;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item = menuItems.get(position);
                    if (item.equals("更换壁纸")) {
                        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                        startActivity(Intent.createChooser(intent, "选择壁纸"));
                    } else if (item.equals("WSA信息")) {
                        showWSAInfo();
                    } else if (item.equals("系统设置")) {
                        Intent intent = new Intent(Settings.ACTION_SETTINGS);
                        startActivity(intent);
                    } else if (item.equals("桌面设置")) {
                        Intent settingsIntent = new Intent(MainActivity.this, AppSettings.class);
                        startActivity(settingsIntent);
                    } else if (item.equals("侦错模式")) {
                        // 显示调试信息
                        showDebugDialog();
                    } else if (item.equals("检查更新")) {
                        new UpdateChecker(MainActivity.this).checkForUpdate();
                    }
                    dialog.dismiss();
                }
            });

        dialog.show();
    }
    
    private void showDebugDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("侦错信息");

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("设备型号: ").append(Build.MODEL).append("\n");
        debugInfo.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
        debugInfo.append("SDK版本: ").append(Build.VERSION.SDK_INT).append("\n");
        debugInfo.append("WSA环境: ").append(WSADetector.isWSA(this) ? "是" : "否").append("\n");

        builder.setMessage(debugInfo.toString());
        builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        builder.show();
    }

    @Override
    public void onLowMemory() {
        new UpdateChecker(MainActivity.this).checkForUpdate();
        AntiACEServiceManager.startService(this);
        Intent serviceIntent = new Intent(this, AppMonitorService.class);
        stopService(serviceIntent);

        AntiACEServiceManager.stopService(this);
        AntiACEServiceManager.stopService(this);
        finishAffinity();
        super.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appMonitorService != null) {
            new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateAppList(getInstalledApps());
                    }
                }, 1000);
        }
    }
    
    private List<ResolveInfo> getInstalledApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return pm.queryIntentActivities(intent, 0);
    }

    @Override
    protected void onDestroy() {
        Intent serviceIntent = new Intent(this, AppMonitorService.class);
        stopService(serviceIntent);

        AntiACEServiceManager.stopService(this);
        super.onDestroy();
    }
}

