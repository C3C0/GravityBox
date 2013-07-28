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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.PowerManager;
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

public class ModRebootMenu {
    private static final String TAG = "ModRebootMenu";
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.internal.policy.impl.GlobalActions";
    public static final String CLASS_ACTION = "com.android.internal.policy.impl.GlobalActions.Action";

    private static Context mContext;
    private static String mRebootStr;
    private static String mRebootSoftStr;
    private static String mRecoveryStr;
    private static Drawable mRebootIcon;
    private static Drawable mRebootSoftIcon;
    private static Drawable mRecoveryIcon;
    private static List<IIconListAdapterItem> mRebootItemList;
    private static String mRebootConfirmStr;
    private static String mRebootConfirmRecoveryStr;
    private static Unhook mRebootActionHook;

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
                   Context gbContext = mContext.createPackageContext(
                           GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                   Resources res = mContext.getResources();
                   Resources gbRes = gbContext.getResources();

                   int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                   int rebootSoftStrId = gbRes.getIdentifier("reboot_soft", "string", GravityBox.PACKAGE_NAME);
                   int recoveryStrId = gbRes.getIdentifier("poweroff_recovery", "string", GravityBox.PACKAGE_NAME);
                   mRebootStr  = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
                   mRebootSoftStr = gbRes.getString(rebootSoftStrId);
                   mRecoveryStr = gbRes.getString(recoveryStrId);

                   mRebootIcon = gbRes.getDrawable(
                           gbRes.getIdentifier("ic_lock_reboot", "drawable", GravityBox.PACKAGE_NAME));
                   mRebootSoftIcon = gbRes.getDrawable(
                           gbRes.getIdentifier("ic_lock_reboot_soft", "drawable", GravityBox.PACKAGE_NAME));
                   mRecoveryIcon = gbRes.getDrawable(
                           gbRes.getIdentifier("ic_lock_recovery", "drawable", GravityBox.PACKAGE_NAME));

                   mRebootItemList = new ArrayList<IIconListAdapterItem>();
                   mRebootItemList.add(new BasicIconListItem(mRebootStr, null, mRebootIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRebootSoftStr, null, mRebootSoftIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRecoveryStr, null, mRecoveryIcon, null));

                   mRebootConfirmStr = gbRes.getString(gbRes.getIdentifier(
                           "reboot_confirm", "string", GravityBox.PACKAGE_NAME));
                   mRebootConfirmRecoveryStr = gbRes.getString(gbRes.getIdentifier(
                           "reboot_confirm_recovery", "string", GravityBox.PACKAGE_NAME));
                   
                   log("GlobalActions constructed, resources set.");
               }
            });

            XposedHelpers.findAndHookMethod(globalActionsClass, "createDialog", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mRebootActionHook != null) {
                        log("Unhooking previous hook of reboot action item");
                        mRebootActionHook.unhook();
                        mRebootActionHook = null;
                    }
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mContext == null) return;

                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWEROFF_ADVANCED, false)) {
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Object> mItems = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");

                    // try to find out if reboot action item already exists in the list of GlobalActions items
                    // strategy:
                    // 1) check if Action has mIconResId field
                    // 2) check if the name of the corresponding resource contains "reboot" substring
                    log("Searching for existing reboot action item...");
                    Object rebootActionItem = null;
                    Resources res = mContext.getResources();
                    for (Object o : mItems) {
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                            String resName = res.getResourceName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("resName = " + resName);
                            if (resName.contains("reboot")) {
                                rebootActionItem = o;
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

                    if (rebootActionItem != null) {
                        log("Existing Reboot action item found! Replacing onPress()");
                        mRebootActionHook = XposedHelpers.findAndHookMethod(rebootActionItem.getClass(), 
                                "onPress", new XC_MethodReplacement () {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                showDialog();
                                return null;
                            }
                        });
                    } else {
                        log("Existing Reboot action item NOT found! Adding new RebootAction item");
                        Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass }, 
                                new RebootAction());
                        // add to the second position
                        mItems.add(1, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void showDialog() {
        if (mContext == null) {
            log("mContext is null - aborting");
            return;
        }

        try {
            log("about to build reboot dialog");

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(mRebootStr)
                .setAdapter(new IconListAdapter(mContext, mRebootItemList), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        log("onClick() item = " + which);
                        handleReboot(mContext, mRebootStr, which);
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
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void handleReboot(Context context, String caption, final int mode) {
        try {
            final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            final String message = (mode == 0 || mode == 1) ? mRebootConfirmStr : mRebootConfirmRecoveryStr;

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
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
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static class RebootAction implements InvocationHandler {
        private Context mContext;

        public RebootAction() {
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
                showDialog();
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
            } else {
                return null;
            }
        }
        
    }
}