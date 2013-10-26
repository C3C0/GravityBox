/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModNavigationBar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModNavigationBar";
    private static final String SEPARATOR = "#C3C0#";
    private static final boolean DEBUG = false;

    private static final String CLASS_NAVBAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    private static boolean mAlwaysShowMenukey;
    private static Object mNavigationBarView;
    private static Object[] mRecentsKeys;
    private static HomeKeyInfo[] mHomeKeys;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static int mHomeLongpressAction = 0;

    // Application launcher
    private static boolean mAppLauncherEnabled;
    private static KeyButtonView mAppKey0;
    private static KeyButtonView mAppKey90;
    private static Dialog mDialog;
    private static Handler mHandler;
    private static AppInfo mAppLongpress;
    private static List<AppInfo> mAppSlots;
    private static PackageManager mPm;
    private static Context mGbContext;
    private static Resources mResources;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static class HomeKeyInfo {
        public ImageView homeKey;
        public boolean supportsLongPressDefault;
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_MENUKEY)) {
                    mAlwaysShowMenukey = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_MENUKEY, false);
                    if (DEBUG) log("mAlwaysShowMenukey = " + mAlwaysShowMenukey);
                    if (mNavigationBarView != null) {
                        try {
                            final boolean showMenu = XposedHelpers.getBooleanField(
                                    mNavigationBarView, "mShowMenu");
                            XposedHelpers.callMethod(mNavigationBarView, 
                                    "setMenuVisibility", showMenu, true);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_ENABLE)) {
                    mAppLauncherEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_ENABLE, false);
                    setAppKeyVisibility(mAppLauncherEnabled);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_SLOT) &&
                        intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_APP)) {
                    int slot = intent.getIntExtra(GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_SLOT, -1);
                    String app = intent.getStringExtra(GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_APP);
                    if (DEBUG) log("appSlot=" + slot + "; app=" + app);
                    updateAppSlot(slot, app);
                }
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED)) {
                mRecentsSingletapAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED)) {
                mRecentsLongpressAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED)) {
                mHomeLongpressAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateHomeKeyLongpressSupport();
            }
        }
    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> navbarViewClass = XposedHelpers.findClass(CLASS_NAVBAR_VIEW, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);

            mAlwaysShowMenukey = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_MENUKEY, false);

            try {
                mRecentsSingletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0"));
                mRecentsLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0"));
                mHomeLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS, "0"));
            } catch (NumberFormatException nfe) {
                XposedBridge.log(nfe);
            }

            XposedBridge.hookAllConstructors(navbarViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (context == null) return;

                    mResources = context.getResources();
                    mGbContext = context.createPackageContext(
                            GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    mHandler = new Handler();
                    mPm = context.getPackageManager();

                    mAppLauncherEnabled = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_NAVBAR_LAUNCHER_ENABLE, false);

                    mAppLongpress = new AppInfo(-1);
                    updateAppSlot(-1, prefs.getString(
                            GravityBoxSettings.PREF_KEY_NAVBAR_LAUNCHER_LONGPRESS, null));

                    mAppSlots = new ArrayList<AppInfo>();
                    mAppSlots.add(new AppInfo(R.id.quickapp1));
                    mAppSlots.add(new AppInfo(R.id.quickapp2));
                    mAppSlots.add(new AppInfo(R.id.quickapp3));
                    mAppSlots.add(new AppInfo(R.id.quickapp4));
                    mAppSlots.add(new AppInfo(R.id.quickapp5));
                    mAppSlots.add(new AppInfo(R.id.quickapp6));
                    mAppSlots.add(new AppInfo(R.id.quickapp7));
                    mAppSlots.add(new AppInfo(R.id.quickapp8));
                    for (int i = 0; i < GravityBoxSettings.PREF_KEY_NAVBAR_LAUNCHER_SLOT.size(); i++) {
                        updateAppSlot(i, prefs.getString(
                                GravityBoxSettings.PREF_KEY_NAVBAR_LAUNCHER_SLOT.get(i), null));
                    }

                    mNavigationBarView = param.thisObject;
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                    context.registerReceiver(mBroadcastReceiver, intentFilter);
                    if (DEBUG) log("NavigationBarView constructed; Broadcast receiver registered");
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setMenuVisibility",
                    boolean.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = (Boolean) param.args[0] || mAlwaysShowMenukey;
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Context context = ((View) param.thisObject).getContext();
                    final Resources gbRes = mGbContext.getResources();
                    final int backButtonResId = mResources.getIdentifier("back", "id", PACKAGE_NAME);
                    final int recentAppsResId = mResources.getIdentifier("recent_apps", "id", PACKAGE_NAME);
                    final int homeButtonResId = mResources.getIdentifier("home", "id", PACKAGE_NAME);
                    final View[] rotatedViews = 
                            (View[]) XposedHelpers.getObjectField(param.thisObject, "mRotatedViews");

                    if (rotatedViews != null) {
                        mRecentsKeys = new Object[rotatedViews.length];
                        mHomeKeys = new HomeKeyInfo[rotatedViews.length];
                        int index = 0;
                        for(View v : rotatedViews) {
                            if (backButtonResId != 0) { 
                                ImageView backButton = (ImageView) v.findViewById(backButtonResId);
                                if (backButton != null) {
                                    backButton.setScaleType(ScaleType.FIT_CENTER);
                                }
                            }
                            if (recentAppsResId != 0) {
                                ImageView recentAppsButton = (ImageView) v.findViewById(recentAppsResId);
                                mRecentsKeys[index] = recentAppsButton;
                            }
                            if (homeButtonResId != 0) { 
                                HomeKeyInfo hkInfo = new HomeKeyInfo();
                                hkInfo.homeKey = (ImageView) v.findViewById(homeButtonResId);
                                if (hkInfo.homeKey != null) {
                                    hkInfo.supportsLongPressDefault = 
                                        XposedHelpers.getBooleanField(hkInfo.homeKey, "mSupportsLongpress");
                                }
                                mHomeKeys[index] = hkInfo;
                            }
                            index++;
                        }
                    }

                    // insert app key
                    ViewGroup vRot, navButtons;
                    LinearLayout.LayoutParams lp;

                    // insert app key in rot0 view
                    vRot = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(
                            mResources.getIdentifier("rot0", "id", PACKAGE_NAME));
                    if (vRot != null) {
                        navButtons = (ViewGroup) vRot.findViewById(
                                mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
                        mAppKey0 = new KeyButtonView(context);
                        lp = new LinearLayout.LayoutParams(
                                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                                        mResources.getDisplayMetrics()),
                                        LinearLayout.LayoutParams.MATCH_PARENT);
                        lp.weight = 0;
                        mAppKey0.setLayoutParams(lp);
                        mAppKey0.setScaleType(ScaleType.FIT_CENTER);
                        mAppKey0.setClickable(true);
                        mAppKey0.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_apps));
                        mAppKey0.setOnClickListener(mAppKeyOnClickListener);
                        mAppKey0.setOnLongClickListener(mAppOnLongClick);
                        navButtons.removeViewAt(0);
                        navButtons.addView(mAppKey0, 0);
                    }

                    // insert app key in rot90 view
                    vRot = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(
                            mResources.getIdentifier("rot90", "id", PACKAGE_NAME));
                    if (vRot != null) {
                        navButtons = (ViewGroup) vRot.findViewById(
                                mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
                        mAppKey90 = new KeyButtonView(context);
                        lp = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                                                mResources.getDisplayMetrics()));
                        lp.weight = 0;
                        mAppKey90.setLayoutParams(lp);
                        mAppKey90.setClickable(true);
                        mAppKey90.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_apps));
                        mAppKey90.setOnClickListener(mAppKeyOnClickListener);
                        mAppKey90.setOnLongClickListener(mAppOnLongClick);
                        navButtons.removeViewAt(navButtons.getChildCount() - 1);
                        navButtons.addView(mAppKey90);
                    }

                    setAppKeyVisibility(mAppLauncherEnabled);
                    updateRecentsKeyCode();
                    updateHomeKeyLongpressSupport();
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "shouldDisableNavbarGestures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mHomeLongpressAction != 0) {
                        param.setResult(true);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setDisabledFlags",
                    int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    boolean visible = mAppLauncherEnabled;
                    View v = (View) XposedHelpers.callMethod(param.thisObject, "getRecentsButton");
                    if (v != null) {
                        visible &= v.getVisibility() == View.VISIBLE;
                    }
                    setAppKeyVisibility(visible);
                }
            });
        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void updateRecentsKeyCode() {
        if (mRecentsKeys == null) return;

        try {
            final boolean hasAction = recentsKeyHasAction();
            for (Object o : mRecentsKeys) {
                if (o != null) {
                    XposedHelpers.setIntField(o, "mCode", hasAction ? KeyEvent.KEYCODE_APP_SWITCH : 0);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean recentsKeyHasAction() {
        return (mRecentsSingletapAction != 0 || mRecentsLongpressAction != 0);
    }

    private static void updateHomeKeyLongpressSupport() {
        if (mHomeKeys == null) return;

        try {
            for (HomeKeyInfo hkInfo : mHomeKeys) {
                if (hkInfo.homeKey != null) {
                    XposedHelpers.setBooleanField(hkInfo.homeKey, "mSupportsLongpress",
                            mHomeLongpressAction == 0 ? hkInfo.supportsLongPressDefault : true);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setAppKeyVisibility(boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        if (mAppKey0 != null) {
            mAppKey0.setVisibility(visibility);
        }
        if (mAppKey90 != null) {
            mAppKey90.setVisibility(visibility);
        }
    }

    private static Runnable mDismissAppDialogRunnable = new Runnable() {
        @Override
        public void run() {
            dismissAppDialog();
        }
    };

    private static boolean dismissAppDialog() {
        boolean dismissed = false;
        mHandler.removeCallbacks(mDismissAppDialogRunnable);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            dismissed = true;
        }
        mDialog = null;
        return dismissed;
    }

    private static View.OnClickListener mAppKeyOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if (dismissAppDialog()) {
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(mGbContext);
                View appv = inflater.inflate(R.layout.navbar_app_dialog, null);
                View appRow1 = appv.findViewById(R.id.appRow1);
                View appRow2 = appv.findViewById(R.id.appRow2);
                View separator = appv.findViewById(R.id.separator);
                int appCount = 0;
                boolean appRow1Visible = false;
                boolean appRow2Visible = false;
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
                    tv.setOnClickListener(mAppOnClick);

                    appRow1Visible |= ai.getResId() == R.id.quickapp1 || ai.getResId() == R.id.quickapp2 || 
                            ai.getResId() == R.id.quickapp3 || ai.getResId() == R.id.quickapp4;
                    appRow2Visible |= ai.getResId() == R.id.quickapp5 || ai.getResId() == R.id.quickapp6 || 
                            ai.getResId() == R.id.quickapp7 || ai.getResId() == R.id.quickapp8;

                    appCount++;
                }

                if (appCount == 0) {
                    return;
                }

                appRow1.setVisibility(appRow1Visible ? View.VISIBLE : View.GONE);
                appRow2.setVisibility(appRow2Visible ? View.VISIBLE : View.GONE);
                separator.setVisibility(appRow1Visible && appRow2Visible ?
                        View.VISIBLE : View.GONE);

                mDialog = new Dialog(v.getContext());
                mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mDialog.setContentView(appv);
                mDialog.setCanceledOnTouchOutside(true);
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
                int pf = XposedHelpers.getIntField(mDialog.getWindow().getAttributes(), "privateFlags");
                pf |= 0x00000010;
                XposedHelpers.setIntField(mDialog.getWindow().getAttributes(), "privateFlags", pf);
                mDialog.getWindow().getAttributes().gravity = Gravity.BOTTOM;
                mDialog.show();
                mHandler.postDelayed(mDismissAppDialogRunnable, 4000);
            } catch (Throwable t) {
                log("Error opening navbar app dialog: " + t.getMessage());
            }
        }
    };

    private static View.OnClickListener mAppOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissAppDialog();

            AppInfo aiProcessing = null;
            try {
                for(AppInfo ai : mAppSlots) {
                    aiProcessing = ai;
                    if (v.getId() == ai.getResId()) {
                        startActivity(v.getContext(), ai.getIntent());
                        return;
                    }
                }
                aiProcessing = null;
            } catch (Exception e) {
                log("Unable to start activity: " + e.getMessage());
                if (aiProcessing != null) {
                    aiProcessing.initAppInfo(null);
                }
            }
        }
    };

    private static View.OnLongClickListener mAppOnLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mAppLongpress.getValue() == null) {
                return false;
            } else {
                try {
                    startActivity(v.getContext(), mAppLongpress.getIntent());
                    return true;
                } catch (Exception e) {
                    log("Unable to start activity: " + e.getMessage());
                    mAppLongpress.initAppInfo(null);
                    return false;
                }
            }
        }
    };

    private static void startActivity(Context context, Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private static void updateAppSlot(int slot, String value) {
        if (slot == -1) {
            if (mAppLongpress.getValue() == null ||
                    !mAppLongpress.getValue().equals(value)) {
                mAppLongpress.initAppInfo(value);
            }
        } else {
            AppInfo ai = mAppSlots.get(slot);
            if (ai.getValue() == null || !ai.getValue().equals(value)) {
                ai.initAppInfo(value);
            }
        }
    }

    private static final class AppInfo {
        private String mPackageName;
        private String mClassName;
        private String mAppName;
        private Drawable mAppIcon;
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
            mAppIcon = null;
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
                Bitmap scaledIcon = Bitmap.createScaledBitmap(appIcon, sizePx, sizePx, true);
                mAppIcon = new BitmapDrawable(mResources, scaledIcon);
                if (DEBUG) log("AppInfo initialized for: " + getAppName());
            } catch (NameNotFoundException e) {
                log("App not found: " + ((mPackageName == null) ? "NULL" : mPackageName.toString()));
                reset();
            } catch (Exception e) {
                log("Unexpected error: " + e.getMessage());
                reset();
            }
        }
    }
}
