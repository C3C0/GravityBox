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

import java.io.File;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModCallCard {
    private static final String TAG = "GB:ModCallCard";
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String CLASS_CALLCARD = "com.android.phone.CallCard";
    private static final String CLASS_PHONE_CONSTANTS_STATE = Build.VERSION.SDK_INT > 16 ?
            "com.android.internal.telephony.PhoneConstants$State" :
            "com.android.internal.telephony.Phone$State";
    private static final String CLASS_CALL = "com.android.internal.telephony.Call";
    private static final String CLASS_IN_CALL_TOUCH_UI = "com.android.phone.InCallTouchUi";
    private static final boolean DEBUG = false;
    
    private static Class<?> phoneConstStateClass;
    private static Class<?> callClass;

    public static void initZygote() {
        try {
            if (DEBUG) XposedBridge.log(TAG + ": initZygote");
            phoneConstStateClass = XposedHelpers.findClass(CLASS_PHONE_CONSTANTS_STATE, null);
            callClass = XposedHelpers.findClass(CLASS_CALL, null);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        if (DEBUG) XposedBridge.log(TAG + ": init");

        try {
            final Class<?> callCardClass = XposedHelpers.findClass(CLASS_CALLCARD, classLoader);
            final Class<?> inCallTouchUiClass = XposedHelpers.findClass(CLASS_IN_CALL_TOUCH_UI, classLoader);

            XposedHelpers.findAndHookMethod(callCardClass, "updateCallInfoLayout", phoneConstStateClass,
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false))
                        return;
                    if (DEBUG) XposedBridge.log(TAG + ": CallCard: after updateCallInfoLayout");

                    LinearLayout layout = (LinearLayout) param.thisObject;
                    ViewGroup.MarginLayoutParams mlParams = 
                            (ViewGroup.MarginLayoutParams) layout.getLayoutParams();
                    if (mlParams != null) {
                        mlParams.bottomMargin = 0;
                    }
                }
            });

            if (Utils.isMtkDevice()) {
                XposedHelpers.findAndHookMethod(callCardClass, "updateCallBannerBackground", 
                        callClass, ViewGroup.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        prefs.reload();
                        if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false))
                            return;
                        if (DEBUG) XposedBridge.log(TAG + ": CallCard: after updateCallBannerBackground");
    
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
            }

            XposedHelpers.findAndHookMethod(inCallTouchUiClass, "showIncomingCallWidget",
                    callClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) XposedBridge.log(TAG + ": InCallTouchUi: after showIncomingCallWidget");
                    prefs.reload();
                    boolean showFullscreen = 
                            prefs.getBoolean(GravityBoxSettings.PREF_KEY_CALLER_FULLSCREEN_PHOTO, false);

                    View incomingCallWidget =
                            (View) XposedHelpers.getObjectField(param.thisObject, "mIncomingCallWidget");
                    if (incomingCallWidget != null) {
                        if (showFullscreen) {
                            incomingCallWidget.setBackgroundColor(Color.TRANSPARENT);
                        } else if (Build.DISPLAY.toLowerCase().contains("gravitymod")) {
                            incomingCallWidget.setBackgroundColor(Color.BLACK);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(callCardClass, "showImage",
                    ImageView.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE, false) ||
                            param.args[0] == null) return;

                    final ImageView iv = (ImageView) param.args[0];
                    final Context context = iv.getContext();
                    final int resId = context.getResources().getIdentifier(
                            "picture_unknown", "drawable", PACKAGE_NAME);

                    if (resId == (Integer) param.args[1]) {
                        Context gbContext = context.createPackageContext(GravityBox.PACKAGE_NAME, 0);
                        final String path = gbContext.getFilesDir() + "/caller_photo";
                        File f = new File(path);
                        if (f.exists()) {
                            Bitmap b = BitmapFactory.decodeFile(path);
                            if (b != null) {
                                XposedHelpers.callStaticMethod(callCardClass, "showImage", iv, b);
                                param.setResult(null);
                                return;
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}