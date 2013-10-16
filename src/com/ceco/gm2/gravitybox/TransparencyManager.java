/*
 * Copyright (C) 2013 AOKP Project
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

import java.util.List;

import de.robv.android.xposed.XposedBridge;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

public class TransparencyManager {
    public static final String SETTING_STATUS_BAR_ALPHA_CONFIG_LAUNCHER = "status_bar_alpha_config_launcher";
    public static final String SETTING_STATUS_BAR_ALPHA_CONFIG_LOCKSCREEN = "status_bar_alpha_config_lockscreen";
    public static final String SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LAUNCHER = "navigation_bar_alpha_config_launcher";
    public static final String SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LOCKSCREEN = "navigation_bar_alpha_config_lockscreen";
    private static final boolean DEBUG = false;

    public static final float KEYGUARD_ALPHA = 0.44f;

    private static final String TAG = "GB:TransparencyManager";

    Object mNavbar;
    Object mStatusbar;

    SomeInfo mNavbarInfo = new SomeInfo();
    SomeInfo mStatusbarInfo = new SomeInfo();

    final Context mContext;

    Handler mHandler = new Handler();

    boolean mIsHomeShowing;
    boolean mIsKeyguardShowing;

    KeyguardManager km;
    ActivityManager am;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static class SomeInfo {
        ValueAnimator anim;
        int color;
        float keyguardAlpha;
        float homeAlpha;
        boolean tempDisable;
    }

    private final Runnable updateTransparencyRunnable = new Runnable() {
        @Override
        public void run() {
            doTransparentUpdate();
        }
    };

    public TransparencyManager(Context context) {
        mContext = context;

        km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        }, intentFilter);

        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
    }

    private void update(boolean force) {
        mHandler.removeCallbacks(updateTransparencyRunnable);
        if (force || 
                mStatusbarInfo.homeAlpha != 1 || 
                mStatusbarInfo.keyguardAlpha != 1 ||
                mNavbarInfo.homeAlpha != 1 ||
                mNavbarInfo.keyguardAlpha != 1) {
            if (DEBUG) log("Updating transparency");
            mHandler.postDelayed(updateTransparencyRunnable, 100);
        }
    }

    public void update() {
        update(false);
    }

    public void setNavbar(Object n) {
        mNavbar = n;
    }

    public void setStatusbar(Object s) {
        mStatusbar = s;
    }

    public void setTempDisableStatusbarState(boolean state) {
        mStatusbarInfo.tempDisable = state;
    }

    public void setTempNavbarState(boolean state) {
        mNavbarInfo.tempDisable = state;
    }

    private ValueAnimator createAnimation(final SomeInfo info, View v) {
        if (info.anim != null) {
            info.anim.cancel();
            info.anim = null;
        }

        float a = 1;

        if (info.tempDisable) {
            info.tempDisable = false;
        } else if (mIsKeyguardShowing) {
            a = info.keyguardAlpha;
        } else if (mIsHomeShowing) {
            a = info.homeAlpha;
        }

        final float alpha = a;
        ValueAnimator anim = null;
        if (v.getBackground() instanceof BackgroundAlphaColorDrawable) {
            final BackgroundAlphaColorDrawable bg = (BackgroundAlphaColorDrawable) v
                    .getBackground();
            anim = ValueAnimator.ofObject(new ArgbEvaluator(), info.color,
                    BackgroundAlphaColorDrawable.applyAlphaToColor(bg.getBgColor(), alpha));
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    info.color = (Integer) animation.getAnimatedValue();
                    bg.setColor(info.color);
                }
            });
        } else {
            // custom image is set by the theme, let's just apply the alpha if we can.
            v.getBackground().setAlpha(BackgroundAlphaColorDrawable.floatAlphaToInt(alpha));
            return null;
        }
        anim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                info.anim = null;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                info.anim = null;
            }
        });
        info.anim = anim;
        return anim;
    }

    private void doTransparentUpdate() {
        mIsKeyguardShowing = isKeyguardShowing();
        mIsHomeShowing = isLauncherShowing();

        ValueAnimator navAnim = null, sbAnim = null;
        if (mNavbar != null) {
            navAnim = createAnimation(mNavbarInfo, (View)mNavbar);
        }
        if (mStatusbar != null) {
            sbAnim = createAnimation(mStatusbarInfo, (View)mStatusbar);
        }
        if (navAnim != null && sbAnim != null) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(navAnim, sbAnim);
            set.start();
        } else {
            if(navAnim != null) {
                navAnim.start();
            } else if(sbAnim != null) {
                sbAnim.start();
            }
        }
    }

    private boolean isLauncherShowing() {
        try {
            final List<ActivityManager.RecentTaskInfo> recentTasks = am
                    .getRecentTasks(
                            1, ActivityManager.RECENT_WITH_EXCLUDED);
            if (recentTasks.size() > 0) {
                ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(0);
                Intent intent = new Intent(recentInfo.baseIntent);
                if (recentInfo.origActivity != null) {
                    intent.setComponent(recentInfo.origActivity);
                }
                if (isCurrentHomeActivity(intent.getComponent(), null)) {
                    return true;
                }
            }
        } catch(Exception ignore) {
        }
        return false;
    }

    private boolean isKeyguardShowing() {
        if (km == null)
            return false;
        return km.isKeyguardLocked();
    }

    private boolean isCurrentHomeActivity(ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .resolveActivityInfo(mContext.getPackageManager(), 0);
        }
        return homeInfo != null
                && homeInfo.packageName.equals(component.getPackageName())
                && homeInfo.name.equals(component.getClassName());
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(
                    Settings.System.getUriFor(SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LAUNCHER), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LOCKSCREEN), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(SETTING_STATUS_BAR_ALPHA_CONFIG_LAUNCHER), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(SETTING_STATUS_BAR_ALPHA_CONFIG_LOCKSCREEN), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        float value;

        value = Settings.System.getInt(resolver, SETTING_STATUS_BAR_ALPHA_CONFIG_LAUNCHER, 0);
        mStatusbarInfo.homeAlpha = 1 - (value / 100f);

        value = Settings.System.getInt(resolver, SETTING_STATUS_BAR_ALPHA_CONFIG_LOCKSCREEN, 0);
        mStatusbarInfo.keyguardAlpha = 1 - (value / 100f);

        value = Settings.System.getInt(resolver, SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LAUNCHER, 0);
        mNavbarInfo.homeAlpha = 1 - (value / 100f);

        value = Settings.System.getInt(resolver, SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LOCKSCREEN, 0);
        mNavbarInfo.keyguardAlpha = 1 - (value / 100f);

        update(true);
    }
}

