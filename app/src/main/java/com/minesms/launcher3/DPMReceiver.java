package com.minesms.launcher3;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DPMReceiver extends DeviceAdminReceiver {
    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
        super.onDisabled(context, intent);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
        super.onEnabled(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        Toast.makeText(context, "Disable Device Admin Requested", Toast.LENGTH_SHORT).show();
        return super.onDisableRequested(context, intent);
    }
}
