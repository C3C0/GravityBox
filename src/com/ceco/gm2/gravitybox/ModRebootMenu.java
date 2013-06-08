package com.ceco.gm2.gravitybox;

import java.util.ArrayList;

import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import com.ceco.gm2.gravitybox.adapters.*;

public class ModRebootMenu {
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.internal.policy.impl.GlobalActions";
    public static final String CLASS_GLOBAL_ACTIONS_POWEROFF = "com.android.internal.policy.impl.GlobalActions$2";
    private static Context mContext;

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        XposedBridge.log("ModRebootMenu: init");

        try {
            Class<?> globalActionsClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS, classLoader);
            Class<?> globalActionsPowerOffClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS_POWEROFF, classLoader);

            XposedBridge.hookAllConstructors(globalActionsClass, new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                   mContext = (Context) param.args[0];
                   XposedBridge.log("ModRebootMenu: GlobalActions constructed, mContext set");
               }
            });

            XposedHelpers.findAndHookMethod(globalActionsPowerOffClass, "onPress", new XC_MethodHook () {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWEROFF_ADVANCED, false)) {
                        return;
                    }

                    if (mContext == null) {
                        XposedBridge.log("ModRebootMenu: mContext is null...falling back to original method");
                        return;
                    }

                    XposedBridge.log("ModRebootMenu: about to get resources and set items");
                    Context gbContext = mContext.createPackageContext(
                            GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    Resources gbRes = gbContext.getResources();
                    Resources res = mContext.getResources();

                    int powerOffStrId = res.getIdentifier("power_off", "string", PACKAGE_NAME);
                    int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                    int recoveryStrId = gbRes.getIdentifier("poweroff_recovery", "string", GravityBox.PACKAGE_NAME);
                    Drawable powerOffIcon = gbRes.getDrawable(
                            gbRes.getIdentifier("ic_lock_power_off", "drawable", GravityBox.PACKAGE_NAME));
                    Drawable rebootIcon = gbRes.getDrawable(
                            gbRes.getIdentifier("ic_lock_reboot", "drawable", GravityBox.PACKAGE_NAME));
                    Drawable recoveryIcon = gbRes.getDrawable(
                            gbRes.getIdentifier("ic_lock_recovery", "drawable", GravityBox.PACKAGE_NAME));

                    String[] items = new String[3];
                    items[0] = (powerOffStrId == 0) ? "Power off" : res.getString(powerOffStrId);
                    items[1] = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
                    items[2] = (recoveryStrId == 0) ? "Recovery" : gbRes.getString(recoveryStrId);

                    ArrayList<IIconListAdapterItem> itemList = new ArrayList<IIconListAdapterItem>();
                    itemList.add(new BasicIconListItem(items[0], null, powerOffIcon, null));
                    itemList.add(new BasicIconListItem(items[1], null, rebootIcon, null));
                    itemList.add(new BasicIconListItem(items[2], null, recoveryIcon, null));

                    XposedBridge.log("ModRebootMenu: about to build power off dialog");
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setTitle(items[0])
                        .setAdapter(new IconListAdapter(mContext, itemList), new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                                XposedBridge.log("ModRebootMenu: onClick() item = " + which);
                                switch (which) {
                                    case 0: // power off - fall back to original method
                                        try {
                                            XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                                        } catch (Exception e) {
                                            XposedBridge.log(e);
                                        }
                                        break;
                                    case 1: // reboot
                                        pm.reboot(null);
                                        break;
                                    case 2: // reboot recovery
                                        pm.reboot("recovery");
                                        break;
                                }
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
                    param.setResult(null);
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}