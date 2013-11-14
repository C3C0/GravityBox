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

import de.robv.android.xposed.XSharedPreferences;
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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;

public class TransparencyManager implements BroadcastSubReceiver {
    private static final String TAG = "GB:TransparencyManager";
    private static final boolean DEBUG = false;

    public static final int MODE_DISABLED = 0;
    public static final int MODE_STATUSBAR = 1;
    public static final int MODE_NAVBAR = 2;
    public static final int MODE_FULL = 3;

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

    int mMode = MODE_DISABLED;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static class SomeInfo {
        ValueAnimator anim;
        int color;
        float keyguardAlpha;
        float homeAlpha;
    }

    private final Runnable updateTransparencyRunnable = new Runnable() {
        @Override
        public void run() {
            doTransparentUpdate();
        }
    };

    public TransparencyManager(Context context, int mode) {
        mContext = context;
        mMode = mode;

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
    }

    public static boolean isStatusbarEnabled(int mode) {
        return ((mode & MODE_STATUSBAR) != 0);
    }

    public boolean isStatusbarEnabled() {
        return isStatusbarEnabled(mMode);
    }

    public static boolean isNavbarEnabled(int mode) {
        return ((mode & MODE_NAVBAR) != 0);
    }

    public boolean isNavbarEnabled() {
        return isNavbarEnabled(mMode);
    }

    private void update(boolean force) {
        mHandler.removeCallbacks(updateTransparencyRunnable);
        if (force || 
                (isStatusbarEnabled() && 
                        (mStatusbarInfo.homeAlpha != 1 || 
                         mStatusbarInfo.keyguardAlpha != 1 )) ||
                (isNavbarEnabled() &&
                        (mNavbarInfo.homeAlpha != 1 ||
                         mNavbarInfo.keyguardAlpha != 1))) {
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

    private ValueAnimator createAnimation(final SomeInfo info, View v) {
        if (info.anim != null) {
            info.anim.cancel();
            info.anim = null;
        }

        float a = 1;

        if (mIsKeyguardShowing) {
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
            Drawable bg = v.getBackground();
            if (bg != null) {
                bg.setAlpha(BackgroundAlphaColorDrawable.floatAlphaToInt(alpha));
            }
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
        if ((mMode & MODE_NAVBAR) != 0 && mNavbar != null) {
            navAnim = createAnimation(mNavbarInfo, (View)mNavbar);
        }
        if ((mMode & MODE_STATUSBAR) != 0 && mStatusbar != null) {
            sbAnim = createAnimation(mStatusbarInfo, (View)mStatusbar);
        }
        if (navAnim != null && sbAnim != null) {
            if (DEBUG) log("Updating transparency for statusbar & navbar");
            AnimatorSet set = new AnimatorSet();
            set.playTogether(navAnim, sbAnim);
            set.start();
        } else {
            if(navAnim != null) {
                if (DEBUG) log("Updating transparency for navbar");
                navAnim.start();
            } else if(sbAnim != null) {
                if (DEBUG) log("Updating transparency for statusbar");
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
    
    public void initPreferences(XSharedPreferences prefs) {
        int value;

        value = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_STATUSBAR_LAUNCHER, 0);
        mStatusbarInfo.homeAlpha = 1 - (value / 100f);

        value = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_STATUSBAR_LOCKSCREEN, 0);
        mStatusbarInfo.keyguardAlpha = 1 - (value / 100f);

        value = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_NAVBAR_LAUNCHER, 0);
        mNavbarInfo.homeAlpha = 1 - (value / 100f);

        value = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_NAVBAR_LOCKSCREEN, 0);
        mNavbarInfo.keyguardAlpha = 1 - (value / 100f);
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
            int value;
            if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER, 0);
                mStatusbarInfo.homeAlpha = 1 - (value / 100f);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN, 0);
                mStatusbarInfo.keyguardAlpha = 1 - (value / 100f);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER, 0);
                mNavbarInfo.homeAlpha = 1 - (value / 100f);
            }
            if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN, 0);
                mNavbarInfo.keyguardAlpha = 1 - (value / 100f);
            }

            update(true);
        }
    }
}

