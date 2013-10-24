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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import com.ceco.gm2.gravitybox.adapters.*;

public class ModPowerMenu {
    private static final String TAG = "GB:ModPowerMenu";
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.internal.policy.impl.GlobalActions";
    public static final String CLASS_ACTION = "com.android.internal.policy.impl.GlobalActions.Action";
    private static final boolean DEBUG = false;

    private static Context mContext;
    private static Handler mHandler;
    private static String mRebootStr;
    private static String mRebootSoftStr;
    private static String mRecoveryStr;
    private static String mBootloaderStr;
    private static Drawable mRebootIcon;
    private static Drawable mRebootSoftIcon;
    private static Drawable mRecoveryIcon;
    private static Drawable mBootloaderIcon;
    private static Drawable mExpandedDesktopIcon;
    private static Drawable mScreenshotIcon;
    private static List<IIconListAdapterItem> mRebootItemList;
    private static String mRebootConfirmStr;
    private static String mRebootConfirmRecoveryStr;
    private static String mRebootConfirmBootloaderStr;
    private static String mExpandedDesktopStr;
    private static String mExpandedDesktopOnStr;
    private static String mExpandedDesktopOffStr;
    private static String mScreenshotStr;
    private static Unhook mRebootActionHook;
    private static Object mRebootActionItem;
    private static boolean mRebootActionItemStockExists;
    private static Object mScreenshotAction;
    private static Object mExpandedDesktopAction;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {

        try {
            final Class<?> globalActionsClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS, classLoader);
            final Class<?> actionClass = XposedHelpers.findClass(CLASS_ACTION, classLoader);

            XposedBridge.hookAllConstructors(globalActionsClass, new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                   mContext = (Context) param.args[0];
                   mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                   Context gbContext = mContext.createPackageContext(
                           GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                   Resources res = mContext.getResources();
                   Resources gbRes = gbContext.getResources();

                   int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                   int rebootSoftStrId = R.string.reboot_soft;
                   int recoveryStrId = R.string.poweroff_recovery;
                   int bootloaderStrId = R.string.poweroff_bootloader;
                   mRebootStr  = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
                   mRebootSoftStr = gbRes.getString(rebootSoftStrId);
                   mRecoveryStr = gbRes.getString(recoveryStrId);
                   mBootloaderStr = gbRes.getString(bootloaderStrId);
                   mExpandedDesktopStr = gbRes.getString(R.string.action_expanded_desktop_title);
                   mExpandedDesktopOnStr = gbRes.getString(R.string.action_expanded_desktop_on);
                   mExpandedDesktopOffStr = gbRes.getString(R.string.action_expanded_desktop_off);
                   mScreenshotStr = gbRes.getString(R.string.screenshot);

                   mRebootIcon = gbRes.getDrawable(R.drawable.ic_lock_reboot);
                   mRebootSoftIcon = gbRes.getDrawable(R.drawable.ic_lock_reboot_soft);
                   mRecoveryIcon = gbRes.getDrawable(R.drawable.ic_lock_recovery);
                   mBootloaderIcon = gbRes.getDrawable(R.drawable.ic_lock_bootloader);
                   mExpandedDesktopIcon = gbRes.getDrawable(R.drawable.ic_lock_expanded_desktop);
                   mScreenshotIcon = gbRes.getDrawable(R.drawable.ic_lock_screenshot);

                   mRebootItemList = new ArrayList<IIconListAdapterItem>();
                   mRebootItemList.add(new BasicIconListItem(mRebootStr, null, mRebootIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRebootSoftStr, null, mRebootSoftIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRecoveryStr, null, mRecoveryIcon, null));
                   if (!Utils.isMtkDevice()) {
                       mRebootItemList.add(new BasicIconListItem(mBootloaderStr, null, mBootloaderIcon, null));
                   }

                   mRebootConfirmStr = String.format(gbRes.getString(R.string.reboot_confirm),
                           gbRes.getString(Utils.isTablet() ? R.string.device_tablet : R.string.device_phone));
                   mRebootConfirmRecoveryStr = String.format(gbRes.getString(R.string.reboot_confirm_recovery),
                           gbRes.getString(Utils.isTablet() ? R.string.device_tablet : R.string.device_phone));
                   mRebootConfirmBootloaderStr = String.format(gbRes.getString(R.string.reboot_confirm_bootloader),
                           gbRes.getString(Utils.isTablet() ? R.string.device_tablet : R.string.device_phone));

                   if (DEBUG) log("GlobalActions constructed, resources set.");
               }
            });

