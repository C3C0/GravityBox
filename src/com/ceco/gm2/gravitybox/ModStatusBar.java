package com.ceco.gm2.gravitybox;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.ceco.gm2.gravitybox.preference.AppPickerPreference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModStatusBar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModStatusBar";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_TICKER = "com.android.systemui.statusbar.phone.PhoneStatusBar$MyTicker";
    private static final String CLASS_PHONE_STATUSBAR_POLICY = "com.android.systemui.statusbar.phone.PhoneStatusBarPolicy";
    private static final boolean DEBUG = false;

    private static ViewGroup mIconArea;
    private static ViewGroup mRootView;
    private static LinearLayout mLayoutClock;
    private static TextView mClock;
    private static TextView mClockExpanded;
    private static Object mPhoneStatusBar;
    private static Context mContext;
    private static int mAnimPushUpOut;
    private static int mAnimPushDownIn;
    private static int mAnimFadeIn;
    private static boolean mClockCentered = false;
    private static int mClockOriginalPaddingLeft;
    private static boolean mClockShowDow = false;
    private static boolean mAmPmHide = false;
    private static boolean mClockHide = false;
    private static String mClockLink;
    private static boolean mAlarmHide = false;
    private static Object mPhoneStatusBarPolicy;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_CLOCK_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CENTER_CLOCK)) {
                    setClockPosition(intent.getBooleanExtra(GravityBoxSettings.EXTRA_CENTER_CLOCK, false));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_DOW)) {
                    mClockShowDow = intent.getBooleanExtra(GravityBoxSettings.EXTRA_CLOCK_DOW, false);
                    if (mClock != null) {
                        XposedHelpers.callMethod(mClock, "updateClock");
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_AMPM_HIDE)) {
                    mAmPmHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_AMPM_HIDE, false);
                    if (mClock != null) {
                        XposedHelpers.callMethod(mClock, "updateClock");
                    }
                    if (mClockExpanded != null) {
                        XposedHelpers.callMethod(mClockExpanded, "updateClock");
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_HIDE)) {
                    mClockHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_CLOCK_HIDE, false);
                    if (mClock != null) {
                        mClock.setVisibility(mClockHide ? View.GONE : View.VISIBLE);
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_LINK)) {
                    mClockLink = intent.getStringExtra(GravityBoxSettings.EXTRA_CLOCK_LINK);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_ALARM_HIDE)) {
                    mAlarmHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_ALARM_HIDE, false);
                    if (mPhoneStatusBarPolicy != null) {
                        String alarmFormatted = Settings.System.getString(context.getContentResolver(),
                                Settings.System.NEXT_ALARM_FORMATTED);
                        Intent i = new Intent();
                        i.putExtra("alarmSet", (alarmFormatted != null && !alarmFormatted.isEmpty()));
                        XposedHelpers.callMethod(mPhoneStatusBarPolicy, "updateAlarm", i);
                    }
                }
            }
        }
    };

    public static void initResources(final XSharedPreferences prefs, final InitPackageResourcesParam resparam) {
        try {
            String layout = Utils.hasGeminiSupport() ? "gemini_super_status_bar" : "super_status_bar";
            resparam.res.hookLayout(PACKAGE_NAME, "layout", layout, new XC_LayoutInflated() {

                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    prefs.reload();
                    mClockShowDow = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_DOW, false);
                    mAmPmHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE, false);
                    mClockHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_HIDE, false);
                    mClockLink = prefs.getString(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_LINK, null);
                    mAlarmHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_ALARM_ICON_HIDE, false);

                    String iconAreaId = Build.VERSION.SDK_INT > 16 ?
                            "system_icon_area" : "icons";
                    mIconArea = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier(iconAreaId, "id", PACKAGE_NAME));
                    if (mIconArea == null) return;

                    mRootView = Build.VERSION.SDK_INT > 16 ?
                            (ViewGroup) mIconArea.getParent().getParent() :
                            (ViewGroup) mIconArea.getParent();
                    if (mRootView == null) return;

                    // find statusbar clock
                    mClock = (TextView) mIconArea.findViewById(
                            liparam.res.getIdentifier("clock", "id", PACKAGE_NAME));
                    if (mClock == null) return;
                    ModStatusbarColor.setClock(mClock);
                    // use this additional field to identify the instance of Clock that resides in status bar
                    XposedHelpers.setAdditionalInstanceField(mClock, "sbClock", true);
                    mClockOriginalPaddingLeft = mClock.getPaddingLeft();
                    if (mClockHide) {
                        mClock.setVisibility(View.GONE);
                    }

                    // find notification panel clock
                    String panelHolderId = Build.VERSION.SDK_INT > 16 ?
                            "panel_holder" : "notification_panel";
                    final ViewGroup panelHolder = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier(panelHolderId, "id", PACKAGE_NAME));
                    if (panelHolder != null) {
                        mClockExpanded = (TextView) panelHolder.findViewById(
                                liparam.res.getIdentifier("clock", "id", PACKAGE_NAME));
                        if (mClockExpanded != null && Build.VERSION.SDK_INT < 17) {
                            mClockExpanded.setClickable(true);
                            mClockExpanded.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    launchClockApp();
                                }
                            });
                        }
                    }
                    
                    // inject new clock layout
                    mLayoutClock = new LinearLayout(liparam.view.getContext());
                    mLayoutClock.setLayoutParams(new LinearLayout.LayoutParams(
                                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                    mLayoutClock.setGravity(Gravity.CENTER);
                    mLayoutClock.setVisibility(View.GONE);
                    mRootView.addView(mLayoutClock);
                    log("mLayoutClock injected");

                    XposedHelpers.findAndHookMethod(mClock.getClass(), "getSmallTime", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // is this a status bar Clock instance?
                            // yes, if it contains our additional sbClock field
                            if (DEBUG) log("getSmallTime() called. mAmPmHide=" + mAmPmHide);
                            Object sbClock = XposedHelpers.getAdditionalInstanceField(param.thisObject, "sbClock");
                            if (DEBUG) log("Is statusbar clock: " + (sbClock == null ? "false" : "true"));
                            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                            String clockText = param.getResult().toString();
                            if (DEBUG) log("Original clockText: '" + clockText + "'");
                            String amPm = calendar.getDisplayName(
                                    Calendar.AM_PM, Calendar.SHORT, Locale.getDefault());
                            if (DEBUG) log("Locale specific AM/PM string: '" + amPm + "'");
                            int amPmIndex = clockText.indexOf(amPm);
                            if (DEBUG) log("Original AM/PM index: " + amPmIndex);
                            if (mAmPmHide && amPmIndex != -1) {
                                clockText = clockText.replace(amPm, "").trim();
                                if (DEBUG) log("AM/PM removed. New clockText: '" + clockText + "'");
                                amPmIndex = -1;
                            } else if (!mAmPmHide 
                                        && !DateFormat.is24HourFormat(mClock.getContext()) 
                                        && amPmIndex == -1) {
                                // insert AM/PM if missing
                                clockText += " " + amPm;
                                amPmIndex = clockText.indexOf(amPm);
                                if (DEBUG) log("AM/PM added. New clockText: '" + clockText + "'; New AM/PM index: " + amPmIndex);
                            }
                            CharSequence dow = "";
                            // apply day of week only to statusbar clock, not the notification panel clock
                            if (mClockShowDow && sbClock != null) {
                                dow = calendar.getDisplayName(
                                        Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) + " ";
                            }
                            clockText = dow + clockText;
                            SpannableStringBuilder sb = new SpannableStringBuilder(clockText);
                            sb.setSpan(new RelativeSizeSpan(0.7f), 0, dow.length(), 
                                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                            if (amPmIndex > -1) {
                                int offset = Character.isWhitespace(clockText.charAt(dow.length() + amPmIndex - 1)) ?
                                        1 : 0;
                                sb.setSpan(new RelativeSizeSpan(0.7f), dow.length() + amPmIndex - offset, 
                                        dow.length() + amPmIndex + amPm.length(), 
                                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                            }
                            if (DEBUG) log("Final clockText: '" + sb + "'");
                            param.setResult(sb);
                        }
                    });

                    setClockPosition(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_CENTER_CLOCK, false));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> phoneStatusBarClass =
                    XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> tickerClass =
                    XposedHelpers.findClass(CLASS_TICKER, classLoader);
            final Class<?> phoneStatusBarPolicyClass = 
                    XposedHelpers.findClass(CLASS_PHONE_STATUSBAR_POLICY, classLoader);

            final Class<?>[] loadAnimParamArgs = new Class<?>[2];
            loadAnimParamArgs[0] = int.class;
            loadAnimParamArgs[1] = Animation.AnimationListener.class;

            XposedBridge.hookAllConstructors(phoneStatusBarPolicyClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneStatusBarPolicy = param.thisObject;
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarPolicyClass, 
                    "updateAlarm", Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object sbService = XposedHelpers.getObjectField(param.thisObject, "mService");
                    if (sbService != null) {
                        boolean alarmSet = ((Intent)param.args[0]).getBooleanExtra("alarmSet", false);
                        XposedHelpers.callMethod(sbService, "setIconVisibility", "alarm_clock",
                                (alarmSet && !mAlarmHide));
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneStatusBar = param.thisObject;
                    mContext = (Context) XposedHelpers.getObjectField(mPhoneStatusBar, "mContext");
                    Resources res = mContext.getResources();
                    mAnimPushUpOut = res.getIdentifier("push_up_out", "anim", "android");
                    mAnimPushDownIn = res.getIdentifier("push_down_in", "anim", "android");
                    mAnimFadeIn = res.getIdentifier("fade_in", "anim", "android");

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_CLOCK_CHANGED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "showClock", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mClock == null) return;

                    boolean visible = (Boolean) param.args[0] && !mClockHide;
                    mClock.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(phoneStatusBarClass, "startActivityDismissingKeyguard", 
                        Intent.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mClockLink == null) return;

                        Intent i = (Intent) param.args[0];
                        if (i != null && Intent.ACTION_QUICK_CLOCK.equals(i.getAction())) {
                            final ComponentName cn = getComponentNameFromClockLink();
                            if (cn != null) {
                                i = new Intent();
                                i.setComponent(cn);
                                param.args[0] = i;
                            }
                        }
                    }
                });
            }

            XposedHelpers.findAndHookMethod(tickerClass, "tickerStarting", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mLayoutClock == null || !mClockCentered) return;

                    mLayoutClock.setVisibility(View.GONE);
                    Animation anim = (Animation) XposedHelpers.callMethod(
                            mPhoneStatusBar, "loadAnim", loadAnimParamArgs, mAnimPushUpOut, null);
                    mLayoutClock.startAnimation(anim);
                }
            });

            XposedHelpers.findAndHookMethod(tickerClass, "tickerDone", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mLayoutClock == null || !mClockCentered) return;

                    mLayoutClock.setVisibility(View.VISIBLE);
                    Animation anim = (Animation) XposedHelpers.callMethod(
                            mPhoneStatusBar, "loadAnim", loadAnimParamArgs, mAnimPushDownIn, null);
                    mLayoutClock.startAnimation(anim);
                }
            });

            XposedHelpers.findAndHookMethod(tickerClass, "tickerHalting", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mLayoutClock == null || !mClockCentered) return;

                    mLayoutClock.setVisibility(View.VISIBLE);
                    Animation anim = (Animation) XposedHelpers.callMethod(
                            mPhoneStatusBar, "loadAnim", loadAnimParamArgs, mAnimFadeIn, null);
                    mLayoutClock.startAnimation(anim);
                }
            });
        }
        catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setClockPosition(boolean center) {
        if (mClockCentered == center || mClock == null || 
                mIconArea == null || mLayoutClock == null) {
            return;
        }

        if (center) {
            mClock.setGravity(Gravity.CENTER);
            mClock.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mClock.setPadding(0, 0, 0, 0);
            mIconArea.removeView(mClock);
            mLayoutClock.addView(mClock);
            mLayoutClock.setVisibility(View.VISIBLE);
            log("Clock set to center position");
        } else {
            mClock.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            mClock.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            mClock.setPadding(mClockOriginalPaddingLeft, 0, 0, 0);
            mLayoutClock.removeView(mClock);
            mIconArea.addView(mClock);
            mLayoutClock.setVisibility(View.GONE);
            log("Clock set to normal position");
        }

        mClockCentered = center;
    }

    private static ComponentName getComponentNameFromClockLink() {
        if (mClockLink == null) return null;

        try {
            String[] splitValue = mClockLink.split(AppPickerPreference.SEPARATOR);
            ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
            return cn;
        } catch (Exception e) {
            log("Error getting ComponentName from clock link: " + e.getMessage());
            return null;
        }
    }

    private static void launchClockApp() {
        if (mContext == null || mClockLink == null) return;

        try {
            Intent i = new Intent();
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.setComponent(getComponentNameFromClockLink());
            mContext.startActivity(i);
            if (mPhoneStatusBar != null) {
                XposedHelpers.callMethod(mPhoneStatusBar, Build.VERSION.SDK_INT > 16 ?
                        "animateCollapsePanels" : "animateCollapse");
            }
        } catch (ActivityNotFoundException e) {
            log("Error launching assigned app for clock: " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}