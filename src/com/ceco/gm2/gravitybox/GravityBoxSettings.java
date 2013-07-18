package com.ceco.gm2.gravitybox;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class GravityBoxSettings extends Activity {
    public static final String PREF_KEY_QUICK_SETTINGS = "pref_quick_settings";

    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_PERCENT = 3;
    public static final int BATTERY_STYLE_PERCENT_STOCK = 4;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    public static final int BATTERY_WARNING_POPUP = 1;
    public static final int BATTERY_WARNING_SOUND = 2;

    public static final String PREF_KEY_SIGNAL_ICON_AUTOHIDE = "pref_signal_icon_autohide";
    public static final String PREF_KEY_POWEROFF_ADVANCED = "pref_poweroff_advanced";

    public static final String PREF_KEY_VOL_KEY_CURSOR_CONTROL = "pref_vol_key_cursor_control";
    public static final int VOL_KEY_CURSOR_CONTROL_OFF = 0;
    public static final int VOL_KEY_CURSOR_CONTROL_ON = 1;
    public static final int VOL_KEY_CURSOR_CONTROL_ON_REVERSE = 2;

    public static final String PREF_KEY_RECENTS_CLEAR_ALL = "pref_recents_clear_all";
    public static final String PREF_KEY_CALLER_FULLSCREEN_PHOTO = "pref_caller_fullscreen_photo";
    public static final String PREF_KEY_FIX_DATETIME_CRASH = "pref_fix_datetime_crash";
    public static final String PREF_KEY_FIX_CALLER_ID_PHONE = "pref_fix_caller_id_phone";
    public static final String PREF_KEY_FIX_CALLER_ID_MMS = "pref_fix_caller_id_mms";
    public static final String PREF_KEY_FIX_MMS_WAKELOCK = "pref_mms_fix_wakelock";
    public static final String PREF_KEY_FIX_CALENDAR = "pref_fix_calendar";
    public static final String PREF_KEY_STATUSBAR_BGCOLOR = "pref_statusbar_bgcolor";
    public static final String PREF_KEY_STATUSBAR_CENTER_CLOCK = "pref_statusbar_center_clock";
    public static final String PREF_KEY_FIX_TTS_SETTINGS = "pref_fix_tts_settings";
    public static final String PREF_KEY_FIX_DEV_OPTS = "pref_fix_dev_opts";
    public static final String PREF_KEY_ABOUT_GRAVITYBOX = "pref_about_gb";
    public static final String PREF_KEY_ABOUT_XPOSED = "pref_about_xposed";
    public static final String PREF_KEY_ABOUT_DONATE = "pref_about_donate";
    public static final String PREF_KEY_CRT_OFF_EFFECT = "pref_crt_off_effect";
    public static final String PREF_KEY_ENGINEERING_MODE = "pref_engineering_mode";
    public static final String APP_ENGINEERING_MODE = "com.mediatek.engineermode";
    public static final String APP_ENGINEERING_MODE_CLASS = "com.mediatek.engineermode.EngineerMode";
    public static final String PREF_KEY_DUAL_SIM_RINGER = "pref_dual_sim_ringer";
    public static final String APP_DUAL_SIM_RINGER = "dualsim.ringer";
    public static final String APP_DUAL_SIM_RINGER_CLASS = "dualsim.ringer.main";

    public static final String PREF_CAT_KEY_LOCKSCREEN_BACCKGROUND = "pref_cat_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND = "pref_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR = "pref_lockscreen_bg_color";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE = "pref_lockscreen_bg_image";
    public static final String LOCKSCREEN_BG_DEFAULT = "default";
    public static final String LOCKSCREEN_BG_COLOR = "color";
    public static final String LOCKSCREEN_BG_IMAGE = "image";
    private static final int LOCKSCREEN_BACKGROUND = 1024;

    public static final String PREF_KEY_LOCKSCREEN_MAXIMIZE_WIDGETS = "pref_lockscreen_maximize_widgets";
    public static final String PREF_KEY_LOCKSCREEN_ROTATION = "pref_lockscreen_rotation";
    public static final String PREF_KEY_LOCKSCREEN_MENU_KEY = "pref_lockscreen_menu_key";
    public static final String PREF_KEY_FLASHING_LED_DISABLE = "pref_flashing_led_disable";
    public static final String PREF_KEY_CHARGING_LED_DISABLE = "pref_charging_led_disable";

    public static final String PREF_KEY_BRIGHTNESS_MIN = "pref_brightness_min";
    public static final String PREF_KEY_AUTOBRIGHTNESS = "pref_autobrightness";

    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";
    public static final String PREF_KEY_MUSIC_VOLUME_STEPS = "pref_music_volume_steps";
    public static final String PREF_KEY_SAFE_MEDIA_VOLUME = "pref_safe_media_volume";

    public static final String PREF_KEY_HWKEY_MENU_LONGPRESS = "pref_hwkey_menu_longpress";
    public static final String PREF_KEY_HWKEY_MENU_DOUBLETAP = "pref_hwkey_menu_doubletap";
    public static final String PREF_KEY_HWKEY_BACK_LONGPRESS = "pref_hwkey_back_longpress";
    public static final String PREF_KEY_HWKEY_DOUBLETAP_SPEED = "pref_hwkey_doubletap_speed";
    public static final String PREF_KEY_HWKEY_KILL_DELAY = "pref_hwkey_kill_delay";
    public static final int HWKEY_ACTION_DEFAULT = 0;
    public static final int HWKEY_ACTION_SEARCH = 1;
    public static final int HWKEY_ACTION_VOICE_SEARCH = 2;
    public static final int HWKEY_ACTION_PREV_APP = 3;
    public static final int HWKEY_ACTION_KILL = 4;
    public static final int HWKEY_DOUBLETAP_SPEED_DEFAULT = 400;
    public static final int HWKEY_KILL_DELAY_DEFAULT = 1000;
    public static final String ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_MENU_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_MENU_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_BACK_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED = "gravitybox.intent.action.HWKEY_DOUBLETAP_SPEED_CHANGED";
    public static final String ACTION_PREF_HWKEY_KILL_DELAY_CHANGED = "gravitybox.intent.action.HWKEY_KILL_DELAY_CHANGED";
    public static final String EXTRA_HWKEY_VALUE = "hwKeyValue";

    public static final String PREF_KEY_PHONE_FLIP = "pref_phone_flip";
    public static final int PHONE_FLIP_ACTION_NONE = 0;
    public static final int PHONE_FLIP_ACTION_MUTE = 1;
    public static final int PHONE_FLIP_ACTION_DISMISS = 2;
    public static final String PREF_KEY_PHONE_CALL_CONNECT_VIBRATE_DISABLE = "pref_phone_call_connect_vibrate_disable";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";
    public static final String ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED = "gravitybox.intent.action.SIGNAL_ICON_AUTOHIDE_CHANGED";

    public static final String ACTION_PREF_STATUSBAR_BGCOLOR_CHANGED = "gravitybox.intent.action.STATUSBAR_BGCOLOR_CHANGED";
    public static final String EXTRA_SB_BGCOLOR = "bgColor";

    public static final String ACTION_PREF_QUICKSETTINGS_CHANGED = "gravitybox.intent.action.QUICKSETTINGS_CHANGED";
    public static final String EXTRA_QS_PREFS = "qsPrefs";

    public static final String ACTION_PREF_CENTER_CLOCK_CHANGED = "gravitybox.intent.action.CENTER_CLOCK_CHANGED";
    public static final String EXTRA_CENTER_CLOCK = "centerClock";

    public static final String ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED = "gravitybox.intent.action.SAFE_MEDIA_VOLUME_CHANGED";
    public static final String EXTRA_SAFE_MEDIA_VOLUME_ENABLED = "enabled";

    private static final List<String> rebootKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_FIX_DATETIME_CRASH,
            PREF_KEY_FIX_CALENDAR,
            PREF_KEY_FIX_CALLER_ID_PHONE,
            PREF_KEY_FIX_CALLER_ID_MMS,
            PREF_KEY_FIX_TTS_SETTINGS,
            PREF_KEY_FIX_DEV_OPTS,
            PREF_KEY_BRIGHTNESS_MIN,
            PREF_KEY_LOCKSCREEN_MENU_KEY,
            PREF_KEY_FIX_MMS_WAKELOCK,
            PREF_KEY_MUSIC_VOLUME_STEPS
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private ListPreference mBatteryStyle;
        private ListPreference mLowBatteryWarning;
        private MultiSelectListPreference mSignalIconAutohide;
        private SharedPreferences mPrefs;
        private AlertDialog mDialog;
        private MultiSelectListPreference mQuickSettings;
        private ColorPickerPreference mStatusbarBgColor;
        private Preference mPrefAboutGb;
        private Preference mPrefAboutXposed;
        private Preference mPrefAboutDonate;
        private Preference mPrefEngMode;
        private Preference mPrefDualSimRinger;
        private PreferenceCategory mPrefCatLockscreenBg;
        private ListPreference mPrefLockscreenBg;
        private ColorPickerPreference mPrefLockscreenBgColor;
        private Preference mPrefLockscreenBgImage;
        private File wallpaperImage;
        private File wallpaperTemporary;
        private EditTextPreference mPrefBrightnessMin;
        private ListPreference mPrefHwKeyMenuLongpress;
        private ListPreference mPrefHwKeyMenuDoubletap;
        private ListPreference mPrefHwKeyBackLongpress;
        private ListPreference mPrefHwKeyDoubletapSpeed;
        private ListPreference mPrefHwKeyKillDelay;
        private ListPreference mPrefPhoneFlip;

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // this is important because although the handler classes that read these settings
            // are in the same package, they are executed in the context of the hooked package
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.gravitybox);

            mPrefs = getPreferenceScreen().getSharedPreferences();

            mBatteryStyle = (ListPreference) findPreference(PREF_KEY_BATTERY_STYLE);
            mLowBatteryWarning = (ListPreference) findPreference(PREF_KEY_LOW_BATTERY_WARNING_POLICY);
            mSignalIconAutohide = (MultiSelectListPreference) findPreference(PREF_KEY_SIGNAL_ICON_AUTOHIDE);
            mQuickSettings = (MultiSelectListPreference) findPreference(PREF_KEY_QUICK_SETTINGS);
            mStatusbarBgColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_BGCOLOR);
            mStatusbarBgColor.setAlphaSliderEnabled(true);

            mPrefAboutGb = (Preference) findPreference(PREF_KEY_ABOUT_GRAVITYBOX);
            
            String version = "";
            try {
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version = " v" + pInfo.versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } finally {
                mPrefAboutGb.setTitle(getActivity().getTitle() + version);
            }

            mPrefAboutXposed = (Preference) findPreference(PREF_KEY_ABOUT_XPOSED);
            mPrefAboutDonate = (Preference) findPreference(PREF_KEY_ABOUT_DONATE);

            mPrefEngMode = (Preference) findPreference(PREF_KEY_ENGINEERING_MODE);
            if (!isAppInstalled(APP_ENGINEERING_MODE)) {
                getPreferenceScreen().removePreference(mPrefEngMode);
            }

            mPrefDualSimRinger = (Preference) findPreference(PREF_KEY_DUAL_SIM_RINGER);
            if (!isAppInstalled(APP_DUAL_SIM_RINGER)) {
                getPreferenceScreen().removePreference(mPrefDualSimRinger);
            }

            mPrefCatLockscreenBg = 
                    (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_BACCKGROUND);
            mPrefLockscreenBg = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBgColor = 
                    (ColorPickerPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR);
            mPrefLockscreenBgImage = 
                    (Preference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE);

            wallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper"); 
            wallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");

            mPrefBrightnessMin = (EditTextPreference) findPreference(PREF_KEY_BRIGHTNESS_MIN);

            mPrefHwKeyMenuLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_LONGPRESS);
            mPrefHwKeyMenuDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_DOUBLETAP);
            mPrefHwKeyBackLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_LONGPRESS);
            mPrefHwKeyDoubletapSpeed = (ListPreference) findPreference(PREF_KEY_HWKEY_DOUBLETAP_SPEED);
            mPrefHwKeyKillDelay = (ListPreference) findPreference(PREF_KEY_HWKEY_KILL_DELAY);

            mPrefPhoneFlip = (ListPreference) findPreference(PREF_KEY_PHONE_FLIP);
        }

        @Override
        public void onResume() {
            super.onResume();

            updatePreferences();
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);

            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }

            super.onPause();
        }

        private void updatePreferences() {
            mBatteryStyle.setSummary(mBatteryStyle.getEntry());
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());

            Set<String> autoHide =  mSignalIconAutohide.getValues();
            String summary = "";
            if (autoHide.contains("notifications_disabled")) {
                summary += getString(R.string.sim_disable_notifications_summary);
            }
            if (autoHide.contains("sim1")) {
                if (!summary.isEmpty()) summary += ", ";
                summary += getString(R.string.sim_slot_1);
            }
            if (autoHide.contains("sim2")) {
                if (!summary.isEmpty()) summary += ", ";
                summary += getString(R.string.sim_slot_2);
            }
            if (summary.isEmpty()) {
                summary = getString(R.string.signal_icon_autohide_summary);
            }
            mSignalIconAutohide.setSummary(summary);

            mPrefLockscreenBg.setSummary(mPrefLockscreenBg.getEntry());

            mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgColor);
            mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgImage);
            String option = mPrefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT);
            if (option.equals(LOCKSCREEN_BG_COLOR)) {
                mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgColor);
            } else if (option.equals(LOCKSCREEN_BG_IMAGE)) {
                mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgImage);
            }

            mPrefHwKeyMenuLongpress.setSummary(mPrefHwKeyMenuLongpress.getEntry());
            mPrefHwKeyMenuDoubletap.setSummary(mPrefHwKeyMenuDoubletap.getEntry());
            mPrefHwKeyBackLongpress.setSummary(mPrefHwKeyBackLongpress.getEntry());
            mPrefHwKeyDoubletapSpeed.setSummary(getString(R.string.pref_hwkey_doubletap_speed_summary)
                    + " (" + mPrefHwKeyDoubletapSpeed.getEntry() + ")");
            mPrefHwKeyKillDelay.setSummary(getString(R.string.pref_hwkey_kill_delay_summary)
                    + " (" + mPrefHwKeyKillDelay.getEntry() + ")");

            mPrefPhoneFlip.setSummary(getString(R.string.pref_phone_flip_summary)
                    + " (" + mPrefPhoneFlip.getEntry() + ")");
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updatePreferences();

            Intent intent = new Intent();
            if (key.equals(PREF_KEY_BATTERY_STYLE)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                int batteryStyle = Integer.valueOf(prefs.getString(PREF_KEY_BATTERY_STYLE, "1"));
                intent.putExtra("batteryStyle", batteryStyle);                
            } else if (key.equals(PREF_KEY_SIGNAL_ICON_AUTOHIDE)) {
                intent.setAction(ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED);
                String[] autohidePrefs = mSignalIconAutohide.getValues().toArray(new String[0]);
                intent.putExtra("autohidePrefs", autohidePrefs);
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                String[] qsPrefs = mQuickSettings.getValues().toArray(new String[0]);
                intent.putExtra(EXTRA_QS_PREFS, qsPrefs);
            } else if (key.equals(PREF_KEY_STATUSBAR_BGCOLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_BGCOLOR_CHANGED);
                intent.putExtra(EXTRA_SB_BGCOLOR, prefs.getInt(PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_STATUSBAR_CENTER_CLOCK)) {
                intent.setAction(ACTION_PREF_CENTER_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CENTER_CLOCK, 
                        prefs.getBoolean(PREF_KEY_STATUSBAR_CENTER_CLOCK, false));
            } else if (key.equals(PREF_KEY_SAFE_MEDIA_VOLUME)) {
                intent.setAction(ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED);
                intent.putExtra(EXTRA_SAFE_MEDIA_VOLUME_ENABLED,
                        prefs.getBoolean(PREF_KEY_SAFE_MEDIA_VOLUME, false));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_DOUBLETAP_SPEED)) {
                intent.setAction(ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_DOUBLETAP_SPEED, "400")));
            } else if (key.equals(PREF_KEY_HWKEY_KILL_DELAY)) {
                intent.setAction(ACTION_PREF_HWKEY_KILL_DELAY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_KILL_DELAY, "1000")));
            }
            if (intent.getAction() != null) {
                getActivity().sendBroadcast(intent);
            }

            if (key.equals(PREF_KEY_FIX_CALLER_ID_PHONE) ||
                    key.equals(PREF_KEY_FIX_CALLER_ID_MMS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.important);
                int msgId = (key.equals(PREF_KEY_FIX_CALLER_ID_PHONE)) ?
                        R.string.fix_caller_id_phone_alert : R.string.fix_caller_id_mms_alert;
                builder.setMessage(msgId);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(getActivity(), getString(R.string.reboot_required), Toast.LENGTH_SHORT).show();
                    }
                });
                mDialog = builder.create();
                mDialog.show();
            }

            if (key.equals(PREF_KEY_BRIGHTNESS_MIN)) {
                String strVal = prefs.getString(PREF_KEY_BRIGHTNESS_MIN, "20");
                try {
                    int val = Integer.valueOf(strVal);
                    int newVal = val;
                    if (val < 20) newVal = 20;
                    if (val > 80) newVal = 80;
                    if (val != newVal) {
                        Editor editor = prefs.edit();
                        editor.putString(PREF_KEY_BRIGHTNESS_MIN, String.valueOf(newVal));
                        editor.commit();
                        mPrefBrightnessMin.setText(String.valueOf(newVal));
                    }
                } catch (NumberFormatException e) {
                    Editor editor = prefs.edit();
                    editor.putString(PREF_KEY_BRIGHTNESS_MIN, "20");
                    editor.commit();
                    mPrefBrightnessMin.setText("20");
                }
            }

            if (rebootKeys.contains(key))
                Toast.makeText(getActivity(), getString(R.string.reboot_required), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref) {
            Intent intent = null;

            if (pref == mPrefAboutGb) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_gravitybox)));
            } else if (pref == mPrefAboutXposed) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_xposed)));
            } else if (pref == mPrefAboutDonate) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate)));
            } else if (pref == mPrefEngMode) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_ENGINEERING_MODE, APP_ENGINEERING_MODE_CLASS);
            } else if (pref == mPrefDualSimRinger) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_DUAL_SIM_RINGER, APP_DUAL_SIM_RINGER_CLASS);
            } else if (pref == mPrefLockscreenBgImage) {
                setCustomLockscreenImage();
                return true;
            }
            
            if (intent != null) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
            }

            return super.onPreferenceTreeClick(prefScreen, pref);
        }

        private boolean isAppInstalled(String appUri) {
            PackageManager pm = getActivity().getPackageManager();
            try {
                pm.getPackageInfo(appUri, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomLockscreenImage() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            // Lock screen for tablets visible section are different in landscape/portrait,
            // image need to be cropped correctly, like wallpaper setup for scrolling in background in home screen
            // other wise it does not scale correctly
            if (Utils.isTablet(getActivity())) {
                width = getActivity().getWallpaperDesiredMinimumWidth();
                height = getActivity().getWallpaperDesiredMinimumHeight();
                float spotlightX = (float) display.getWidth() / width;
                float spotlightY = (float) display.getHeight() / height;
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("spotlightX", spotlightX);
                intent.putExtra("spotlightY", spotlightY);
            } else {
                boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;
                intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
                intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            }
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(wallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, LOCKSCREEN_BACKGROUND);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == LOCKSCREEN_BACKGROUND) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(wallpaperImage);
                    }
                    wallpaperImage.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}