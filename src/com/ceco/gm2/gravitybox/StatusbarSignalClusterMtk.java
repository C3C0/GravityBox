package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StatusbarSignalClusterMtk extends StatusbarSignalCluster {
    protected boolean mRoamingIndicatorsDisabled;

    public StatusbarSignalClusterMtk(LinearLayout view, StatusBarIconManager iconManager) {
        super(view, iconManager);

        mRoamingIndicatorsDisabled = false;
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        super.onBroadcastReceived(context, intent);

        if (intent.getAction().equals(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED)) {
            mRoamingIndicatorsDisabled = intent.getBooleanExtra(
                    GravityBoxSettings.EXTRA_INDICATORS_DISABLED, false);
        }
    }

    @Override
    public void initPreferences(XSharedPreferences prefs) {
        super.initPreferences(prefs);

        mRoamingIndicatorsDisabled = prefs.getBoolean(
                GravityBoxSettings.PREF_KEY_DISABLE_ROAMING_INDICATORS, false);
    }

    @Override
    protected void updateMobileIcon() {
        try {
            Object mobileIconId = null;
            Object[] mobileIconIds = null, mobileIconIdsGemini = null;
            Object mobileActivityId = null, mobileActivityIdGemini = null;
            Object mobileTypeId = null, mobileTypeIdGemini = null;
            if (Utils.hasGeminiSupport()) {
                mobileIconIds = (Object[]) XposedHelpers.getObjectField(mView, "mMobileStrengthId");
                mobileIconIdsGemini = (Object[]) XposedHelpers.getObjectField(mView, "mMobileStrengthIdGemini");
                mobileActivityIdGemini = XposedHelpers.getObjectField(mView, "mMobileActivityIdGemini");
                mobileTypeIdGemini = XposedHelpers.getObjectField(mView, "mMobileTypeIdGemini");
            } else {
                mobileIconId = (Object) XposedHelpers.getObjectField(mView, "mMobileStrengthId");
            }
            mobileActivityId = XposedHelpers.getObjectField(mView, "mMobileActivityId");
            mobileTypeId = XposedHelpers.getObjectField(mView, "mMobileTypeId");
    
            // for SIM Slot 1
            if (XposedHelpers.getBooleanField(mView, "mMobileVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView mobile = (ImageView) XposedHelpers.getObjectField(mView, "mMobile");
                if (mobile != null) {
                    int resId = (Integer) XposedHelpers.callMethod(Utils.hasGeminiSupport() ?
                                    mobileIconIds[0] : mobileIconId, "getIconId");
                    Drawable d = mIconManager.getMobileIcon(resId);
                    if (d != null) mobile.setImageDrawable(d);
                }
                if (mIconManager.isMobileIconChangeAllowed()) {
                    ImageView mobileActivity = 
                            (ImageView) XposedHelpers.getObjectField(mView, "mMobileActivity");
                    if (mobileActivity != null) {
                        try {
                            int resId = (Integer) XposedHelpers.callMethod(mobileActivityId, "getIconId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyDataActivityColorFilter(d);
                            mobileActivity.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileActivity.setImageDrawable(null);
                        }
                    }
                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(mView, "mMobileType");
                    if (mobileType != null) {
                        try {
                            int resId = Utils.hasGeminiSupport() ?
                                    (Integer) XposedHelpers.callMethod(mobileTypeId, "getIconId") :
                                    XposedHelpers.getIntField(mView, "mMobileTypeId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyColorFilter(d);
                            mobileType.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileType.setImageDrawable(null);
                        }
                    }
                    if (XposedHelpers.getBooleanField(mView, "mRoaming")) {
                        ImageView mobileRoam = (ImageView) XposedHelpers.getObjectField(mView, "mMobileRoam");
                        if (mobileRoam != null) {
                            try {
                                int resId = XposedHelpers.getIntField(mView, "mRoamingId");
                                Drawable d = mResources.getDrawable(resId).mutate();
                                d = mIconManager.applyColorFilter(d);
                                mobileRoam.setImageDrawable(d);
                            } catch (Resources.NotFoundException e) { 
                                mobileRoam.setImageDrawable(null);
                            }
                        }
                    }
                }
            }

            // for SIM Slot 2
            if (Utils.hasGeminiSupport() && 
                    XposedHelpers.getBooleanField(mView, "mMobileVisibleGemini") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView mobile = (ImageView) XposedHelpers.getObjectField(mView, "mMobileGemini");
                if (mobile != null) {
                    int resId = (Integer) XposedHelpers.callMethod(mobileIconIdsGemini[0], "getIconId");
                    Drawable d = mIconManager.getMobileIcon(1, resId);
                    if (d != null) mobile.setImageDrawable(d);
                }
                if (mIconManager.isMobileIconChangeAllowed(1)) {
                    ImageView mobileActivity = 
                            (ImageView) XposedHelpers.getObjectField(mView, "mMobileActivityGemini");
                    if (mobileActivity != null) {
                        try {
                            int resId = (Integer) XposedHelpers.callMethod(mobileActivityIdGemini, "getIconId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyDataActivityColorFilter(1, d);
                            mobileActivity.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileActivity.setImageDrawable(null);
                        }
                    }
                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(mView, "mMobileTypeGemini");
                    if (mobileType != null) {
                        try {
                            int resId = (Integer) XposedHelpers.callMethod(mobileTypeIdGemini, "getIconId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyColorFilter(1, d);
                            mobileType.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileType.setImageDrawable(null);
                        }
                    }
                    if (XposedHelpers.getBooleanField(mView, "mRoamingGemini")) {
                        ImageView mobileRoam = (ImageView) XposedHelpers.getObjectField(mView, "mMobileRoamGemini");
                        if (mobileRoam != null) {
                            try {
                                int resId = XposedHelpers.getIntField(mView, "mRoamingGeminiId");
                                Drawable d = mResources.getDrawable(resId).mutate();
                                d = mIconManager.applyColorFilter(1, d);
                                mobileRoam.setImageDrawable(d);
                            } catch (Resources.NotFoundException e) { 
                                mobileRoam.setImageDrawable(null);
                            }
                        }
                    }
                }
            }  
        } catch (Throwable t) {
                XposedBridge.log(t);
        }
    }

    @Override 
    protected void updateAirplaneModeIcon() {
        try {
            ImageView airplaneModeIcon = Utils.hasGeminiSupport() ?
                    (ImageView) XposedHelpers.getObjectField(mView, "mFlightMode") :
                        (ImageView) XposedHelpers.getObjectField(mView, "mAirplane");
            if (airplaneModeIcon != null) {
                Drawable d = airplaneModeIcon.getDrawable();
                if (mIconManager.isColoringEnabled()) {
                    d = mIconManager.applyColorFilter(d);
                } else if (d != null) {
                    d.setColorFilter(null);
                }
                airplaneModeIcon.setImageDrawable(d);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void apply() {
        super.apply();

        updateRoamingIndicator();
    }

    protected void updateRoamingIndicator() {
        try {
            if (mRoamingIndicatorsDisabled) {
                ImageView mobileRoam;
                mobileRoam = (ImageView) XposedHelpers.getObjectField(mView, "mMobileRoam");
                if (mobileRoam != null) mobileRoam.setVisibility(View.GONE);
                if (Utils.hasGeminiSupport()) {
                    mobileRoam = (ImageView) XposedHelpers.getObjectField(mView, "mMobileRoamGemini");
                    if (mobileRoam != null) mobileRoam.setVisibility(View.GONE);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
