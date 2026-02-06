package com.minesms.launcher3;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

public class DebugDialog {
    private Dialog dialog;
    private Context context;

    public DebugDialog(Context context) {
        this.context = context;
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_debug);
        dialog.setTitle("侦错信息");

        TextView debugInfo = dialog.findViewById(R.id.debug_info);
        Button closeBtn = dialog.findViewById(R.id.debug_close);

        // 显示系统信息
        StringBuilder info = new StringBuilder();
        info.append("设备型号: ").append(Build.MODEL).append("\n");
        info.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("SDK版本: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("处理器: ").append(Build.CPU_ABI).append("\n");
        info.append("系统时间: ").append(System.currentTimeMillis()).append("\n");

        debugInfo.setText(info.toString());

        closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

        dialog.show();
    }
}

