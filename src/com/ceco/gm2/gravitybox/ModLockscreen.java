package com.ceco.gm2.gravitybox;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewManager;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModLockscreen {
    private static final String TAG = "ModLockscreen";
    private static final String CLASS_KGVIEW_MANAGER = "com.android.internal.policy.impl.keyguard.KeyguardViewManager";
    private static final String CLASS_KG_HOSTVIEW = "com.android.internal.policy.impl.keyguard.KeyguardHostView";

    private static XSharedPreferences mPrefs;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote(final XSharedPreferences prefs) {
        log("initZygote");

        try {
            mPrefs = prefs;
            final Class<?> kgViewManagerClass = XposedHelpers.findClass(CLASS_KGVIEW_MANAGER, null);
            final Class<?> kgHostViewClass = XposedHelpers.findClass(CLASS_KG_HOSTVIEW, null);

            boolean enableMenuKey = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_LOCKSCREEN_MENU_KEY, false);
            XResources.setSystemWideReplacement("android", "bool", "config_disableMenuKeyInLockScreen", !enableMenuKey);

            XposedHelpers.findAndHookMethod(kgViewManagerClass, "maybeCreateKeyguardLocked", 
                    boolean.class, boolean.class, Bundle.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPrefs.reload();
                    ViewManager viewManager = (ViewManager) XposedHelpers.getObjectField(
                            param.thisObject, "mViewManager");
                    FrameLayout keyGuardHost = (FrameLayout) XposedHelpers.getObjectField(
                            param.thisObject, "mKeyguardHost");
                    WindowManager.LayoutParams windowLayoutParams = (WindowManager.LayoutParams) 
                            XposedHelpers.getObjectField(param.thisObject, "mWindowLayoutParams");

                    final String bgType = prefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND,
                            GravityBoxSettings.LOCKSCREEN_BG_DEFAULT);

                    if (!bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_DEFAULT)) {
                        windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
                    } else {
                        windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
                    }
                    viewManager.updateViewLayout(keyGuardHost, windowLayoutParams);
                    log("maybeCreateKeyguardLocked: layout updated");
                }
            });

            XposedHelpers.findAndHookMethod(kgViewManagerClass, "inflateKeyguardView",
                    Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPrefs.reload();

                    FrameLayout keyguardView = (FrameLayout) XposedHelpers.getObjectField(
                            param.thisObject, "mKeyguardView");

                    final String bgType = mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND, 
                            GravityBoxSettings.LOCKSCREEN_BG_DEFAULT);

                    if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_COLOR)) {
                        int color = mPrefs.getInt(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR, Color.BLACK);
                        keyguardView.setBackgroundColor(color);
                        log("inflateKeyguardView: background color set");
                    } else if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_IMAGE)) {
                        try {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            FrameLayout flayout = new FrameLayout(context);
                            flayout.setLayoutParams(new LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 
                                    ViewGroup.LayoutParams.MATCH_PARENT));
                            Context gbContext = context.createPackageContext(
                                    GravityBox.PACKAGE_NAME, 0);
                            String wallpaperFile = gbContext.getFilesDir() + "/lockwallpaper";
                            Bitmap background = BitmapFactory.decodeFile(wallpaperFile);
                            Drawable d = new BitmapDrawable(context.getResources(), background);
                            ImageView mLockScreenWallpaperImage = new ImageView(context);
                            mLockScreenWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
                            mLockScreenWallpaperImage.setImageDrawable(d);
                            flayout.addView(mLockScreenWallpaperImage, -1, -1);
                            keyguardView.addView(flayout,0);
                            log("inflateKeyguardView: background image set");
                        } catch (NameNotFoundException e) {
                            XposedBridge.log(e);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgViewManagerClass, 
                    "shouldEnableScreenRotation", new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            prefs.reload();
                            return prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_ROTATION, false);
                        }
            });

            XposedHelpers.findAndHookMethod(kgHostViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Object slidingChallenge = XposedHelpers.getObjectField(
                            param.thisObject, "mSlidingChallengeLayout");
                    minimizeChallengeIfDesired(slidingChallenge);
                }
            });
            XposedHelpers.findAndHookMethod(kgHostViewClass, "onScreenTurnedOn", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Object slidingChallenge = XposedHelpers.getObjectField(
                            param.thisObject, "mSlidingChallengeLayout");
                    minimizeChallengeIfDesired(slidingChallenge);
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void minimizeChallengeIfDesired(Object challenge) {
        if (challenge == null) return;

        mPrefs.reload();
        if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_MAXIMIZE_WIDGETS, false)) {
            log("minimizeChallengeIfDesired: challenge minimized");
            XposedHelpers.callMethod(challenge, "showChallenge", false);
        }
    }
}