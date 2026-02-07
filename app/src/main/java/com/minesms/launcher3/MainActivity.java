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
import android.support.annotation.NonNull;
import android.widget.*;
import com.minesms.launcher3.ApkExtractor;
import com.minesms.launcher3.UpdateChecker;
import android.provider.Settings;
import android.view.View;
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
import android.view.ViewGroup;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.Context;
import android.os.Environment;
//import android.annotation.NonNull;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;

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

    private int REQUEST_STORAGE_PERMISSION;

    //private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 0;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppMonitorService.LocalBinder binder = (AppMonitorService.LocalBinder) service;
            appMonitorService = binder.getService();
            // 设置应用更新监听器
            appMonitorService.setAppUpdateListener(new AppMonitorService.AppUpdateListener() {
                    @Override
                    public void onAppsUpdated(final List<ResolveInfo> newApps) {
                        runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateAppList(newApps);
                                }
                            });
                    }
                });
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            appMonitorService = null;
            isServiceBound = false;
        }
    };
    
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


        String[] permissions_noMedia = {
            Manifest.permission.READ_EXTERNAL_STORAGE
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions_noMedia, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);
        }

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

        // 对应用列表进行排序（Aa-Zz顺序）
        sortAppsByName(apps);

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
                    if (view instanceof ImageView && data instanceof Drawable) {
                        ((ImageView) view).setImageDrawable((Drawable) data);
                        return true;
                    }
                    return false;
                }
            });

        gridView.setAdapter(adapter);
        
        startAppMonitorService();

        // 设置单击事件
        // 修改应用点击事件处理
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position == apps.size()) {
                        Intent settingsIntent = new Intent(MainActivity.this, AppSettings.class);
                        try {
                            startActivity(settingsIntent);
                        } catch (Exception f) {
                            Toast.makeText(getApplication(), "无法打开设置", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        ResolveInfo app = apps.get(position);
                        if (app.activityInfo.exported) {
                            Intent launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                            if (launchIntent != null) {
                                try {
                                    startActivity(launchIntent);
                                } catch (Exception e) {
                                    // 应用可能被停用，尝试其他启动方式
                                    try {
                                        Intent appIntent = new Intent(Intent.ACTION_MAIN);
                                        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                                        appIntent.setClassName(app.activityInfo.packageName, app.activityInfo.name);
                                        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(appIntent);
                                    } catch (Exception ex) {
                                        Toast.makeText(MainActivity.this, 
                                                       "无法启动应用，可能已被停用", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "启动应用失败: " + app.activityInfo.packageName, ex);
                                    }
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "该应用无法启动", Toast.LENGTH_SHORT).show();
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

    private void sortAppsByName(List<ResolveInfo> appList) {
        java.util.Collections.sort(appList, new java.util.Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo app1, ResolveInfo app2) {
                    String name1 = app1.loadLabel(pm).toString().toLowerCase();
                    String name2 = app2.loadLabel(pm).toString().toLowerCase();
                    return name1.compareTo(name2);
                }
            });
    }
    
    private void startAppMonitorService() {
        Intent serviceIntent = new Intent(this, AppMonitorService.class);

        // 绑定服务而不是简单启动
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 移除原来的监听器设置代码
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

        // 对应用列表进行排序
        sortAppsByName(apps);

        // 重新准备数据
        for (ResolveInfo app : apps) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("icon", app.loadIcon(pm));
            map.put("name", app.loadLabel(pm).toString());
            originalData.add(map);
        }

        // 添加设置图标（放在最后）
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

        // 获取搜索相关视图
        final android.widget.EditText searchEditText = dialog.findViewById(R.id.search_edittext);
        final ListView searchResultsListView = dialog.findViewById(R.id.search_results_listview);
        final ListView menuListView = dialog.findViewById(R.id.menu_listview);
        final View searchResultsContainer = dialog.findViewById(R.id.search_results_container);

        // 初始化搜索功能
        setupSearchFunctionality(searchEditText, searchResultsListView, searchResultsContainer, dialog);

        // 菜单项列表
        final List<String> menuItems = new ArrayList<>();
        if (!WSADetector.isWSA(this)) {
            menuItems.add("更换壁纸");
        } else {
            menuItems.add("WSA信息");
        }
        menuItems.add("系统设置");
        menuItems.add("桌面设置");
        menuItems.add("侦错模式");
        menuItems.add("检查更新");
        menuItems.add("安装测试用App");

        ArrayAdapter<String> menuAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextSize(16);
                textView.setPadding(32, 32, 32, 32);
                return view;
            }
        };

        menuListView.setAdapter(menuAdapter);

        // 菜单项点击事件
        menuListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = menuItems.get(position);
                handleMenuItemClick(item, dialog);
            }
        });

        dialog.show();
    }


    private void setupSearchFunctionality(final android.widget.EditText searchEditText,
                                          final ListView searchResultsListView,
                                          final View searchResultsContainer,
                                          final Dialog dialog) {

        // 搜索文本变化监听器
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String searchText = s.toString().trim();
                if (searchText.isEmpty()) {
                    // 清空搜索框时隐藏搜索结果
                    searchResultsContainer.setVisibility(View.GONE);
                } else {
                    // 有搜索内容时显示搜索结果
                    searchResultsContainer.setVisibility(View.VISIBLE);
                    performSearch(searchText, searchResultsListView, dialog);
                }
            }
        });

        // 初始化搜索结果列表为隐藏
        searchResultsContainer.setVisibility(View.GONE);
    }

    private void performSearch(String searchText, ListView searchResultsListView, final Dialog dialog) {
        List<ResolveInfo> searchResults = new ArrayList<>();

        // 获取所有应用并搜索
        List<ResolveInfo> allApps = getInstalledApps();

        for (ResolveInfo app : allApps) {
            String appName = app.loadLabel(pm).toString().toLowerCase();
            String packageName = app.activityInfo.packageName.toLowerCase();

            // 搜索应用名称或包名
            if (appName.contains(searchText.toLowerCase()) ||
                    packageName.contains(searchText.toLowerCase())) {
                searchResults.add(app);
            }
        }

        // 创建搜索结果的适配器
        SearchResultsAdapter adapter = new SearchResultsAdapter(this, searchResults);
        searchResultsListView.setAdapter(adapter);

        // 搜索结果点击事件
        searchResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ResolveInfo selectedApp = (ResolveInfo) parent.getItemAtPosition(position);
                launchApp(selectedApp);
                dialog.dismiss(); // 启动应用后关闭对话框
            }
        });
    }

    private class SearchResultsAdapter extends ArrayAdapter<ResolveInfo> {
        private LayoutInflater inflater;

        public SearchResultsAdapter(Context context, List<ResolveInfo> apps) {
            super(context, 0, apps);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_search_result, parent, false);
                holder = new ViewHolder();
                holder.appIcon = convertView.findViewById(R.id.search_app_icon);
                holder.appName = convertView.findViewById(R.id.search_app_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ResolveInfo app = getItem(position);
            if (app != null) {
                holder.appIcon.setImageDrawable(app.loadIcon(pm));
                holder.appName.setText(app.loadLabel(pm));
            }

            return convertView;
        }

        private class ViewHolder {
            ImageView appIcon;
            TextView appName;
        }
    }

    private void setInitialFocus(final Dialog dialog, final ListView menuListView, final EditText searchEditText) {
        // 延迟设置焦点，确保视图已经完全加载
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing() && menuListView != null) {
                    try {
                        // 首先尝试将焦点设置到菜单列表
                        if (menuListView.getChildCount() > 0) {
                            menuListView.requestFocus();
                            menuListView.setSelection(0);

                            // 可选：高亮显示第一个菜单项
                            if (menuListView.getChildAt(0) != null) {
                                menuListView.getChildAt(0).setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                            }
                        }

                        // 确保搜索框没有焦点
                        if (searchEditText != null) {
                            searchEditText.clearFocus();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "设置焦点失败", e);
                    }
                }
            }
        }, 150);
    }

    private void launchApp(ResolveInfo app) {
        if (app.activityInfo.exported) {
            Intent launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
            if (launchIntent != null) {
                try {
                    startActivity(launchIntent);
                } catch (Exception e) {
                    try {
                        Intent appIntent = new Intent(Intent.ACTION_MAIN);
                        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                        appIntent.setClassName(app.activityInfo.packageName, app.activityInfo.name);
                        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(appIntent);
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this,
                                "无法启动应用，可能已被停用", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "启动应用失败: " + app.activityInfo.packageName, ex);
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "该应用无法启动", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "该应用的Activity未导出，无法启动", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMenuItemClick(String item, Dialog dialog) {
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
            showDebugDialog();
        } else if (item.equals("检查更新")) {
            new UpdateChecker(MainActivity.this).checkForUpdate();
        } else if (item.equals("安装测试用App")) {
            checkAndRequestStoragePermission();
        }
        dialog.dismiss();
    }


    private void checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            installStubApp();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //copyApkFromAssetsToDownloadDir();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void installStubApp() {
        
        copyApkFromAssetsToDownloadDir();
        
    }
    
    private void copyApkFromAssetsToDownloadDir() {
        String apkName = "stub.apk"; // APK文件名
        String destinationPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + apkName;

        try {
            try (InputStream in = getAssets().open(apkName);
            OutputStream out = new FileOutputStream(destinationPath)) {

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                Toast.makeText(this, "APK copied successfully", Toast.LENGTH_SHORT).show();
                installApk(destinationPath);
            }
        } catch (IOException e) {} catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to copy APK", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void installApk(String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity();
        }
        super.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateAppList(getInstalledApps());
                }
            }, 1500);
    }
    
    private List<ResolveInfo> getInstalledApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = pm.queryIntentActivities(intent, 0);

        // 对列表进行排序
        sortAppsByName(appList);

        return appList;
    }

    @Override
    protected void onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }

        AntiACEServiceManager.stopService(this);
        super.onDestroy();
    }
}

