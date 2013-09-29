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

import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;

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

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            Class<?> recentPanelViewClass = XposedHelpers.findClass(CLASS_RECENT_PANEL_VIEW, classLoader);
            Class<?> recentVerticalScrollView = XposedHelpers.findClass(CLASS_RECENT_VERTICAL_SCROLL_VIEW, classLoader);
            Class<?> recentHorizontalScrollView = XposedHelpers.findClass(CLASS_RECENT_HORIZONTAL_SCROLL_VIEW, classLoader);

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(recentPanelViewClass, "showImpl", 
                        boolean.class, recentsPanelViewShowHook);
            } else {
                XposedHelpers.findAndHookMethod(recentPanelViewClass, "showIfReady", 
                        recentsPanelViewShowHook);
            }

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
                    ImageView imgView = new ImageView(vg.getContext());
                    imgView.setImageDrawable(res.getDrawable(res.getIdentifier("ic_notify_clear", "drawable", PACKAGE_NAME)));
                    int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, res.getDisplayMetrics());
                    FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(sizePx, sizePx);
                    imgView.setLayoutParams(lParams);
                    imgView.setScaleType(ScaleType.CENTER);
                    imgView.setClickable(true);
                    imgView.setTag("clearAllButton");
                    imgView.setOnClickListener(new View.OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            ViewGroup mRecentsContainer = (ViewGroup) XposedHelpers.getObjectField(
                                    param.thisObject, "mRecentsContainer");
                            // passing null parameter in this case is our action flag to remove all views
                            mRecentsContainer.removeViewInLayout(null);
                        }
                    });
                    imgView.setVisibility(View.GONE);
                    vg.addView(imgView);
                    if (DEBUG) log("clearAllButton ImageView injected");
                    updateButtonLayout((View) param.thisObject, imgView);
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
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

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
                    FrameLayout fl = (FrameLayout) param.thisObject;
                    ImageView iv = (ImageView) fl.findViewWithTag("clearAllButton");
                    if (iv == null) {
                        log("WTF? Clear all recents button not found!");
                        return;
                    }
                    updateButtonLayout((View) param.thisObject, iv);
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static void updateButtonLayout(View container, ImageView iv) {
        mPrefs.reload();
        int gravity = Integer.valueOf(mPrefs.getString(
                GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, "53"));
        List<?> recentTaskDescriptions = (List<?>) XposedHelpers.getObjectField(
                container, "mRecentTaskDescriptions");
        boolean visible = (recentTaskDescriptions != null && recentTaskDescriptions.size() > 0);
        if (Build.VERSION.SDK_INT < 17) {
            visible |= XposedHelpers.getBooleanField(container, "mFirstScreenful");
        }
        if (gravity != GravityBoxSettings.RECENT_CLEAR_OFF && visible) {
            FrameLayout.LayoutParams lparams = (FrameLayout.LayoutParams) iv.getLayoutParams();
            lparams.gravity = gravity;
            if ((gravity & Gravity.TOP) != 0) {
                int marginTop = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 
                        mPrefs.getInt(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_MARGIN_TOP, 0), 
                        iv.getResources().getDisplayMetrics());
                lparams.setMargins(0, marginTop, 0, 0);
            } else {
                lparams.setMargins(0, 0, 0, 0);
            }
            iv.setLayoutParams(lparams);
            iv.setVisibility(View.VISIBLE);
        } else {
            iv.setVisibility(View.GONE);
        }
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

        // don't call original method
        param.setResult(null);
    }
}