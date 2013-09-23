package com.ceco.gm2.gravitybox.quicksettings;

import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class QuickAppTile extends AQuickSettingsTile {
    private static final String TAG = "QuickAppTile";
    private static final String SEPARATOR = "#C3C0#";

    public static final String SETTING_QUICKAPP_DEFAULT = "quick_app_default";
    public static final String SETTING_QUICKAPP_SLOT1 = "quick_app_slot1";
    public static final String SETTING_QUICKAPP_SLOT2 = "quick_app_slot2";
    public static final String SETTING_QUICKAPP_SLOT3 = "quick_app_slot3";
    public static final String SETTING_QUICKAPP_SLOT4 = "quick_app_slot4";

    private AppInfo mMainApp;
    private List<AppInfo> mAppSlots;
    private PackageManager mPm;
    private SettingsObserver mSettingsObserver;
    private Dialog mDialog;
    private Handler mHandler;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private final class AppInfo {
        private String mPackageName;
        private String mClassName;
        private String mAppName;
        private Drawable mAppIcon;
        private Drawable mAppIconSmall;
        private String mValue;
        private int mResId;

        public AppInfo(int resId) {
            mResId = resId;
        }

        public int getResId() {
            return mResId;
        }

        public String getAppName() {
            return (mAppName == null ? 
                    mGbContext.getString(R.string.qs_tile_quickapp) : mAppName);
        }

        public Drawable getAppIcon() {
            return (mAppIcon == null ? 
                    mResources.getDrawable(android.R.drawable.ic_menu_help) : mAppIcon);
        }

        public Drawable getAppIconSmall() {
            return (mAppIconSmall == null ? 
                    mResources.getDrawable(android.R.drawable.ic_menu_help) : mAppIconSmall);
        }

        public String getValue() {
            return mValue;
        }

        public Intent getIntent() {
            if (mPackageName == null || mClassName == null) return null;

            Intent i = new Intent();
            i.setClassName(mPackageName, mClassName);
            return i;
        }

        private void reset() {
            mValue = mPackageName = mClassName = mAppName = null;
            mAppIcon = mAppIconSmall = null;
        }

        public void initAppInfo(String value) {
            mValue = value;
            if (mValue == null) {
                reset();
                return;
            }

            try {
                String[] splitValue = value.split(SEPARATOR);
                mPackageName = splitValue[0];
                mClassName = splitValue[1];
                ComponentName cn = new ComponentName(mPackageName, mClassName);
                ActivityInfo ai = mPm.getActivityInfo(cn, 0);
                mAppName = ai.loadLabel(mPm).toString();
                Bitmap appIcon = ((BitmapDrawable)ai.loadIcon(mPm)).getBitmap();
                int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, 
                        mResources.getDisplayMetrics());
                int sizePxSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, 
                        mResources.getDisplayMetrics());
                Bitmap scaledIcon;
                scaledIcon = Bitmap.createScaledBitmap(appIcon, sizePx, sizePx, true);
                mAppIcon = new BitmapDrawable(mResources, scaledIcon);
                scaledIcon = Bitmap.createScaledBitmap(appIcon, sizePxSmall, sizePxSmall, true);
                mAppIconSmall = new BitmapDrawable(mResources, scaledIcon);
            } catch (NameNotFoundException e) {
                log("App not found: " + ((mPackageName == null) ? "NULL" : mPackageName.toString()));
                reset();
            } catch (Exception e) {
                log("Unexpected error: " + e.getMessage());
                reset();
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_QUICKAPP_DEFAULT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_QUICKAPP_SLOT1), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_QUICKAPP_SLOT2), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_QUICKAPP_SLOT3), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_QUICKAPP_SLOT4), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            log("SettingsObserver onChange()");
            String value;
            AppInfo ai;
            ContentResolver cr = mContext.getContentResolver();

            value = Settings.System.getString(cr, SETTING_QUICKAPP_DEFAULT);
            if (mMainApp.getValue() == null || !mMainApp.getValue().equals(value)) {
                mMainApp.initAppInfo(value);
            }

            for (int i = 0; i <= 3; i++) {
                String key = "quick_app_slot" + (i+1);
                value = Settings.System.getString(cr, key);
                ai = mAppSlots.get(i);
                if (ai.getValue() == null || !ai.getValue().equals(value)) {
                    ai.initAppInfo(value);
                    if (ai.getValue() == null) {
                        Settings.System.putString(cr, key, null);
                    }
                }
            }

            updateResources();
        }
    };

    private Runnable mDismissDialogRunnable = new Runnable() {

        @Override
        public void run() {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
        }
    };

    public QuickAppTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mHandler = new Handler();

        mPm = context.getPackageManager();
        mMainApp = new AppInfo(R.id.quickapp_tileview);
        mAppSlots = new ArrayList<AppInfo>();
        mAppSlots.add(new AppInfo(R.id.quickapp1));
        mAppSlots.add(new AppInfo(R.id.quickapp2));
        mAppSlots.add(new AppInfo(R.id.quickapp3));
        mAppSlots.add(new AppInfo(R.id.quickapp4));

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(mDismissDialogRunnable);
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                    mDialog = null;
                }

                AppInfo aiProcessing = null;
                try {
                    for(AppInfo ai : mAppSlots) {
                        aiProcessing = ai;
                        if (v.getId() == ai.getResId()) {
                            startActivity(ai.getIntent());
                            return;
                        }
                    }

                    aiProcessing = null;
                    startActivity(mMainApp.getIntent());
                } catch (Exception e) {
                    log("Unable to start activity: " + e.getMessage());
                    if (aiProcessing != null) {
                        aiProcessing.initAppInfo(null);
                    }
                }
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                LayoutInflater inflater = LayoutInflater.from(mGbContext);
                View appv = inflater.inflate(R.layout.quick_settings_app_dialog, null);
                boolean atLeastOne = false;
                for (AppInfo ai : mAppSlots) {
                    TextView tv = (TextView) appv.findViewById(ai.getResId());
                    if (ai.getValue() == null) {
                        tv.setVisibility(View.GONE);
                        continue;
                    }

                    tv.setText(ai.getAppName());
                    tv.setTextSize(1, 10);
                    tv.setMaxLines(2);
                    tv.setEllipsize(TruncateAt.END);
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, ai.getAppIcon(), null, null);
                    tv.setClickable(true);
                    tv.setOnClickListener(mOnClick);
                    atLeastOne = true;
                }
                if (!atLeastOne) return true;

                mDialog = new Dialog(mContext);
                mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mDialog.setContentView(appv);
                mDialog.setCanceledOnTouchOutside(true);
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
                int pf = XposedHelpers.getIntField(mDialog.getWindow().getAttributes(), "privateFlags");
                pf |= 0x00000010;
                XposedHelpers.setIntField(mDialog.getWindow().getAttributes(), "privateFlags", pf);
                mDialog.getWindow().clearFlags(LayoutParams.FLAG_DIM_BEHIND);
                mDialog.show();
                mHandler.removeCallbacks(mDismissDialogRunnable);
                mHandler.postDelayed(mDismissDialogRunnable, 4000);
                return true;
            }
        };

        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.onChange(true);
        mSettingsObserver.observe();
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_quick_app, mTile);
    }

    @Override
    protected synchronized void updateTile() {
        mLabel = mMainApp.getAppName();

        TextView tv = (TextView) mTile.findViewById(R.id.quickapp_tileview);
        tv.setText(mLabel);
        tv.setCompoundDrawablesWithIntrinsicBounds(null, mMainApp.getAppIconSmall(), null, null);
    }
}
