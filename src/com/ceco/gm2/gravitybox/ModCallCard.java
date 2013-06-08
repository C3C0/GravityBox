package com.ceco.gm2.gravitybox;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModCallCard {
    private static final String TAG = "ModCallCard";
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String CLASS_CALLCARD = "com.android.phone.CallCard";
    private static final String CLASS_PHONE_CONSTANTS_STATE = 
            "com.android.internal.telephony.PhoneConstants$State";
    private static final String CLASS_CALL = "com.android.internal.telephony.Call";
    private static final String CLASS_IN_CALL_TOUCH_UI = "com.android.phone.InCallTouchUi";
    
    private static Class<?> phoneConstStateClass;
    private static Class<?> callClass;

    public static void initZygote() {
        XposedBridge.log(TAG + ": initZygote");
        phoneConstStateClass = XposedHelpers.findClass(CLASS_PHONE_CONSTANTS_STATE, null);
        callClass = XposedHelpers.findClass(CLASS_CALL, null);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        XposedBridge.log(TAG + ": init");

        try {
            Class<?> callCardClass = XposedHelpers.findClass(CLASS_CALLCARD, classLoader);
            Class<?> inCallTouchUiClass = XposedHelpers.findClass(CLASS_IN_CALL_TOUCH_UI, classLoader);

            XposedHelpers.findAndHookMethod(callCardClass, "updateCallInfoLayout", phoneConstStateClass,
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false))
                        return;
                    XposedBridge.log(TAG + ": CallCard: after updateCallInfoLayout");

                    LinearLayout layout = (LinearLayout) param.thisObject;
                    ViewGroup.MarginLayoutParams mlParams = 
                            (ViewGroup.MarginLayoutParams) layout.getLayoutParams();
                    if (mlParams != null) {
                        mlParams.bottomMargin = 0;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(callCardClass, "updateCallBannerBackground", 
                    callClass, ViewGroup.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false))
                        return;
                    XposedBridge.log(TAG + ": CallCard: after updateCallBannerBackground");

                    TextView simIndicator = 
                            (TextView) XposedHelpers.getObjectField(param.thisObject, "mSimIndicator");
                    if (simIndicator != null) {
                        simIndicator.setBackgroundResource(0);
                    }

                    ViewGroup secondaryInfoContainer =
                            (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mSecondaryInfoContainer");
                    if (secondaryInfoContainer != null) {
                        secondaryInfoContainer.setBackgroundResource(0);
                    }

                    ViewGroup secondaryCallBanner = 
                            (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mSecondaryCallBanner");
                    if (secondaryCallBanner != null) {
                        secondaryCallBanner.setBackgroundResource(0);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(inCallTouchUiClass, "showIncomingCallWidget",
                    callClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": InCallTouchUi: after showIncomingCallWidget");
                    prefs.reload();
                    boolean showFullscreen = 
                            prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false);

                    View incomingCallWidget =
                            (View) XposedHelpers.getObjectField(param.thisObject, "mIncomingCallWidget");
                    if (incomingCallWidget != null) {
                        incomingCallWidget.setBackgroundColor(showFullscreen ? Color.TRANSPARENT : Color.BLACK);
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}