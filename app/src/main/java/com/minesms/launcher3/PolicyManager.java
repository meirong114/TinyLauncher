package com.minesms.launcher3;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

public class PolicyManager {
    public static final int DPM_ACTIVATION_REQUEST_CODE = 100;
    private Context mContext;
    private DevicePolicyManager mDPM;
    private ComponentName adminComponent;

    public PolicyManager(Context context) {
        this.mContext = context;
        mDPM = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(mContext, DPMReceiver.class);
    }

    public boolean isAdminActive() {
        return mDPM.isAdminActive(adminComponent);
    }

    public ComponentName getAdminComponent() {
        return adminComponent;
    }

    public void disableAdmin() {
        mDPM.removeActiveAdmin(adminComponent);
    }
}
