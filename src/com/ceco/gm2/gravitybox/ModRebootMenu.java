package com.ceco.gm2.gravitybox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

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

    private static Context mContext;
    private static Unhook mRebootActionHook;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {

        try {
            final Class<?> globalActionsClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS, classLoader);

            XposedBridge.hookAllConstructors(globalActionsClass, new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                   mContext = (Context) param.args[0];
                   XposedBridge.log("ModRebootMenu: GlobalActions constructed, mContext set");
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
                    // 1) check if ic_lock_reboot drawable exists in system resources
                    // 2) check if Action has field mIconResId that is equal to resId of ic_lock_reboot
                    log("Searching for existing reboot action item...");
                    Object rebootActionItem = null;
                    Resources res = mContext.getResources();
                    int rebootIconId = res.getIdentifier("ic_lock_reboot_radio", "drawable", "android"); 
                    // if zero, try once more with alternative name
                    if (rebootIconId == 0) {
                        log("ic_lock_reboot_radio not found, trying alternative ic_lock_reboot");
                        rebootIconId = res.getIdentifier("ic_lock_reboot", "drawable", "android");
                    }
                    if (rebootIconId != 0) {
                        log("Reboot icon resource found: " + rebootIconId + ". Searching for matching Action item");
                        // check if Action with given icon res id exists
                        for (Object o : mItems) {
                            try {
                                Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                                if ((Integer) f.get(o) == rebootIconId) {
                                    rebootActionItem = o;
                                    break;
                                }
                            } catch (NoSuchFieldError nfe) {
                                // continue
                            }
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
                        ClassLoader cl = param.thisObject.getClass().getClassLoader();
                        Class<?> clsAction = XposedHelpers.findClass(
                                "com.android.internal.policy.impl.GlobalActions.Action", cl);
                        Object action = Proxy.newProxyInstance(cl, new Class<?>[] { clsAction }, 
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
            XposedBridge.log("ModRebootMenu: mContext is null - aborting");
            return;
        }

        try {
            XposedBridge.log("ModRebootMenu: about to get resources and set items");
            Context gbContext = mContext.createPackageContext(
                    GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            Resources gbRes = gbContext.getResources();
            Resources res = mContext.getResources();
    
            int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
            int rebootSoftStrId = gbRes.getIdentifier("reboot_soft", "string", GravityBox.PACKAGE_NAME);
            int recoveryStrId = gbRes.getIdentifier("poweroff_recovery", "string", GravityBox.PACKAGE_NAME);
            Drawable rebootIcon = gbRes.getDrawable(
                    gbRes.getIdentifier("ic_lock_reboot", "drawable", GravityBox.PACKAGE_NAME));
            Drawable rebootSoftIcon = gbRes.getDrawable(
                    gbRes.getIdentifier("ic_lock_reboot_soft", "drawable", GravityBox.PACKAGE_NAME));
            Drawable recoveryIcon = gbRes.getDrawable(
                    gbRes.getIdentifier("ic_lock_recovery", "drawable", GravityBox.PACKAGE_NAME));
    
            final String[] items = new String[3];
            items[0] = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
            items[1] = (rebootSoftStrId == 0) ? "Soft reboot" : gbRes.getString(rebootSoftStrId);
            items[2] = (recoveryStrId == 0) ? "Recovery" : gbRes.getString(recoveryStrId);
    
            ArrayList<IIconListAdapterItem> itemList = new ArrayList<IIconListAdapterItem>();
            itemList.add(new BasicIconListItem(items[0], null, rebootIcon, null));
            itemList.add(new BasicIconListItem(items[1], null, rebootSoftIcon, null));
            itemList.add(new BasicIconListItem(items[2], null, recoveryIcon, null));
    
            XposedBridge.log("ModRebootMenu: about to build reboot dialog");
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(items[0])
                .setAdapter(new IconListAdapter(mContext, itemList), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        XposedBridge.log("ModRebootMenu: onClick() item = " + which);
                        handleReboot(mContext, items[0], which);
                    }
                })
                .setNegativeButton(res.getString(res.getIdentifier("no", "string", PACKAGE_NAME)),
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
            
            Resources gbRes = mContext.createPackageContext(
                    GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY).getResources();
            String message = "";
            if (mode == 0 || mode == 1) {
                message = gbRes.getString(gbRes.getIdentifier(
                        "reboot_confirm", "string", GravityBox.PACKAGE_NAME));
            } else if (mode == 2) {
                message = gbRes.getString(gbRes.getIdentifier(
                        "reboot_confirm_recovery", "string", GravityBox.PACKAGE_NAME));
            }

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
                Context gbContext = mContext.createPackageContext(
                        GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                Resources res = mContext.getResources();
                Resources gbRes = gbContext.getResources();

                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                Drawable recoveryIcon = gbRes.getDrawable(
                        gbRes.getIdentifier("ic_lock_reboot", "drawable", GravityBox.PACKAGE_NAME));
                icon.setImageDrawable(recoveryIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                messageView.setText((rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId));

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                showDialog();
                return null;
            } else if (methodName.equals("onLongPress")) {
                return false;
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