package com.ceco.gm2.gravitybox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixCalendar {
    public static final String TAG = "FixCalendar";
    public static final String PACKAGE_NAME = "com.android.calendar";
    private static final boolean DEBUG = false;

    // com.android.calendar specific
    private static final String CLASS_ALERT_RECEIVER = "com.android.calendar.alerts.AlertReceiver";
    private static final String CLASS_DISMISS_ALARM_SERVICE = "com.android.calendar.alerts.DismissAlarmsService";
    private static final String CLASS_QUICK_RESPONSE_ACTIVITY = "com.android.calendar.alerts.QuickResponseActivity";
    private static final String CLASS_ALERT_SERVICE = "com.android.calendar.alerts.AlertService";    
    private static final String DELETE_ALL_ACTION = "com.android.calendar.DELETEALL";
    private static final String MAIL_ACTION = "com.android.calendar.MAIL";
    private static final String EXTRA_EVENT_ID = "eventid";
    private static final String QRA_EXTRA_EVENT_ID = "eventId";

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        XposedBridge.log(TAG + ": init");

        try {
            final Class<?> alertReceiverClass = XposedHelpers.findClass(CLASS_ALERT_RECEIVER, classLoader);
            final Class<?> dismissAlarmServiceClass = XposedHelpers.findClass(CLASS_DISMISS_ALARM_SERVICE, classLoader);
            final Class<?> quickResponseActivityClass = XposedHelpers.findClass(CLASS_QUICK_RESPONSE_ACTIVITY, classLoader);
            final Class<?> alertServiceClass = XposedHelpers.findClass(CLASS_ALERT_SERVICE, classLoader);

            XposedHelpers.findAndHookMethod(alertReceiverClass, "onReceive", Context.class, Intent.class,
                    new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    Intent intent = (Intent) param.args[1];
                    if (DEBUG) {
                        XposedBridge.log(TAG + ": onReceive: a=" + intent.getAction() + " " + intent.toString());
                    }
                    if (DELETE_ALL_ACTION.equals(intent.getAction())) {

                        /* The user has clicked the "Clear All Notifications"
                         * buttons so dismiss all Calendar alerts.
                         */
                        Intent serviceIntent = new Intent(context, dismissAlarmServiceClass);
                        context.startService(serviceIntent);
                    } else if (MAIL_ACTION.equals(intent.getAction())) {
                        // Close the notification shade.
                        Intent closeNotificationShadeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                        context.sendBroadcast(closeNotificationShadeIntent);

                        // Now start the email intent.
                        final long eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1);
                        if (eventId != -1) {
                            Intent i = new Intent(context, quickResponseActivityClass);
                            i.putExtra(QRA_EXTRA_EVENT_ID, eventId);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(i);
                        }
                    } else {
                        Intent i = new Intent();
                        i.setClass(context, alertServiceClass);
                        i.putExtras(intent);
                        i.putExtra("action", intent.getAction());
                        Uri uri = intent.getData();

                        // This intent might be a BOOT_COMPLETED so it might not have a Uri.
                        if (uri != null) {
                            i.putExtra("uri", uri.toString());
                        }
                        
                        if (DEBUG) {
                            XposedBridge.log(TAG + ": invoking beginStartingService: a=" + 
                                    intent.getAction() + " " + intent.toString());
                        }
                        XposedHelpers.callMethod(param.thisObject, "beginStartingService", context, i);
                    }

                    return null;
                }
                
            });
            
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}