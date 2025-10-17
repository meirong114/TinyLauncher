package com.minesms.launcher3;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AntiACEService extends Service {
    private static final String TAG = "GameSecurityService";

    // 已知带有反作弊的游戏包名列表
    private static final Set<String> BLOCKED_GAME_PACKAGES = new HashSet<>();

    // 排除列表 - 这些游戏不会触发警告
    private static final Set<String> EXCLUDED_PACKAGES = new HashSet<>();

    static {
        // 初始化排除列表
        addExcludedPackages();
        // 初始化检测列表
        addTencentGames();
        addMihoyoGames();
        addOtherGames();
    }

    private static void addExcludedPackages() {
        // 明日方舟系列
        String[] excludedGames = {
            "com.hypergryph.arknights",    // 明日方舟官方
            "com.YoStarEN.Arknights",      // 明日方舟国际版
            "com.YoStarJP.Arknights",      // 明日方舟日服
            "com.YoStarKR.Arknights",      // 明日方舟韩服
            "com.arknights.bilibili",      // 明日方舟B服
            "com.tencent.arknights",       // 明日方舟腾讯版

            // 我的世界国际版
            "com.mojang.minecraftpe",      // 我的世界基岩版
            "com.mojang.minecraft",        // 我的世界Java版（如果有）
            "com.mojang.mcpe",             // 我的世界教育版
            "com.mojang",                  // Mojang其他应用

            // 其他想要排除的游戏
            "com.mojang.studios",          // Mojang工作室
            "com.mojang.realms"            // Minecraft Realms
        };
        for (String game : excludedGames) {
            EXCLUDED_PACKAGES.add(game);
        }
    }

    private static void addTencentGames() {
        String[] tencentGames = {
            "com.tencent.tmgp.sgame",      // 王者荣耀
            "com.tencent.tmgp.pubgm",      // 和平精英
            "com.tencent.ig",              // PUBG Mobile国际版
            "com.tencent.lolm",            // 英雄联盟手游
            "com.tencent.tmgp.cod",        // 使命召唤手游
            "com.tencent.tmgp.nba",        // NBA2K
            "com.tencent.tmgp.cf",         // 穿越火线
            "com.tencent.tmgp.arena",      // 火影忍者
            "com.tencent.newsgame",        // 天天酷跑
            "com.tencent.tmgp.dn",         // 地下城与勇士手游
            "com.tencent.tmgp.dby",        // 天涯明月刀手游
            "com.tencent.qqgame",          // QQ游戏
            "com.tencent.qqlive",          // 腾讯视频（可能包含游戏）
            "com.tencent.qqmusic"          // QQ音乐（可能包含游戏）
        };
        for (String game : tencentGames) {
            if (!EXCLUDED_PACKAGES.contains(game)) {
                BLOCKED_GAME_PACKAGES.add(game);
            }
        }
    }

    private static void addMihoyoGames() {
        String[] mihoyoGames = {
            "com.miHoYo.GenshinImpact",    // 原神
            "com.miHoYo.hkrpg",            // 崩坏：星穹铁道
            "com.mihoyo.hyperion",         // 崩坏3
            "com.miHoYo.honkai3rd",        // 崩坏3（国际版）
            "com.miHoYo.honkaiimpact3",    // 崩坏3（其他版本）
            "com.mihoyo.honkai",           // 崩坏学园2
            "com.miHoYo.tod",              // 未定事件簿
            "com.mihoyo.ys",               // 原神（其他版本）
            "com.miHoYo.Yuanshen",         // 原神（中文版）
            "com.HoYoverse.hkrpg",         // 崩坏：星穹铁道（国际版）
            "com.HoYoverse.GenshinImpact", // 原神（国际版）
            "com.mihoyo.account",          // 米哈游通行证
            "com.mihoyo.cloudgame"         // 云·原神
        };
        for (String game : mihoyoGames) {
            if (!EXCLUDED_PACKAGES.contains(game)) {
                BLOCKED_GAME_PACKAGES.add(game);
            }
        }
    }

    private static void addOtherGames() {
        String[] otherGames = {
            // 重返未来：1999
            "com.deepspace.r1999",
            "com.bluepoch.r1999",
            "com.bilibili.r1999",
            "r1999.deepspace",

            // 卡拉彼丘
            "com.herogame.klbp",
            "com.herogame.kalapiu",
            "com.klbp.herogame",
            "kalapiu.herogame",

            // 我的世界中国版（只检测网易版，排除国际版）
            "com.netease.mc",              // 网易我的世界
            "com.netease.mc.aligames",     // 我的世界阿里版
            "com.netease.x19",             // 我的世界（网易内部代号）

            // 其他可能冲突的游戏（排除明日方舟）
            "com.netease.h45",             // 网易其他游戏
            "com.netease.onmyoji",         // 阴阳师
            "com.bilibili.godgame",        // 碧蓝航线
            "com.YoStar.AetherGazer",      // 深空之眼
            "com.azurlane",                // 碧蓝航线
            "com.archosaur.ew",            // Evolutio 战争

            // 注意：明日方舟相关包名已从检测列表中移除
        };
        for (String game : otherGames) {
            if (!EXCLUDED_PACKAGES.contains(game)) {
                BLOCKED_GAME_PACKAGES.add(game);
            }
        }
    }

    // 提供静态方法获取检测列表（用于调试）
    public static Set<String> getBlockedGamePackages() {
        return new HashSet<>(BLOCKED_GAME_PACKAGES);
    }

    public static Set<String> getExcludedPackages() {
        return new HashSet<>(EXCLUDED_PACKAGES);
    }

    private AlertDialog mWarningDialog;
    private ScheduledExecutorService mScheduler;
    private Handler mMainHandler;
    private boolean mIsMonitoring = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GameSecurityService created");
        Log.d(TAG, "检测列表数量: " + BLOCKED_GAME_PACKAGES.size());
        Log.d(TAG, "排除列表数量: " + EXCLUDED_PACKAGES.size());
        mMainHandler = new Handler(Looper.getMainLooper());
        startMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startMonitoring() {
        if (mIsMonitoring) {
            return;
        }

        mIsMonitoring = true;
        mScheduler = Executors.newSingleThreadScheduledExecutor();

        // 每2秒检查一次
        mScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    checkBlockedGames();
                }
            }, 0, 2, TimeUnit.SECONDS);
    }

    private void checkBlockedGames() {
        final List<GameInfo> detectedGames = getRunningBlockedGames();

        if (!detectedGames.isEmpty() && mWarningDialog == null) {
            // 在主线程显示对话框
            mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showWarningDialog(detectedGames);
                    }
                });
        } else if (detectedGames.isEmpty() && mWarningDialog != null) {
            // 没有检测到冲突游戏，关闭对话框
            mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        closeWarningDialog();
                    }
                });
        } else if (!detectedGames.isEmpty() && mWarningDialog != null) {
            // 更新现有对话框
            mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateWarningDialog(detectedGames);
                    }
                });
        }
    }

    private List<GameInfo> getRunningBlockedGames() {
        List<GameInfo> runningGames = new ArrayList<>();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = getPackageManager();

        if (am != null) {
            // 获取正在运行的应用
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            if (runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    for (String pkgName : processInfo.pkgList) {
                        // 检查是否在检测列表中且不在排除列表中
                        if (BLOCKED_GAME_PACKAGES.contains(pkgName) && !EXCLUDED_PACKAGES.contains(pkgName)) {
                            try {
                                ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
                                String appName = pm.getApplicationLabel(appInfo).toString();
                                String gameType = getGameType(pkgName);

                                boolean alreadyExists = false;
                                for (GameInfo existing : runningGames) {
                                    if (existing.packageName.equals(pkgName)) {
                                        alreadyExists = true;
                                        break;
                                    }
                                }

                                if (!alreadyExists) {
                                    runningGames.add(new GameInfo(appName, pkgName, gameType));
                                    Log.d(TAG, "检测到冲突游戏: " + appName + " (" + pkgName + ")");
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                // 如果找不到应用信息，使用包名
                                String gameType = getGameType(pkgName);
                                runningGames.add(new GameInfo(pkgName, pkgName, gameType));
                                Log.d(TAG, "检测到冲突游戏(未知名称): " + pkgName);
                            }
                        } else if (EXCLUDED_PACKAGES.contains(pkgName)) {
                            Log.d(TAG, "跳过排除游戏: " + pkgName);
                        }
                    }
                }
            }
        }

        return runningGames;
    }

    private String getGameType(String packageName) {
        if (packageName.contains("tencent")) {
            return "腾讯游戏";
        } else if (packageName.contains("mihoyo") || packageName.contains("miHoYo") || packageName.contains("HoYoverse")) {
            return "米哈游游戏";
        } else if (packageName.contains("r1999") || packageName.contains("deepspace") || packageName.contains("bluepoch")) {
            return "重返未来1999";
        } else if (packageName.contains("klbp") || packageName.contains("kalapiu") || packageName.contains("herogame")) {
            return "卡拉彼丘";
        } else if (packageName.contains("netease") || packageName.contains("mc")) {
            return "我的世界中国版";
        } else {
            return "其他游戏";
        }
    }

    private void showWarningDialog(final List<GameInfo> gameInfos) {
        if (mWarningDialog != null && mWarningDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("?安全警告")
            .setMessage(buildWarningMessage(gameInfos))
            .setCancelable(false) // 禁止返回键关闭
            .setPositiveButton("检查状态", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    checkAndUpdateDialog();
                }
            })
            .setNeutralButton("查看详情", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDetailedInfo(gameInfos);
                }
            });

        mWarningDialog = builder.create();
        mWarningDialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        mWarningDialog.show();

        Log.d(TAG, "显示安全警告对话框，检测到 " + gameInfos.size() + " 个冲突游戏");
    }

    private void updateWarningDialog(List<GameInfo> gameInfos) {
        if (mWarningDialog != null && mWarningDialog.isShowing()) {
            mWarningDialog.setMessage(buildWarningMessage(gameInfos));
        }
    }

    private String buildWarningMessage(List<GameInfo> gameInfos) {
        StringBuilder message = new StringBuilder();
        message.append("检测到以下可能冲突的游戏正在运行:\n\n");

        // 按游戏类型分组显示
        String currentType = "";
        for (GameInfo game : gameInfos) {
            if (!game.gameType.equals(currentType)) {
                message.append("【").append(game.gameType).append("】\n");
                currentType = game.gameType;
            }
            message.append("• ").append(game.appName).append("\n");
        }

        message.append("\n 已排除: 明日方舟、我的世界国际版\n\n");
        message.append("⚠反作弊系统会通过漏洞打包本应用的数据并上传。\n");
        message.append("? 为了本程序的安全，禁止与这些游戏一起运行。\n\n");
        message.append("请完全关闭这些游戏后点击「检查状态」。");

        return message.toString();
    }

    private void showDetailedInfo(List<GameInfo> gameInfos) {
        StringBuilder detailedInfo = new StringBuilder();
        detailedInfo.append("冲突游戏详细信息:\n\n");

        for (GameInfo game : gameInfos) {
            detailedInfo.append("游戏名称: ").append(game.appName).append("\n");
            detailedInfo.append("包名: ").append(game.packageName).append("\n");
            detailedInfo.append("类型: ").append(game.gameType).append("\n");
            detailedInfo.append("----------------------------\n");
        }

        detailedInfo.append("\n 排除列表:\n");
        detailedInfo.append("• 明日方舟 (所有版本)\n");
        detailedInfo.append("• 我的世界国际版 (Mojang)\n");
        detailedInfo.append("• 其他白名单游戏\n\n");

        detailedInfo.append("安全提示:\n");
        detailedInfo.append("• 这些游戏的反作弊系统可能会扫描设备上的其他应用\n");
        detailedInfo.append("• 可能导致数据泄露或应用冲突\n");
        detailedInfo.append("• 建议在使用本应用前完全关闭这些游戏\n");

        new AlertDialog.Builder(getApplicationContext())
            .setTitle("冲突详情")
            .setMessage(detailedInfo.toString())
            .setPositiveButton("返回", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .show();
    }

    private void checkAndUpdateDialog() {
        // 重新检查游戏状态
        List<GameInfo> currentGames = getRunningBlockedGames();
        if (currentGames.isEmpty()) {
            closeWarningDialog();
            // 显示成功提示
            showSuccessMessage();
        } else {
            updateWarningDialog(currentGames);
        }
    }

    private void showSuccessMessage() {
        new AlertDialog.Builder(getApplicationContext())
            .setTitle(" 安全检查通过")
            .setMessage("所有冲突游戏已关闭，现在可以安全使用本应用。\n\n排除游戏仍可正常运行。")
            .setPositiveButton("OK", null)
            .show();
    }

    private void closeWarningDialog() {
        if (mWarningDialog != null && mWarningDialog.isShowing()) {
            mWarningDialog.dismiss();
            mWarningDialog = null;
            Log.d(TAG, "安全警告对话框已关闭");
        }
    }

    private void stopMonitoring() {
        mIsMonitoring = false;
        if (mScheduler != null) {
            mScheduler.shutdown();
            mScheduler = null;
        }
        closeWarningDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        Log.d(TAG, "GameSecurityService destroyed");
    }

    // 游戏信息封装类
    private static class GameInfo {
        String appName;
        String packageName;
        String gameType;

        GameInfo(String appName, String packageName, String gameType) {
            this.appName = appName;
            this.packageName = packageName;
            this.gameType = gameType;
        }
    }
}
