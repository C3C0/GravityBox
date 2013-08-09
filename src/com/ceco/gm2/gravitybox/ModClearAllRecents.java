package com.ceco.gm2.gravitybox;

import android.content.res.Resources;
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
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_RECENT_VERTICAL_SCROLL_VIEW = "com.android.systemui.recent.RecentsVerticalScrollView";
    public static final String CLASS_RECENT_HORIZONTAL_SCROLL_VIEW = "com.android.systemui.recent.RecentsHorizontalScrollView";
    public static final String CLASS_RECENT_PANEL_VIEW = "com.android.systemui.recent.RecentsPanelView";

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        XposedBridge.log("ModClearAllRecents: init");

        try {

            Class<?> recentPanelViewClass = XposedHelpers.findClass(CLASS_RECENT_PANEL_VIEW, classLoader);
            Class<?> recentVerticalScrollView = XposedHelpers.findClass(CLASS_RECENT_VERTICAL_SCROLL_VIEW, classLoader);
            Class<?> recentHorizontalScrollView = XposedHelpers.findClass(CLASS_RECENT_HORIZONTAL_SCROLL_VIEW, classLoader);

            XposedHelpers.findAndHookMethod(recentPanelViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_RECENTS_CLEAR_ALL, false))
                        return;
                    
                    XposedBridge.log("ModClearAllRecents: RecentsPanelView onFinishInflate");

                    View view = (View) param.thisObject;
                    Resources res = view.getResources();
                    ViewGroup vg = (ViewGroup) view.findViewById(res.getIdentifier("recents_bg_protect", "id", PACKAGE_NAME));

                    // GM2 already has this image view and own implementation of all recents clear, so skip it if exists
                    if (vg.findViewById(res.getIdentifier("recents_clear", "id", PACKAGE_NAME)) != null) {
                        XposedBridge.log("ModClearAllRecents: recents_clear ImageView already exists (GM2?) - skipping");
                        return;
                    }

                    // otherwise create and inject new ImageView and set onClick listener to handle action
                    ImageView imgView = new ImageView(vg.getContext());
                    imgView.setImageDrawable(res.getDrawable(res.getIdentifier("ic_notify_clear", "drawable", PACKAGE_NAME)));
                    int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, res.getDisplayMetrics());
                    FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                            sizePx, sizePx, Gravity.TOP | Gravity.RIGHT);
                    imgView.setLayoutParams(lParams);
                    imgView.setScaleType(ScaleType.CENTER);
                    imgView.setClickable(true);
                    imgView.setOnClickListener(new View.OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            XposedBridge.log("ModClearAllRecents: recents_clear ImageView onClick();");
                            ViewGroup mRecentsContainer = (ViewGroup) XposedHelpers.getObjectField(
                                    param.thisObject, "mRecentsContainer");
                            // passing null parameter in this case is our action flag to remove all views
                            mRecentsContainer.removeViewInLayout(null);
                        }
                    });
                    vg.addView(imgView);
                    XposedBridge.log("ModClearAllRecents: ImageView injected");
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
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void handleDismissChild(final MethodHookParam param) {
        // skip if non-null view passed - fall back to original method
        if (param.args[0] != null)
            return;

        XposedBridge.log("ModClearAllRecents: handleDismissChild - removing all views");

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