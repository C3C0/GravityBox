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

import java.util.List;


import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class ModClearAllRecents {
    private static final String TAG = "GB:ModClearAllRecents";
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_RECENT_VERTICAL_SCROLL_VIEW = "com.android.systemui.recent.RecentsVerticalScrollView";
    public static final String CLASS_RECENT_HORIZONTAL_SCROLL_VIEW = "com.android.systemui.recent.RecentsHorizontalScrollView";
    public static final String CLASS_RECENT_PANEL_VIEW = "com.android.systemui.recent.RecentsPanelView";
    private static final boolean DEBUG = false;

    private static XSharedPreferences mPrefs;
    private static ImageView mRecentsClearButton;

    // RAM bar
    private static TextView mBackgroundProcessText;
    private static TextView mForegroundProcessText;
    private static ActivityManager mAm;
    private static MemInfoReader mMemInfoReader;
    private static Context mGbContext;
    private static LinearColorBar mRamUsageBar;
    private static Handler mHandler;
    private static int[] mRamUsageBarPaddings;
    private static int mClearAllRecentsSizePx;
    private static int mRamUsageBarVerticalMargin;
    private static int mRamUsageBarHorizontalMargin;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            Class<?> recentPanelViewClass = XposedHelpers.findClass(CLASS_RECENT_PANEL_VIEW, classLoader);
            Class<?> recentVerticalScrollView = XposedHelpers.findClass(CLASS_RECENT_VERTICAL_SCROLL_VIEW, classLoader);
            Class<?> recentHorizontalScrollView = XposedHelpers.findClass(CLASS_RECENT_HORIZONTAL_SCROLL_VIEW, classLoader);

            mMemInfoReader = new MemInfoReader();

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(recentPanelViewClass, "showImpl", 
                        boolean.class, recentsPanelViewShowHook);
            } else {
                XposedHelpers.findAndHookMethod(recentPanelViewClass, "showIfReady", 
                        recentsPanelViewShowHook);
            }

            XposedBridge.hookAllConstructors(recentPanelViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final View v = (View) param.thisObject;
                    Context context = v.getContext();
                    mGbContext = context.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    mHandler = new Handler();
                    mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

                    final Resources res = context.getResources();
                    mRamUsageBarPaddings = new int[4];
                    mRamUsageBarPaddings[0] = mRamUsageBarPaddings[2] = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 4, res.getDisplayMetrics());
                    mRamUsageBarPaddings[1] = mRamUsageBarPaddings[3] = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics());
                    mClearAllRecentsSizePx = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 50, res.getDisplayMetrics());
                    mRamUsageBarVerticalMargin = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 15, res.getDisplayMetrics());
                    mRamUsageBarHorizontalMargin = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 10, res.getDisplayMetrics());
                    if (DEBUG) log("Recents panel view constructed");
                }
            });

            XposedHelpers.findAndHookMethod(recentPanelViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    Resources res = view.getResources();
                    ViewGroup vg = (ViewGroup) view.findViewById(res.getIdentifier("recents_bg_protect", "id", PACKAGE_NAME));

                    // GM2 already has this image view so remove it if exists
                    if (Build.DISPLAY.toLowerCase().contains("gravitymod")) {
                        View rcv = vg.findViewById(res.getIdentifier("recents_clear", "id", PACKAGE_NAME));
                        if (rcv != null) {
                            if (DEBUG) log("recents_clear ImageView found (GM2?) - removing");
                            vg.removeView(rcv);
                        }
                    }

                    // create and inject new ImageView and set onClick listener to handle action
                    mRecentsClearButton = new ImageView(vg.getContext());
                    mRecentsClearButton.setImageDrawable(res.getDrawable(res.getIdentifier("ic_notify_clear", "drawable", PACKAGE_NAME)));
                    FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                            mClearAllRecentsSizePx, mClearAllRecentsSizePx);
                    mRecentsClearButton.setLayoutParams(lParams);
                    mRecentsClearButton.setScaleType(ScaleType.CENTER);
                    mRecentsClearButton.setClickable(true);
                    mRecentsClearButton.setOnClickListener(new View.OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            ViewGroup mRecentsContainer = (ViewGroup) XposedHelpers.getObjectField(
                                    param.thisObject, "mRecentsContainer");
                            // passing null parameter in this case is our action flag to remove all views
                            mRecentsContainer.removeViewInLayout(null);
                        }
                    });
                    mRecentsClearButton.setVisibility(View.GONE);
                    vg.addView(mRecentsClearButton);
                    if (DEBUG) log("clearAllButton ImageView injected");

                    // create and inject RAM bar
                    mRamUsageBar = new LinearColorBar(vg.getContext(), null);
                    mRamUsageBar.setOrientation(LinearLayout.HORIZONTAL);
                    mRamUsageBar.setClipChildren(false);
                    mRamUsageBar.setClipToPadding(false);
                    mRamUsageBar.setPadding(mRamUsageBarPaddings[0], mRamUsageBarPaddings[1],
                            mRamUsageBarPaddings[2], mRamUsageBarPaddings[3]);
                    FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    mRamUsageBar.setLayoutParams(flp);
                    LayoutInflater inflater = LayoutInflater.from(mGbContext);
                    inflater.inflate(R.layout.linear_color_bar, mRamUsageBar, true);
                    vg.addView(mRamUsageBar);
                    mForegroundProcessText = (TextView) mRamUsageBar.findViewById(R.id.foregroundText);
                    mBackgroundProcessText = (TextView) mRamUsageBar.findViewById(R.id.backgroundText);
                    mRamUsageBar.setVisibility(View.GONE);
                    if (DEBUG) log("RAM bar injected");
                }
            });

            // for portrait mode
            XposedHelpers.findAndHookMethod(recentVerticalScrollView, "dismissChild", View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    handleDismissChild(param);
                }
            });

            // for landscape mode
            XposedHelpers.findAndHookMethod(recentHorizontalScrollView, "dismissChild", View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    handleDismissChild(param);
                }
            });

            // When to update RAM bar values
            XposedHelpers.findAndHookMethod(recentPanelViewClass, "clearRecentTasksList", 
                    updateRambarHook);
            XposedHelpers.findAndHookMethod(recentPanelViewClass, "handleSwipe",
                    View.class, updateRambarHook);
            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(recentPanelViewClass, "refreshViews", 
                        updateRambarHook);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook updateRambarHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (mHandler != null) {
                mHandler.post(updateRamBarTask);
            }
        }
    };

    private static XC_MethodHook recentsPanelViewShowHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                boolean show = false;
                if (Build.VERSION.SDK_INT < 17) {
                    show = XposedHelpers.getBooleanField(param.thisObject, "mWaitingToShow")
                           && XposedHelpers.getBooleanField(param.thisObject, "mReadyToShow");
                } else {
                    show = (Boolean) param.args[0];
                }
                if (show) {
                    mPrefs.reload();
                    updateButtonLayout((View) param.thisObject);
                    updateRamBarLayout();
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static void updateButtonLayout(View container) {
        if (mRecentsClearButton == null) return;

        int gravity = Integer.valueOf(mPrefs.getString(
                GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
        List<?> recentTaskDescriptions = (List<?>) XposedHelpers.getObjectField(
                container, "mRecentTaskDescriptions");
        boolean visible = (recentTaskDescriptions != null && recentTaskDescriptions.size() > 0);
        if (Build.VERSION.SDK_INT < 17) {
            visible |= XposedHelpers.getBooleanField(container, "mFirstScreenful");
        }
        if (gravity != GravityBoxSettings.RECENT_CLEAR_OFF && visible) {
            FrameLayout.LayoutParams lparams = 
                    (FrameLayout.LayoutParams) mRecentsClearButton.getLayoutParams();
            lparams.gravity = gravity;
            if (gravity == 51 || gravity == 53) {
                int marginTop = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 
                        mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_TOP, 0), 
                        mRecentsClearButton.getResources().getDisplayMetrics());
                lparams.setMargins(0, marginTop, 0, 0);
            } else {
                int marginBottom = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 
                        mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                        mRecentsClearButton.getResources().getDisplayMetrics());
                lparams.setMargins(0, 0, 0, marginBottom);
            }
            mRecentsClearButton.setLayoutParams(lparams);
            mRecentsClearButton.setVisibility(View.VISIBLE);
        } else {
            mRecentsClearButton.setVisibility(View.GONE);
        }
        if (DEBUG) log("Clear all recents button layout updated");
    }

    private static void handleDismissChild(final MethodHookParam param) {
        // skip if non-null view passed - fall back to original method
        if (param.args[0] != null)
            return;

        if (DEBUG) log("handleDismissChild - removing all views");

        LinearLayout mLinearLayout = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mLinearLayout");
        Handler handler = new Handler();

        int count = mLinearLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = mLinearLayout.getChildAt(i);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Object[] newArgs = new Object[1];
                        newArgs[0] = child;
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, newArgs);
                    } catch (Exception e) {
                        XposedBridge.log(e);
                    }
                }
                
            }, 150 * i);
        }

        if (mHandler != null) {
            mHandler.post(updateRamBarTask);
        }

        // don't call original method
        param.setResult(null);
    }

    private static void updateRamBarLayout() {
        if (mRamUsageBar == null) return;

        final int rbGravity = Integer.valueOf(mPrefs.getString(
                GravityBoxSettings.PREF_KEY_RAMBAR, "0"));
        if (rbGravity == 0) {
            mRamUsageBar.setVisibility(View.GONE);
        } else {
            final int caGravity = Integer.valueOf(mPrefs.getString(
                    GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
            final boolean caOnTop = (caGravity & Gravity.TOP) == Gravity.TOP;
            final boolean caOnLeft = (caGravity & Gravity.LEFT) == Gravity.LEFT;
            final boolean rbOnTop = (rbGravity == Gravity.TOP);
            final boolean sibling = mRecentsClearButton.getVisibility() == View.VISIBLE && 
                    ((caOnTop && rbOnTop) || (!caOnTop && !rbOnTop));
            final int marginTop = rbOnTop ? (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                    mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_TOP, 0), 
                    mRamUsageBar.getResources().getDisplayMetrics()) : 0;
            final int marginBottom = !rbOnTop ? (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                    mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_BOTTOM, 0), 
                    mRamUsageBar.getResources().getDisplayMetrics()) : 0;

            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mRamUsageBar.getLayoutParams();
            flp.gravity = rbGravity;
            flp.setMargins(
                sibling && caOnLeft ? mClearAllRecentsSizePx : mRamUsageBarHorizontalMargin, 
                rbOnTop ? (mRamUsageBarVerticalMargin + marginTop) : 0, 
                sibling && !caOnLeft ? mClearAllRecentsSizePx : mRamUsageBarHorizontalMargin, 
                rbOnTop ? 0 : (mRamUsageBarVerticalMargin + marginBottom)
            );
            mRamUsageBar.setLayoutParams(flp);
            mRamUsageBar.setVisibility(View.VISIBLE);
            mHandler.post(updateRamBarTask);
        }
        if (DEBUG) log("RAM bar layout updated");
    }

    private static final Runnable updateRamBarTask = new Runnable() {
        @Override
        public void run() {
            if (mRamUsageBar == null || mRamUsageBar.getVisibility() == View.GONE) {
                return;
            }

            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            mAm.getMemoryInfo(memInfo);
            long secServerMem = 0;//XposedHelpers.getLongField(memInfo, "secondaryServerThreshold");
            mMemInfoReader.readMemInfo();
            long availMem = mMemInfoReader.getFreeSize() + mMemInfoReader.getCachedSize() -
                    secServerMem;
            long totalMem = mMemInfoReader.getTotalSize();

            String sizeStr = Formatter.formatShortFileSize(mGbContext, totalMem-availMem);
            mForegroundProcessText.setText(mGbContext.getResources().getString(
                    R.string.service_foreground_processes, sizeStr));
            sizeStr = Formatter.formatShortFileSize(mGbContext, availMem);
            mBackgroundProcessText.setText(mGbContext.getResources().getString(
                    R.string.service_background_processes, sizeStr));

            float fTotalMem = totalMem;
            float fAvailMem = availMem;
            mRamUsageBar.setRatios((fTotalMem - fAvailMem) / fTotalMem, 0, 0);
            if (DEBUG) log("RAM bar values updated");
        }
    };
}
