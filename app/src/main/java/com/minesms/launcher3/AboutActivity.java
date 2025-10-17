package com.minesms.launcher3;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ValueAnimator;

public class AboutActivity extends Activity {
    

    private TextView copyrightText;
    private ValueAnimator rotationAnimator;
    private int clickCount = 0;
    private long firstClickTime = 0;
    private Handler longPressHandler = new Handler();
    private boolean isLongPressTriggered = false;

    private Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isLongPressTriggered) {
                isLongPressTriggered = true;
                showHypergryphDialog();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        ImageView aboutIcon = findViewById(R.id.aboutIcon);
        TextView aboutAppName = findViewById(R.id.aboutAppName);
        Button btnGitHub = findViewById(R.id.btnGitHub);
        copyrightText = findViewById(R.id.aboutCopyright);

        // 设置应用图标和名称（已经通过XML设置）
    }

    private void setupClickListeners() {
        
        ImageView aboutIcon = findViewById(R.id.aboutIcon);

        // 图标触摸监听器
        aboutIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            // 按下时开始旋转
                            startContinuousRotation(v);
                            return true;

                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            // 松开时停止旋转并复位
                            stopRotationAndReset(v);
                            return true;
                    }
                    return false;
                }
            });
        
        // GitHub按钮点击事件
        Button btnGitHub = findViewById(R.id.btnGitHub);
        btnGitHub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openGitHub();
                }
            });
            
        Button btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    openUpdate();
                }
                
            
        });

        // 版权文本点击事件（3次点击）
        copyrightText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleTripleClick();
                }
            });

        // 版权文本长按事件（6秒长按）
        copyrightText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startLongPressDetection();
                    return true;
                }
            });

        // 版权文本触摸释放事件
        copyrightText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                        event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                        cancelLongPressDetection();
                    }
                    return false;
                }
            });
    }
    
    private void startContinuousRotation(final View view) {
        if (rotationAnimator != null && rotationAnimator.isRunning()) {
            rotationAnimator.cancel();
        }

        rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(200);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new android.view.animation.LinearInterpolator());

        rotationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float rotation = (Float) animation.getAnimatedValue();
                    view.setRotation(rotation);
                }
            });

        rotationAnimator.start();
    }

    private void stopRotationAndReset(View view) {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
            rotationAnimator = null;
        }

        // 复位到0度
        view.animate()
            .rotation(0)
            .setDuration(1400)
            .start();
    }

    private void handleTripleClick() {
        long currentTime = System.currentTimeMillis();

        if (firstClickTime == 0) {
            firstClickTime = currentTime;
            clickCount = 1;
        } else {
            if (currentTime - firstClickTime <= 2000) { // 2秒内完成3次点击
                clickCount++;
                if (clickCount >= 3) {
                    showHypergryphDialog();
                    resetClickCounter();
                }
            } else {
                resetClickCounter();
            }
        }
    }

    private void resetClickCounter() {
        clickCount = 0;
        firstClickTime = 0;
    }

    private void startLongPressDetection() {
        isLongPressTriggered = false;
        longPressHandler.postDelayed(longPressRunnable, 6000); // 6秒后触发
    }

    private void cancelLongPressDetection() {
        longPressHandler.removeCallbacks(longPressRunnable);
        isLongPressTriggered = false;
    }

    private void openGitHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/meirong114/TinyLauncher"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开GitHub", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openUpdate() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://github.com/meirong114/TinyLauncher/releases"));
        startActivity(intent);
    }

    private void showHypergryphDialog() {
        // 创建对话框
        final Dialog dialog = new Dialog(AboutActivity.this);
        dialog.setContentView(R.layout.dialog_hypergryph);

        // 获取WebView并加载图片
        WebView webView = dialog.findViewById(R.id.hypergryphWebView);
        String imageUrl = "https://i0.hdslb.com/bfs/openplatform/eeca79c84d949f0f8f302034bc5539efdc045ea3.png";

        // 创建HTML内容显示图片
        String htmlContent = "<html><body style='margin:0;padding:0;text-align:center;'>" +
            "<img src='" + imageUrl + "' style='max-width:100%;height:auto;'/>" +
            "<center>" + 
            "<h1>ID:460945369</h1>" + "<br>" + "<h1>Whirity404#8822</h1>" +
            "</center>" + 
            "</body></html>";

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        // 获取按钮
        Button btnLater = dialog.findViewById(R.id.btnLater);
        Button btnNow = dialog.findViewById(R.id.btnNow);
        Button btnRead = dialog.findViewById(R.id.btnRead);

        // 设置按钮点击事件
        btnLater.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

        btnNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("com.hypergryph.arknights", "com.u8.sdk.U8UnityContext");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(AboutActivity.this, "无法打开明日方舟，请检查是否安装", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }
            });

        btnRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // 什么反应都不做，只是关闭按钮点击效果
                    v.setPressed(true);
                    v.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                v.setPressed(false);
                            }
                        }, 100);
                }
            });

        // 显示对话框
        dialog.show();

        // 重置计数器
        resetClickCounter();
        cancelLongPressDetection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelLongPressDetection();
    }
}