            XposedHelpers.findAndHookMethod(globalActionsClass, "createDialog", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mRebootActionHook != null) {
                        if (DEBUG) log("Unhooking previous hook of reboot action item");
                        mRebootActionHook.unhook();
                        mRebootActionHook = null;
                    }
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mContext == null) return;

                    prefs.reload();

                    @SuppressWarnings("unchecked")
                    List<Object> mItems = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");
                    BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                    int index = 1;

                    // try to find out if reboot action item already exists in the list of GlobalActions items
                    // strategy:
                    // 1) check if Action has mIconResId field or mMessageResId field
                    // 2) check if the name of the corresponding resource contains "reboot" or "restart" substring
                    if (mRebootActionItem == null) {
                        if (DEBUG) log("Searching for existing reboot action item...");
                        Resources res = mContext.getResources();
                        for (Object o : mItems) {
                            // search for drawable
                            try {
                                Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                                String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                                if (DEBUG) log("Drawable resName = " + resName);
                                if (resName.contains("reboot") || resName.contains("restart")) {
                                    mRebootActionItem = o;
                                    break;
                                }
                            } catch (NoSuchFieldError nfe) {
                                // continue
                            } catch (Resources.NotFoundException resnfe) { 
                                // continue
                            } catch (IllegalArgumentException iae) {
                                // continue
                            }

                            if (mRebootActionItem == null) {
                                // search for text
                                try {
                                    Field f = XposedHelpers.findField(o.getClass(), "mMessageResId");
                                    String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                                    if (DEBUG) log("Text resName = " + resName);
                                    if (resName.contains("reboot") || resName.contains("restart")) {
                                        mRebootActionItem = o;
                                        break;
                                    }
                                } catch (NoSuchFieldError nfe) {
                                    // continue
                                } catch (Resources.NotFoundException resnfe) { 
                                    // continue
                                } catch (IllegalArgumentException iae) {
                                    // continue
                                }
                            }
                        }

                        if (mRebootActionItem == null) {
                            if (DEBUG) log("Existing Reboot action item NOT found! Creating new RebootAction item");
                            mRebootActionItemStockExists = false;
                            mRebootActionItem = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass }, 
                                    new RebootAction());
                        } else {
                            if (DEBUG) log("Existing Reboot action item found!");
                            mRebootActionItemStockExists = true;
                        }
                    }

                    // Add/hook reboot action if enabled
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWEROFF_ADVANCED, false)) {
                        if (mRebootActionItemStockExists) {
                            mRebootActionHook = XposedHelpers.findAndHookMethod(mRebootActionItem.getClass(), 
                                    "onPress", new XC_MethodReplacement () {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    RebootAction.showRebootDialog(mContext);
                                    return null;
                                }
                            });
                        } else {
                            // add to the second position
                            mItems.add(index, mRebootActionItem);
                        }
                        index++;
                    } else if (mRebootActionItemStockExists) {
                        index++;
                    }

                    // Add screenshot action if enabled
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWERMENU_SCREENSHOT, false)) {
                        if (mScreenshotAction == null) {
                            mScreenshotAction = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ScreenshotAction(mHandler));
                            if (DEBUG) log("mScreenshotAction created");
                        }
                        mItems.add(index++, mScreenshotAction);
                    }

                    // Add Expanded Desktop action if enabled
                    if (ExpandedDesktopAction.isExpandedDesktopEnabled(prefs)) {
                        if (mExpandedDesktopAction == null) {
                            mExpandedDesktopAction = Proxy.newProxyInstance(classLoader, 
                                    new Class<?>[] { actionClass },
                                        new ExpandedDesktopAction());
                            if (DEBUG) log("mExpandedDesktopAction created");
                        }
                        mItems.add(index++, mExpandedDesktopAction);
                    }

                    mAdapter.notifyDataSetChanged();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static class RebootAction implements InvocationHandler {
        private Context mContext;

        public RebootAction() {
        }

        public static void showRebootDialog(final Context context) {
            if (context == null) {
                if (DEBUG) log("Context is null - aborting");
                return;
            }

            try {
                if (DEBUG) log("about to build reboot dialog");

                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(mRebootStr)
                    .setAdapter(new IconListAdapter(context, mRebootItemList), new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (DEBUG) log("onClick() item = " + which);
                            handleReboot(context, mRebootStr, which);
                        }
                    })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
        
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                    });
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        private static void handleReboot(Context context, String caption, final int mode) {
            try {
                final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                String message = mRebootConfirmStr;
                if (mode == 2) {
                    message = mRebootConfirmRecoveryStr;
                } else if (mode == 3) {
                    message = mRebootConfirmBootloaderStr;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(caption)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (mode == 0) {
                                pm.reboot(null);
                            } else if (mode == 1) {
                                Class<?> classSm = XposedHelpers.findClass("android.os.ServiceManager", null);
                                Class<?> classIpm = XposedHelpers.findClass("android.os.IPowerManager.Stub", null);
                                IBinder b = (IBinder) XposedHelpers.callStaticMethod(
                                        classSm, "getService", Context.POWER_SERVICE);
                                Object ipm = XposedHelpers.callStaticMethod(classIpm, "asInterface", b);
                                XposedHelpers.callMethod(ipm, "crash", "Hot reboot");
                            } else if (mode == 2) {
                                pm.reboot("recovery");
                            } else if (mode == 3) {
                                pm.reboot("bootloader");
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mRebootIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mRebootStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                showRebootDialog(mContext);
                return null;
            } else if (methodName.equals("onLongPress")) {
                handleReboot(mContext, mRebootStr, 0);
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else {
                log("RebootAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }

    private static class ExpandedDesktopAction implements InvocationHandler {
        private Context mContext;
        private TextView mStatus;
        private Handler mHandler;

        public ExpandedDesktopAction() {
        }

        public static boolean isExpandedDesktopEnabled(XSharedPreferences prefs) {
            int edMode = GravityBoxSettings.ED_DISABLED;
            try {
                edMode = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
            } catch(NumberFormatException nfe) {
                log("Invalid value for PREF_KEY_EXPANDED_DESKTOP preference");
            }
            return (edMode != GravityBoxSettings.ED_DISABLED);
        }

        public static boolean isExpandedDesktopOn(Context context) {
            return (Settings.System.getInt(context.getContentResolver(),
                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0) == 1);
        }

        private void updateStatus() {
            mStatus.setText(isExpandedDesktopOn(mContext) ? 
                    mExpandedDesktopOnStr : mExpandedDesktopOffStr);
        }

        private void toggleStatus() {
            try {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Settings.System.putInt(mContext.getContentResolver(),
                                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                isExpandedDesktopOn(mContext) ? 0 : 1);
                        updateStatus();
                    }
                }, 200);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        };

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                mHandler = new Handler();

                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mExpandedDesktopIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mExpandedDesktopStr);

                mStatus = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                mStatus.setVisibility(View.VISIBLE);
                updateStatus();

                return v;
            } else if (methodName.equals("onPress")) {
                toggleStatus();
                return null;
            } else if (methodName.equals("onLongPress")) {
                return false;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else {
                log("ExpandedDesktopAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }

    private static class ScreenshotAction implements InvocationHandler {
        private Context mContext;
        private Handler mHandler;

        public ScreenshotAction(Handler handler) {
            mHandler = handler;
        }

        private void takeScreenshot() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(ModHwKeys.ACTION_SCREENSHOT);
                    mContext.sendBroadcast(intent);
                }
                
            }, 1000);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mScreenshotIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mScreenshotStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                takeScreenshot();
                return null;
            } else if (methodName.equals("onLongPress")) {
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else if (methodName.equals("showConditional")) {
                return true;
            } else {
                log("ScreenshotAction: Unhandled invocation method: " + methodName);
                return null;
            }
        }
    }
}