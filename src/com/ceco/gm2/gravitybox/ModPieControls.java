package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ceco.gm2.gravitybox.pie.PieController;
import com.ceco.gm2.gravitybox.pie.PieController.Position;
import com.ceco.gm2.gravitybox.pie.PieLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModPieControls {
    private static final String TAG = "GB:ModPieController";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_INPUT = false;

    private static final String CLASS_BASE_STATUSBAR = "com.android.systemui.statusbar.BaseStatusBar";
    private static final String CLASS_SYSTEM_UI = "com.android.systemui.SystemUI";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    public static final int STATUS_BAR_DISABLE_HOME = 0x00200000;
    public static final int STATUS_BAR_DISABLE_SEARCH = 0x02000000;
    public static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;
    public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;

    public static final String SETTING_PIE_CONTROLS = "pie_controls";
    public static final String SETTING_PIE_SEARCH = "pie_search";
    public static final String SETTING_PIE_GRAVITY = "pie_gravity";
    public static final String SETTING_PIE_SIZE = "pie_size";
    public static final String SETTING_PIE_TRIGGER_SIZE = "pie_trigger_size";

    public static final int PIE_DISABLED = 0;
    public static final int PIE_ENABLED_ALWAYS = 1;
    public static final int PIE_ENABLED_ED = 2;
    public static final int PIE_ENABLED_ED_NAVBAR_HIDDEN = 3;

    private static PieController mPieController;
    private static PieLayout mPieContainer;
    private static int mPieTriggerSlots;
    private static View[] mPieTrigger = new View[PieController.Position.values().length];
    private static Context mContext;
    private static Context mGbContext;
    private static WindowManager mWindowManager;
    private static PieSettingsObserver mSettingsObserver;
    private static boolean mShowMenuItem;
    private static boolean mAlwaysShowMenuItem;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            ContentResolver cr = mContext.getContentResolver();
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_PIE_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_ENABLE)) {
                    Settings.System.putInt(cr, SETTING_PIE_CONTROLS,
                            intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_ENABLE, 0));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_SEARCH)) {
                    int search = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_SEARCH, false) ? 1 : 0;
                    Settings.System.putInt(cr, SETTING_PIE_SEARCH, search);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_TRIGGERS)) {
                    String[] triggers = intent.getStringArrayExtra(
                            GravityBoxSettings.EXTRA_PIE_TRIGGERS);
                    int tslots = 0;
                    for (String s : triggers) {
                        try {
                            tslots |= Integer.valueOf(s);
                        } catch (NumberFormatException e) {
                            XposedBridge.log(e);
                        }
                    }
                    Settings.System.putInt(cr, SETTING_PIE_GRAVITY, tslots);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_TRIGGER_SIZE)) {
                    int size = intent.getIntExtra(GravityBoxSettings.EXTRA_PIE_TRIGGER_SIZE, 5);
                    Settings.System.putInt(cr, SETTING_PIE_TRIGGER_SIZE, size);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_SIZE)) {
                    float size = (float) intent.getIntExtra(
                            GravityBoxSettings.EXTRA_PIE_SIZE, 1000) / 1000f;
                    Settings.System.putFloat(cr, SETTING_PIE_SIZE, size);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE)) {
                    mShowMenuItem = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE, false);
                    if (mPieController != null) {
                        mPieController.setMenuVisibility(mShowMenuItem | mAlwaysShowMenuItem);
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_PIE_MENU)) {
                    mAlwaysShowMenuItem = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_PIE_MENU, false);
                    if (mPieController != null) {
                        mPieController.setMenuVisibility(mShowMenuItem | mAlwaysShowMenuItem);
                    }
                }
            }
        }
    };

    private static View.OnTouchListener mPieTriggerOnTouchHandler = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            final PieController.Tracker tracker = (PieController.Tracker)v.getTag();

            if (tracker == null) {
                if (DEBUG_INPUT) {
                    log("Pie trigger onTouch: action: " + action + ", ("
                            + event.getAxisValue(MotionEvent.AXIS_X) + ","
                            + event.getAxisValue(MotionEvent.AXIS_Y) + ") position: NULL returning: false");
                }
                return false;
            }

            if (!mPieController.isShowing()) {
                if (event.getPointerCount() > 1) {
                    if (DEBUG_INPUT) {
                        log("Pie trigger onTouch: action: " + action
                                + ", (to many pointers) position: " + tracker.position.name()
                                + " returning: false");
                    }
                    return false;
                }

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        tracker.start(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (tracker.move(event)) {
                            if (DEBUG) {
                                log("Pie control activated on: ("
                                        + event.getAxisValue(MotionEvent.AXIS_X) + ","
                                        + event.getAxisValue(MotionEvent.AXIS_Y) + ") with position: "
                                        + tracker.position.name());
                            }
                            // send the activation to the controller
                            mPieController.activateFromTrigger(v, event, tracker.position);
                            // forward a spoofed ACTION_DOWN event
                            MotionEvent echo = (MotionEvent) XposedHelpers.callMethod(event, "copy");
                            echo.setAction(MotionEvent.ACTION_DOWN);
                            return mPieContainer.onTouch(v, echo);
                        }
                        break;
                    default:
                        // whatever it was, we are giving up on this one
                        tracker.active = false;
                        break;
                }
            } else {
                if (DEBUG_INPUT) {
                    log("Pie trigger onTouch: action: " + action + ", ("
                            + event.getAxisValue(MotionEvent.AXIS_X) + ","
                            + event.getAxisValue(MotionEvent.AXIS_Y)
                            + ") position: " + tracker.position.name() + " delegating");
                }
                return mPieContainer.onTouch(v, event);
            }
            if (DEBUG_INPUT) {
                log("Pie trigger onTouch: action: " + action + ", ("
                        + event.getAxisValue(MotionEvent.AXIS_X) + ","
                        + event.getAxisValue(MotionEvent.AXIS_Y) + ") position: "
                        + tracker.position.name() + " returning: " + tracker.active);
            }
            return tracker.active;
        }

    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> baseStatusBarClass = XposedHelpers.findClass(CLASS_BASE_STATUSBAR, classLoader);
            final Class<?> systemUiClass = XposedHelpers.findClass(CLASS_SYSTEM_UI, classLoader);
            final Class<?> phoneStatusBarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);

            mShowMenuItem = prefs.getBoolean(GravityBoxSettings.PREF_KEY_HWKEYS_DISABLE, false);
            mAlwaysShowMenuItem = prefs.getBoolean(GravityBoxSettings.PREF_KEY_PIE_CONTROL_MENU, false);

            XposedHelpers.findAndHookMethod(baseStatusBarClass, "start", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("BaseStatusBar starting...");
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                    mPieController = new PieController(mContext, mGbContext);
                    mPieController.attachTo(param.thisObject);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_PIE_CHANGED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);

                    mSettingsObserver = new PieSettingsObserver(new Handler());
                    mSettingsObserver.onChange(true);
                    mSettingsObserver.observe();
                }
            });

            XposedHelpers.findAndHookMethod(systemUiClass, 
                    "onConfigurationChanged", Configuration.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    attachPie();
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "disable", int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mPieController == null) return;

                    final int old = XposedHelpers.getIntField(param.thisObject, "mDisabled");
                    final int state = (Integer) param.args[0];
                    final int diff = state ^ old;
                    if ((diff & (STATUS_BAR_DISABLE_HOME
                            | STATUS_BAR_DISABLE_RECENT
                            | STATUS_BAR_DISABLE_BACK
                            | STATUS_BAR_DISABLE_SEARCH)) != 0) {
                        mPieController.setDisabledFlags(state);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, 
                    "setNavigationIconHints", int.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mPieController == null) return;

                    mPieController.setNavigationIconHints((Integer)param.args[0]);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass,
                    "topAppWindowChanged", boolean.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mPieController == null) return;

                    mPieController.setMenuVisibility((Boolean)param.args[0] 
                            | mShowMenuItem
                            | mAlwaysShowMenuItem);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static final class PieSettingsObserver extends ContentObserver {
        PieSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_PIE_CONTROLS), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_PIE_GRAVITY), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_PIE_TRIGGER_SIZE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_MODE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) log("PieSettingsObserver onChange()");
            mPieTriggerSlots = Settings.System.getInt(mContext.getContentResolver(),
                    SETTING_PIE_GRAVITY, Position.BOTTOM.FLAG);

            attachPie();
        }
    }

    private static boolean isPieEnabled() {
        final ContentResolver cr = mContext.getContentResolver();
        final int pieMode = Settings.System.getInt(cr, SETTING_PIE_CONTROLS, 0);
        if (DEBUG) log("isPieEnabled: SETTING_PIE_CONTROLS = " + pieMode);

        switch(pieMode) {
            case PIE_DISABLED: return false;
            case PIE_ENABLED_ALWAYS: return true;
            case PIE_ENABLED_ED:
            case PIE_ENABLED_ED_NAVBAR_HIDDEN:
                final int edMode = Settings.System.getInt(
                        cr, ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_MODE, 0);
                if (DEBUG) log("isPieEnabled: SETTING_EXPANDED_DESKTOP_MODE = " + edMode);
                final boolean edEnabled = Settings.System.getInt(
                        cr, ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0) == 1;
                if (DEBUG) log("isPieEnabled: SETTING_EXPANDED_DESKTOP_STATE = " + edEnabled);
                return edEnabled && (pieMode == PIE_ENABLED_ED ||
                        (pieMode == PIE_ENABLED_ED_NAVBAR_HIDDEN 
                            && (edMode == GravityBoxSettings.ED_NAVBAR ||
                                edMode == GravityBoxSettings.ED_BOTH)));
            default: return false;
        }
    }

    private static void attachPie() {
        if (isPieEnabled()) {

            // Create our container, if it does not exist already
            if (mPieContainer == null) {
                mPieContainer = new PieLayout(mContext, mGbContext);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT);
                // This title is for debugging only. See: dumpsys window
                lp.setTitle("PieControlPanel");
                lp.windowAnimations = android.R.style.Animation;
                lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;

                mWindowManager.addView(mPieContainer, lp);
                mPieController.attachTo(mPieContainer);
            }

            // add or update pie triggers
            if (DEBUG) log("AttachPie with trigger position flags: " + mPieTriggerSlots);

            refreshPieTriggers();

        } else {
            for (int i = 0; i < mPieTrigger.length; i++) {
                if (mPieTrigger[i] != null) {
                    mWindowManager.removeView(mPieTrigger[i]);
                    mPieTrigger[i] = null;
                }
            }
        }
    }

    private static void refreshPieTriggers() {
        for (Position g : Position.values()) {
            View trigger = mPieTrigger[g.INDEX];
            if (trigger == null && (mPieTriggerSlots & g.FLAG) != 0) {
                trigger = new View(mContext);
                trigger.setClickable(false);
                trigger.setLongClickable(false);
                trigger.setTag(mPieController.buildTracker(g));
                trigger.setOnTouchListener(mPieTriggerOnTouchHandler);

                if (DEBUG) {
                    trigger.setVisibility(View.VISIBLE);
                    trigger.setBackgroundColor(0x77ff0000);
                    log("addPieTrigger on " + g.INDEX
                            + " with position: " + g + " : " + trigger.toString());
                }
                mWindowManager.addView(trigger, getPieTriggerLayoutParams(g));
                mPieTrigger[g.INDEX] = trigger;
            } else if (trigger != null && (mPieTriggerSlots & g.FLAG) == 0) {
                mWindowManager.removeView(trigger);
                mPieTrigger[g.INDEX] = null;
            } else if (trigger != null) {
                mWindowManager.updateViewLayout(trigger, getPieTriggerLayoutParams(g));
            }
        }
    }

    private static WindowManager.LayoutParams getPieTriggerLayoutParams(Position position) {
        final Resources res = mContext.getResources();

        int width = (int) (res.getDisplayMetrics().widthPixels * 0.8f);
        int height = (int) (res.getDisplayMetrics().heightPixels * 0.8f);
        int triggerThickness = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, Settings.System.getInt(
                        mContext.getContentResolver(), SETTING_PIE_TRIGGER_SIZE, 5), 
                        res.getDisplayMetrics());
        if (DEBUG) log("Pie trigger thickness: " + triggerThickness);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                (position == Position.TOP || position == Position.BOTTOM
                        ? width : triggerThickness),
                (position == Position.LEFT || position == Position.RIGHT
                        ? height : triggerThickness),
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        /* | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM */,
                PixelFormat.TRANSLUCENT);
        // This title is for debugging only. See: dumpsys window
        lp.setTitle("PieTrigger" + position.name());
        if (position == Position.LEFT || position == Position.RIGHT) {
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        } else {
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        }
        lp.gravity = position.ANDROID_GRAVITY;
        return lp;
    }
}