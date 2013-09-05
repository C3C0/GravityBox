package com.ceco.gm2.gravitybox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ceco.gm2.gravitybox.preference.AutoBrightnessDialogPreference;
import com.ceco.gm2.gravitybox.preference.SeekBarPreference;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
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
    public static final String PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW = "pref_qs_tiles_per_row";
    public static final String PREF_KEY_QUICK_SETTINGS_AUTOSWITCH = "pref_auto_switch_qs";
    public static final String PREF_KEY_QUICK_PULLDOWN = "pref_quick_pulldown";
    public static final int QUICK_PULLDOWN_OFF = 0;
    public static final int QUICK_PULLDOWN_RIGHT = 1;
    public static final int QUICK_PULLDOWN_LEFT = 2;

    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT = "pref_battery_percent_text";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_CIRCLE_PERCENT = 3;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    public static final int BATTERY_WARNING_POPUP = 1;
    public static final int BATTERY_WARNING_SOUND = 2;

    public static final String PREF_KEY_SIGNAL_ICON_AUTOHIDE = "pref_signal_icon_autohide";
    public static final String PREF_KEY_DISABLE_ROAMING_INDICATORS = "pref_disable_roaming_indicators";
    public static final String ACTION_DISABLE_ROAMING_INDICATORS_CHANGED = "gravitybox.intent.action.DISABLE_ROAMING_INDICATORS_CHANGED";
    public static final String EXTRA_INDICATORS_DISABLED = "indicatorsDisabled";
    public static final String PREF_KEY_POWEROFF_ADVANCED = "pref_poweroff_advanced";

    public static final String PREF_KEY_VOL_KEY_CURSOR_CONTROL = "pref_vol_key_cursor_control";
    public static final int VOL_KEY_CURSOR_CONTROL_OFF = 0;
    public static final int VOL_KEY_CURSOR_CONTROL_ON = 1;
    public static final int VOL_KEY_CURSOR_CONTROL_ON_REVERSE = 2;

    public static final String PREF_KEY_RECENTS_CLEAR_ALL = "pref_recents_clear_all2";
    public static final String PREF_KEY_RECENTS_CLEAR_MARGIN_TOP = "pref_recent_clear_margin_top";
    public static final int RECENT_CLEAR_OFF = 0;
    public static final int RECENT_CLEAR_TOP_LEFT = 51;
    public static final int RECENT_CLEAR_TOP_RIGHT = 53;
    public static final int RECENT_CLEAR_BOTTOM_LEFT = 83;
    public static final int RECENT_CLEAR_BOTTOM_RIGHT = 85;

    public static final String PREF_CAT_KEY_PHONE = "pref_cat_phone";
    public static final String PREF_KEY_CALLER_FULLSCREEN_PHOTO = "pref_caller_fullscreen_photo";
    public static final String PREF_KEY_ROAMING_WARNING_DISABLE = "pref_roaming_warning_disable";
    public static final String PREF_CAT_KEY_FIXES = "pref_cat_fixes";
    public static final String PREF_KEY_FIX_DATETIME_CRASH = "pref_fix_datetime_crash";
    public static final String PREF_KEY_FIX_CALLER_ID_PHONE = "pref_fix_caller_id_phone";
    public static final String PREF_KEY_FIX_CALLER_ID_MMS = "pref_fix_caller_id_mms";
    public static final String PREF_KEY_FIX_MMS_WAKELOCK = "pref_mms_fix_wakelock";
    public static final String PREF_KEY_FIX_CALENDAR = "pref_fix_calendar";
    public static final String PREF_CAT_KEY_STATUSBAR = "pref_cat_statusbar";
    public static final String PREF_CAT_KEY_STATUSBAR_QS = "pref_cat_statusbar_qs";
    public static final String PREF_KEY_STATUSBAR_BGCOLOR = "pref_statusbar_bgcolor2";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE = "pref_statusbar_icon_color_enable";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR = "pref_statusbar_icon_color";
    public static final String PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR = "pref_statusbar_data_activity_color";
    public static final String PREF_KEY_STATUSBAR_CENTER_CLOCK = "pref_statusbar_center_clock";
    public static final String PREF_KEY_STATUSBAR_CLOCK_DOW = "pref_statusbar_clock_dow";
    public static final String PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE = "pref_clock_ampm_hide";
    public static final String PREF_KEY_STATUSBAR_CLOCK_HIDE = "pref_clock_hide";
    public static final String PREF_KEY_ALARM_ICON_HIDE = "pref_alarm_icon_hide";
    public static final String PREF_KEY_TM_STATUSBAR_LAUNCHER = "pref_tm_statusbar_launcher";
    public static final String PREF_KEY_TM_STATUSBAR_LOCKSCREEN = "pref_tm_statusbar_lockscreen";
    public static final String PREF_KEY_TM_NAVBAR_LAUNCHER = "pref_tm_navbar_launcher";
    public static final String PREF_KEY_TM_NAVBAR_LOCKSCREEN = "pref_tm_navbar_lockscreen";
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

    public static final String PREF_CAT_KEY_LOCKSCREEN = "pref_cat_lockscreen";
    public static final String PREF_CAT_KEY_LOCKSCREEN_BACKGROUND = "pref_cat_lockscreen_background";
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

    public static final String PREF_CAT_KEY_DISPLAY = "pref_cat_display";
    public static final String PREF_CAT_KEY_BRIGHTNESS = "pref_cat_brightness";
    public static final String PREF_KEY_BRIGHTNESS_MASTER_SWITCH = "pref_brightness_master_switch";
    public static final String PREF_KEY_BRIGHTNESS_MIN = "pref_brightness_min2";
    public static final String PREF_KEY_SCREEN_DIM_LEVEL = "pref_screen_dim_level";
    public static final String PREF_KEY_AUTOBRIGHTNESS = "pref_autobrightness";
    public static final String PREF_KEY_HOLO_BG_SOLID_BLACK = "pref_holo_bg_solid_black";
    public static final String PREF_KEY_HOLO_BG_DITHER = "pref_holo_bg_dither";

    public static final String PREF_CAT_KEY_MEDIA = "pref_cat_media";
    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";
    public static final String PREF_KEY_MUSIC_VOLUME_STEPS = "pref_music_volume_steps";
    public static final String PREF_KEY_SAFE_MEDIA_VOLUME = "pref_safe_media_volume";
    public static final String PREF_KEY_VOLUME_PANEL_EXPANDABLE = "pref_volume_panel_expandable";
    public static final String ACTION_PREF_VOLUME_PANEL_MODE_CHANGED = "gravitybox.intent.action.VOLUME_PANEL_MODE_CHANGED";
    public static final String EXTRA_EXPANDABLE = "expandable";
    public static final String PREF_KEY_LINK_VOLUMES = "pref_link_volumes";
    public static final String ACTION_PREF_LINK_VOLUMES_CHANGED = "gravitybox.intent.action.LINK_VOLUMES_CHANGED";
    public static final String EXTRA_LINKED = "linked";

    public static final String PREF_KEY_HWKEY_MENU_LONGPRESS = "pref_hwkey_menu_longpress";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS = "pref_hwkey_home_longpress";
    public static final String PREF_KEY_HWKEY_MENU_DOUBLETAP = "pref_hwkey_menu_doubletap";
    public static final String PREF_KEY_HWKEY_BACK_LONGPRESS = "pref_hwkey_back_longpress";
    public static final String PREF_KEY_HWKEY_CUSTOM_APP = "pref_hwkey_custom_app";
    public static final String PREF_KEY_HWKEY_DOUBLETAP_SPEED = "pref_hwkey_doubletap_speed";
    public static final String PREF_KEY_HWKEY_KILL_DELAY = "pref_hwkey_kill_delay";
    public static final String PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE = "pref_volume_rocker_wake_disable";
    public static final int HWKEY_ACTION_DEFAULT = 0;
    public static final int HWKEY_ACTION_SEARCH = 1;
    public static final int HWKEY_ACTION_VOICE_SEARCH = 2;
    public static final int HWKEY_ACTION_PREV_APP = 3;
    public static final int HWKEY_ACTION_KILL = 4;
    public static final int HWKEY_ACTION_SLEEP = 5;
    public static final int HWKEY_ACTION_RECENT_APPS = 6;
    public static final int HWKEY_ACTION_CUSTOM_APP = 7;
    public static final int HWKEY_DOUBLETAP_SPEED_DEFAULT = 400;
    public static final int HWKEY_KILL_DELAY_DEFAULT = 1000;
    public static final String ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_MENU_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_MENU_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_HOME_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_BACK_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED = "gravitybox.intent.action.HWKEY_DOUBLETAP_SPEED_CHANGED";
    public static final String ACTION_PREF_HWKEY_KILL_DELAY_CHANGED = "gravitybox.intent.action.HWKEY_KILL_DELAY_CHANGED";
    public static final String ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED = "gravitybox.intent.action.VOLUME_ROCKER_WAKE_CHANGED";
    public static final String EXTRA_HWKEY_VALUE = "hwKeyValue";
    public static final String EXTRA_VOLUME_ROCKER_WAKE_DISABLE = "volumeRockerWakeDisable";

    public static final String PREF_KEY_PHONE_FLIP = "pref_phone_flip";
    public static final int PHONE_FLIP_ACTION_NONE = 0;
    public static final int PHONE_FLIP_ACTION_MUTE = 1;
    public static final int PHONE_FLIP_ACTION_DISMISS = 2;
    public static final String PREF_KEY_PHONE_CALL_CONNECT_VIBRATE_DISABLE = "pref_phone_call_connect_vibrate_disable";

    public static final String PREF_CAT_KEY_NOTIF_DRAWER_STYLE = "pref_cat_notification_drawer_style";
    public static final String PREF_KEY_NOTIF_BACKGROUND = "pref_notif_background";
    public static final String PREF_KEY_NOTIF_COLOR = "pref_notif_color";
    public static final String PREF_KEY_NOTIF_COLOR_MODE = "pref_notif_color_mode";
    public static final String PREF_KEY_NOTIF_IMAGE_PORTRAIT = "pref_notif_image_portrait";
    public static final String PREF_KEY_NOTIF_IMAGE_LANDSCAPE = "pref_notif_image_landscape";
    public static final String PREF_KEY_NOTIF_BACKGROUND_ALPHA = "pref_notif_background_alpha";
    public static final String NOTIF_BG_DEFAULT = "default";
    public static final String NOTIF_BG_COLOR = "color";
    public static final String NOTIF_BG_IMAGE = "image";
    public static final String NOTIF_BG_COLOR_MODE_OVERLAY = "overlay";
    public static final String NOTIF_BG_COLOR_MODE_UNDERLAY = "underlay";
    private static final int NOTIF_BG_IMAGE_PORTRAIT = 1025;
    private static final int NOTIF_BG_IMAGE_LANDSCAPE = 1026;
    public static final String ACTION_NOTIF_BACKGROUND_CHANGED = "gravitybox.intent.action.NOTIF_BACKGROUND_CHANGED";
    public static final String EXTRA_BG_TYPE = "bgType";
    public static final String EXTRA_BG_COLOR = "bgColor";
    public static final String EXTRA_BG_COLOR_MODE = "bgColorMode";
    public static final String EXTRA_BG_ALPHA = "bgAlpha";

    public static final String PREF_KEY_PIE_CONTROL_ENABLE = "pref_pie_control_enable";
    public static final String PREF_KEY_PIE_CONTROL_SEARCH = "pref_pie_control_search";
    public static final String PREF_KEY_PIE_CONTROL_MENU = "pref_pie_control_menu";
    public static final String PREF_KEY_PIE_CONTROL_TRIGGERS = "pref_pie_control_trigger_positions";
    public static final String PREF_KEY_PIE_CONTROL_TRIGGER_SIZE = "pref_pie_control_trigger_size";
    public static final String PREF_KEY_PIE_CONTROL_SIZE = "pref_pie_control_size";
    public static final String PREF_KEY_NAVBAR_DISABLE = "pref_navbar_disable";
    public static final String PREF_KEY_HWKEYS_DISABLE = "pref_hwkeys_disable";
    public static final String ACTION_PREF_PIE_CHANGED = "gravitybox.intent.action.PREF_PIE_CHANGED";
    public static final String EXTRA_PIE_ENABLE = "pieEnable";
    public static final String EXTRA_PIE_SEARCH = "pieSearch";
    public static final String EXTRA_PIE_MENU = "pieMenu";
    public static final String EXTRA_PIE_TRIGGERS = "pieTriggers";
    public static final String EXTRA_PIE_TRIGGER_SIZE = "pieTriggerSize";
    public static final String EXTRA_PIE_SIZE = "pieSize";
    public static final String EXTRA_PIE_HWKEYS_DISABLE = "hwKeysDisable";

    public static final String PREF_KEY_BUTTON_BACKLIGHT_MODE = "pref_button_backlight_mode";
    public static final String PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS = "pref_button_backlight_notifications";
    public static final String ACTION_PREF_BUTTON_BACKLIGHT_CHANGED = "gravitybox.intent.action.BUTTON_BACKLIGHT_CHANGED";
    public static final String EXTRA_BB_MODE = "bbMode";
    public static final String EXTRA_BB_NOTIF = "bbNotif";
    public static final String BB_MODE_DEFAULT = "default";
    public static final String BB_MODE_DISABLE = "disable";
    public static final String BB_MODE_ALWAYS_ON = "always_on";

    public static final String PREF_KEY_QUICKAPP_DEFAULT = "pref_quickapp_default";
    public static final String PREF_KEY_QUICKAPP_SLOT1 = "pref_quickapp_slot1";
    public static final String PREF_KEY_QUICKAPP_SLOT2 = "pref_quickapp_slot2";
    public static final String PREF_KEY_QUICKAPP_SLOT3 = "pref_quickapp_slot3";
    public static final String PREF_KEY_QUICKAPP_SLOT4 = "pref_quickapp_slot4";
    public static final String ACTION_PREF_QUICKAPP_CHANGED = "gravitybox.intent.action.QUICKAPP_CHANGED";
    public static final String EXTRA_QUICKAPP_DEFAULT = "quickAppDefault";
    public static final String EXTRA_QUICKAPP_SLOT1 = "quickAppSlot1";
    public static final String EXTRA_QUICKAPP_SLOT2 = "quickAppSlot2";
    public static final String EXTRA_QUICKAPP_SLOT3 = "quickAppSlot3";
    public static final String EXTRA_QUICKAPP_SLOT4 = "quickAppSlot4";

    public static final String PREF_KEY_GB_THEME_DARK = "pref_gb_theme_dark";
    public static final String FILE_THEME_DARK_FLAG = "theme_dark";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "gravitybox.intent.action.BATTERY_STYLE_CHANGED";
    public static final String ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED = "gravitybox.intent.action.SIGNAL_ICON_AUTOHIDE_CHANGED";

    public static final String ACTION_PREF_STATUSBAR_COLOR_CHANGED = "gravitybox.intent.action.STATUSBAR_COLOR_CHANGED";
    public static final String EXTRA_SB_BG_COLOR = "bgColor";
    public static final String EXTRA_SB_ICON_COLOR_ENABLE = "iconColorEnable";
    public static final String EXTRA_SB_ICON_COLOR = "iconColor";
    public static final String EXTRA_SB_DATA_ACTIVITY_COLOR = "dataActivityColor";
    public static final String EXTRA_TM_SB_LAUNCHER = "tmSbLauncher";
    public static final String EXTRA_TM_SB_LOCKSCREEN = "tmSbLockscreen";
    public static final String EXTRA_TM_NB_LAUNCHER = "tmNbLauncher";
    public static final String EXTRA_TM_NB_LOCKSCREEN = "tmNbLockscreen";

    public static final String ACTION_PREF_QUICKSETTINGS_CHANGED = "gravitybox.intent.action.QUICKSETTINGS_CHANGED";
    public static final String EXTRA_QS_PREFS = "qsPrefs";
    public static final String EXTRA_QS_COLS = "qsCols";
    public static final String EXTRA_QS_AUTOSWITCH = "qsAutoSwitch";
    public static final String EXTRA_QUICK_PULLDOWN = "quickPulldown";

    public static final String ACTION_PREF_CLOCK_CHANGED = "gravitybox.intent.action.CENTER_CLOCK_CHANGED";
    public static final String EXTRA_CENTER_CLOCK = "centerClock";
    public static final String EXTRA_CLOCK_DOW = "clockDow";
    public static final String EXTRA_AMPM_HIDE = "ampmHide";
    public static final String EXTRA_CLOCK_HIDE = "clockHide";
    public static final String EXTRA_ALARM_HIDE = "alarmHide";

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
            PREF_KEY_MUSIC_VOLUME_STEPS,
            PREF_KEY_HOLO_BG_SOLID_BLACK,
            PREF_KEY_NAVBAR_DISABLE,
            PREF_KEY_HOLO_BG_DITHER,
            PREF_KEY_SCREEN_DIM_LEVEL,
            PREF_KEY_BRIGHTNESS_MASTER_SWITCH
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set Holo Dark theme if flag file exists
        File file = new File(getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
        if (file.exists()) {
            this.setTheme(android.R.style.Theme_Holo);
        }

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
        private File notifBgImagePortrait;
        private File notifBgImageLandscape;
        private ListPreference mPrefHwKeyMenuLongpress;
        private ListPreference mPrefHwKeyMenuDoubletap;
        private ListPreference mPrefHwKeyHomeLongpress;
        private ListPreference mPrefHwKeyBackLongpress;
        private ListPreference mPrefHwKeyDoubletapSpeed;
        private ListPreference mPrefHwKeyKillDelay;
        private ListPreference mPrefPhoneFlip;
        private CheckBoxPreference mPrefSbIconColorEnable;
        private ColorPickerPreference mPrefSbIconColor;
        private ColorPickerPreference mPrefSbDaColor;
        private PreferenceScreen mPrefCatFixes;
        private CheckBoxPreference mPrefFixDateTimeCrash;
        private CheckBoxPreference mPrefFixCallerIDPhone;
        private CheckBoxPreference mPrefFixCallerIDMms;
        private CheckBoxPreference mPrefFixMmsWakelock;
        private CheckBoxPreference mPrefFixCalendar;
        private CheckBoxPreference mPrefFixTtsSettings;
        private CheckBoxPreference mPrefFixDevOpts;
        private PreferenceScreen mPrefCatStatusbar;
        private PreferenceScreen mPrefCatStatusbarQs;
        private CheckBoxPreference mPrefAutoSwitchQs;
        private ListPreference mPrefQuickPulldown;
        private PreferenceScreen mPrefCatNotifDrawerStyle;
        private ListPreference mPrefNotifBackground;
        private ColorPickerPreference mPrefNotifColor;
        private Preference mPrefNotifImagePortrait;
        private Preference mPrefNotifImageLandscape;
        private ListPreference mPrefNotifColorMode;
        private CheckBoxPreference mPrefDisableRoamingIndicators;
        private ListPreference mPrefButtonBacklightMode;
        private CheckBoxPreference mPrefPieEnabled;
        private CheckBoxPreference mPrefPieNavBarDisabled;
        private CheckBoxPreference mPrefPieHwKeysDisabled;
        private CheckBoxPreference mPrefGbThemeDark;
        private ListPreference mPrefRecentClear;
        private PreferenceScreen mPrefCatPhone;
        private CheckBoxPreference mPrefRoamingWarningDisable;
        private CheckBoxPreference mPrefBrightnessMasterSwitch;
        private SeekBarPreference mPrefBrightnessMin;
        private SeekBarPreference mPrefScreenDimLevel;
        private AutoBrightnessDialogPreference mPrefAutoBrightness;
        private PreferenceScreen mPrefCatLockscreen;
        private PreferenceScreen mPrefCatDisplay;
        private PreferenceScreen mPrefCatBrightness;
        private CheckBoxPreference mPrefCrtOff;
        private PreferenceScreen mPrefCatMedia;
        private CheckBoxPreference mPrefSafeMediaVolume;

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
                    (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBg = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBgColor = 
                    (ColorPickerPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR);
            mPrefLockscreenBgImage = 
                    (Preference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE);

            wallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper"); 
            wallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");
            notifBgImagePortrait = new File(getActivity().getFilesDir() + "/notifwallpaper");
            notifBgImageLandscape = new File(getActivity().getFilesDir() + "/notifwallpaper_landscape");

            mPrefHwKeyMenuLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_LONGPRESS);
            mPrefHwKeyMenuDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_DOUBLETAP);
            mPrefHwKeyHomeLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS);
            mPrefHwKeyBackLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_LONGPRESS);
            mPrefHwKeyDoubletapSpeed = (ListPreference) findPreference(PREF_KEY_HWKEY_DOUBLETAP_SPEED);
            mPrefHwKeyKillDelay = (ListPreference) findPreference(PREF_KEY_HWKEY_KILL_DELAY);

            mPrefPhoneFlip = (ListPreference) findPreference(PREF_KEY_PHONE_FLIP);

            mPrefSbIconColorEnable = (CheckBoxPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE);
            mPrefSbIconColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR);
            mPrefSbDaColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR);

            mPrefCatFixes = (PreferenceScreen) findPreference(PREF_CAT_KEY_FIXES);
            mPrefFixDateTimeCrash = (CheckBoxPreference) findPreference(PREF_KEY_FIX_DATETIME_CRASH);
            mPrefFixCallerIDPhone = (CheckBoxPreference) findPreference(PREF_KEY_FIX_CALLER_ID_PHONE);
            mPrefFixCallerIDMms = (CheckBoxPreference) findPreference(PREF_KEY_FIX_CALLER_ID_MMS);
            mPrefFixMmsWakelock = (CheckBoxPreference) findPreference(PREF_KEY_FIX_MMS_WAKELOCK);
            mPrefFixCalendar = (CheckBoxPreference) findPreference(PREF_KEY_FIX_CALENDAR);
            mPrefFixTtsSettings = (CheckBoxPreference) findPreference(PREF_KEY_FIX_TTS_SETTINGS);
            mPrefFixDevOpts = (CheckBoxPreference) findPreference(PREF_KEY_FIX_DEV_OPTS);
            mPrefCatStatusbar = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR);
            mPrefCatStatusbarQs = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR_QS);
            mPrefAutoSwitchQs = (CheckBoxPreference) findPreference(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH);
            mPrefQuickPulldown = (ListPreference) findPreference(PREF_KEY_QUICK_PULLDOWN);

            mPrefCatNotifDrawerStyle = (PreferenceScreen) findPreference(PREF_CAT_KEY_NOTIF_DRAWER_STYLE);
            mPrefNotifBackground = (ListPreference) findPreference(PREF_KEY_NOTIF_BACKGROUND);
            mPrefNotifColor = (ColorPickerPreference) findPreference(PREF_KEY_NOTIF_COLOR);
            mPrefNotifImagePortrait = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_PORTRAIT);
            mPrefNotifImageLandscape = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_LANDSCAPE);
            mPrefNotifColorMode = (ListPreference) findPreference(PREF_KEY_NOTIF_COLOR_MODE);

            mPrefDisableRoamingIndicators = (CheckBoxPreference) findPreference(PREF_KEY_DISABLE_ROAMING_INDICATORS);
            mPrefButtonBacklightMode = (ListPreference) findPreference(PREF_KEY_BUTTON_BACKLIGHT_MODE);

            mPrefPieEnabled = (CheckBoxPreference) findPreference(PREF_KEY_PIE_CONTROL_ENABLE);
            mPrefPieNavBarDisabled = (CheckBoxPreference) findPreference(PREF_KEY_NAVBAR_DISABLE);
            mPrefPieHwKeysDisabled = (CheckBoxPreference) findPreference(PREF_KEY_HWKEYS_DISABLE);

            mPrefGbThemeDark = (CheckBoxPreference) findPreference(PREF_KEY_GB_THEME_DARK);
            File file = new File(getActivity().getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
            mPrefGbThemeDark.setChecked(file.exists());

            mPrefRecentClear = (ListPreference) findPreference(PREF_KEY_RECENTS_CLEAR_ALL);

            mPrefCatPhone = (PreferenceScreen) findPreference(PREF_CAT_KEY_PHONE);
            mPrefRoamingWarningDisable = (CheckBoxPreference) findPreference(PREF_KEY_ROAMING_WARNING_DISABLE);

            mPrefBrightnessMasterSwitch = (CheckBoxPreference) findPreference(PREF_KEY_BRIGHTNESS_MASTER_SWITCH);
            mPrefBrightnessMin = (SeekBarPreference) findPreference(PREF_KEY_BRIGHTNESS_MIN);
            mPrefScreenDimLevel = (SeekBarPreference) findPreference(PREF_KEY_SCREEN_DIM_LEVEL);
            mPrefAutoBrightness = (AutoBrightnessDialogPreference) findPreference(PREF_KEY_AUTOBRIGHTNESS);

            mPrefCatLockscreen = (PreferenceScreen) findPreference(PREF_CAT_KEY_LOCKSCREEN);
            mPrefCatDisplay = (PreferenceScreen) findPreference(PREF_CAT_KEY_DISPLAY);
            mPrefCatBrightness = (PreferenceScreen) findPreference(PREF_CAT_KEY_BRIGHTNESS);
            mPrefCrtOff = (CheckBoxPreference) findPreference(PREF_KEY_CRT_OFF_EFFECT);
            mPrefCatMedia = (PreferenceScreen) findPreference(PREF_CAT_KEY_MEDIA);
            mPrefSafeMediaVolume = (CheckBoxPreference) findPreference(PREF_KEY_SAFE_MEDIA_VOLUME);

            // Remove Phone specific preferences on Tablet devices
            if (Utils.isTablet(getActivity())) {
            	mPrefCatStatusbarQs.removePreference(mPrefAutoSwitchQs);
            	mPrefCatStatusbarQs.removePreference(mPrefQuickPulldown);
            }

            // Remove MTK specific preferences for non-MTK devices
            if (!Utils.isMtkDevice()) {
                getPreferenceScreen().removePreference(mPrefCatFixes);
                mPrefCatStatusbar.removePreference(mSignalIconAutohide);
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                mQuickSettings.setEntries(R.array.qs_tile_aosp_entries);
                mQuickSettings.setEntryValues(R.array.qs_tile_aosp_values);
                mPrefCatPhone.removePreference(mPrefRoamingWarningDisable);
            } else {
                // Remove preferences not needed for ZTE V987
                if (Build.MODEL.contains("V987") && Build.DISPLAY.contains("ZTE-CN-9B18D-P188F04")) {
                	mPrefCatFixes.removePreference(mPrefFixDateTimeCrash);
                	mPrefCatFixes.removePreference(mPrefFixTtsSettings);
                	mPrefCatFixes.removePreference(mPrefFixDevOpts);
                }

                mQuickSettings.setEntries(R.array.qs_tile_entries);
                mQuickSettings.setEntryValues(R.array.qs_tile_values);
            }

            // Remove preferences not compatible with Android 4.1
            if (Build.VERSION.SDK_INT < 17) {
                getPreferenceScreen().removePreference(mPrefCatLockscreen);
                mPrefCatStatusbar.removePreference(mPrefCatStatusbarQs);
                mPrefCatStatusbar.removePreference(mPrefCatNotifDrawerStyle);
                mPrefCatDisplay.removePreference(mPrefCatBrightness);
                mPrefCatDisplay.removePreference(mPrefCrtOff);
                mPrefCatMedia.removePreference(mPrefSafeMediaVolume);
            }

            setDefaultValues();
        }

        @Override
        public void onResume() {
            super.onResume();

            updatePreferences(null);
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

        private void setDefaultValues() {
            if (mPrefs.getStringSet(PREF_KEY_QUICK_SETTINGS, null) == null) {
                Editor e = mPrefs.edit();
                Set<String> defVal = new HashSet<String>(
                        Arrays.asList(getResources().getStringArray(
                                Utils.isMtkDevice() ? R.array.qs_tile_values : R.array.qs_tile_aosp_values))); 
                e.putStringSet(PREF_KEY_QUICK_SETTINGS, defVal);
                e.commit();
                mQuickSettings.setValues(defVal);
            }
        }

        private void updatePreferences(String key) {
            if (key == null || key.equals(PREF_KEY_BATTERY_STYLE)) {
                mBatteryStyle.setSummary(mBatteryStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOW_BATTERY_WARNING_POLICY)) {
                mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_SIGNAL_ICON_AUTOHIDE)) {
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
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_BACKGROUND)) {
                mPrefLockscreenBg.setSummary(mPrefLockscreenBg.getEntry());
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgColor);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgImage);
                String option = mPrefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT);
                if (option.equals(LOCKSCREEN_BG_COLOR)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgColor);
                } else if (option.equals(LOCKSCREEN_BG_IMAGE)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgImage);
                }
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                mPrefHwKeyMenuLongpress.setSummary(mPrefHwKeyMenuLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                mPrefHwKeyMenuDoubletap.setSummary(mPrefHwKeyMenuDoubletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                mPrefHwKeyHomeLongpress.setSummary(mPrefHwKeyHomeLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                mPrefHwKeyBackLongpress.setSummary(mPrefHwKeyBackLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_DOUBLETAP_SPEED)) {
                mPrefHwKeyDoubletapSpeed.setSummary(getString(R.string.pref_hwkey_doubletap_speed_summary)
                        + " (" + mPrefHwKeyDoubletapSpeed.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_KILL_DELAY)) {
                mPrefHwKeyKillDelay.setSummary(getString(R.string.pref_hwkey_kill_delay_summary)
                        + " (" + mPrefHwKeyKillDelay.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_PHONE_FLIP)) {
                mPrefPhoneFlip.setSummary(getString(R.string.pref_phone_flip_summary)
                        + " (" + mPrefPhoneFlip.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                mPrefSbIconColor.setEnabled(mPrefSbIconColorEnable.isChecked());
                mPrefSbDaColor.setEnabled(mPrefSbIconColorEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_BACKGROUND)) {
                mPrefNotifBackground.setSummary(mPrefNotifBackground.getEntry());
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifColor);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifColorMode);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifImagePortrait);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifImageLandscape);
                String option = mPrefs.getString(PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT);
                if (option.equals(NOTIF_BG_COLOR)) {
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColor);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColorMode);
                } else if (option.equals(NOTIF_BG_IMAGE)) {
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifImagePortrait);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifImageLandscape);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColorMode);
                }
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_COLOR_MODE)) {
                mPrefNotifColorMode.setSummary(mPrefNotifColorMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                mPrefButtonBacklightMode.setSummary(mPrefButtonBacklightMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                if (!mPrefPieEnabled.isChecked()) {
                    if (mPrefPieNavBarDisabled.isChecked()) {
                        Editor e = mPrefs.edit();
                        e.putBoolean(PREF_KEY_NAVBAR_DISABLE, false);
                        e.commit();
                        mPrefPieNavBarDisabled.setChecked(false);
                        Toast.makeText(getActivity(), getString(
                                R.string.reboot_required), Toast.LENGTH_SHORT).show();
                    }
                    if (mPrefPieHwKeysDisabled.isChecked()) {
                        Editor e = mPrefs.edit();
                        e.putBoolean(PREF_KEY_HWKEYS_DISABLE, false);
                        e.commit();
                        mPrefPieHwKeysDisabled.setChecked(false);
                    }
                    mPrefPieHwKeysDisabled.setEnabled(false);
                    mPrefPieNavBarDisabled.setEnabled(false);
                } else {
                    mPrefPieNavBarDisabled.setEnabled(true);
                    mPrefPieHwKeysDisabled.setEnabled(true);
                }
            }

            if (key == null || key.equals(PREF_KEY_RECENTS_CLEAR_ALL)) {
                mPrefRecentClear.setSummary(mPrefRecentClear.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BRIGHTNESS_MASTER_SWITCH)) {
                final boolean enabled = mPrefBrightnessMasterSwitch.isChecked();
                mPrefBrightnessMin.setEnabled(enabled);
                mPrefScreenDimLevel.setEnabled(enabled);
                mPrefAutoBrightness.setEnabled(enabled);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updatePreferences(key);

            Intent intent = new Intent();
            if (key.equals(PREF_KEY_BATTERY_STYLE)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                int batteryStyle = Integer.valueOf(prefs.getString(PREF_KEY_BATTERY_STYLE, "1"));
                intent.putExtra("batteryStyle", batteryStyle);
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                intent.putExtra("batteryPercent", prefs.getBoolean(PREF_KEY_BATTERY_PERCENT_TEXT, false));
            } else if (key.equals(PREF_KEY_SIGNAL_ICON_AUTOHIDE)) {
                intent.setAction(ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED);
                String[] autohidePrefs = mSignalIconAutohide.getValues().toArray(new String[0]);
                intent.putExtra("autohidePrefs", autohidePrefs);
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                String[] qsPrefs = mQuickSettings.getValues().toArray(new String[0]);
                intent.putExtra(EXTRA_QS_PREFS, qsPrefs);
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_COLS, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW, "3")));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_AUTOSWITCH,
                        prefs.getBoolean(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH, false));
            } else if (key.equals(PREF_KEY_QUICK_PULLDOWN)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QUICK_PULLDOWN, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_PULLDOWN, "0")));
            } else if (key.equals(PREF_KEY_STATUSBAR_BGCOLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_BG_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR_ENABLE,
                        prefs.getBoolean(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_ICON_COLOR, 
                        getResources().getInteger(R.integer.COLOR_HOLO_BLUE_LIGHT)));
            } else if (key.equals(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_DATA_ACTIVITY_COLOR,
                        prefs.getInt(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, Color.WHITE));
            } else if (key.equals(PREF_KEY_TM_STATUSBAR_LAUNCHER)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_SB_LAUNCHER, prefs.getInt(PREF_KEY_TM_STATUSBAR_LAUNCHER, 0));
            } else if (key.equals(PREF_KEY_TM_STATUSBAR_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_SB_LOCKSCREEN, prefs.getInt(PREF_KEY_TM_STATUSBAR_LOCKSCREEN, 0));
            } else if (key.equals(PREF_KEY_TM_NAVBAR_LAUNCHER)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_NB_LAUNCHER, prefs.getInt(PREF_KEY_TM_NAVBAR_LAUNCHER, 0));
            } else if (key.equals(PREF_KEY_TM_NAVBAR_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_NB_LOCKSCREEN, prefs.getInt(PREF_KEY_TM_NAVBAR_LOCKSCREEN, 0));
            } else if (key.equals(PREF_KEY_STATUSBAR_CENTER_CLOCK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CENTER_CLOCK, 
                        prefs.getBoolean(PREF_KEY_STATUSBAR_CENTER_CLOCK, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DOW,
                        prefs.getBoolean(PREF_KEY_STATUSBAR_CLOCK_DOW, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_AMPM_HIDE, prefs.getBoolean(
                        PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_HIDE, prefs.getBoolean(PREF_KEY_STATUSBAR_CLOCK_HIDE, false));
            } else if (key.equals(PREF_KEY_ALARM_ICON_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_ALARM_HIDE, prefs.getBoolean(PREF_KEY_ALARM_ICON_HIDE, false));
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
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_HOME_LONGPRESS, "0")));
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
            } else if (key.equals(PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED);
                intent.putExtra(EXTRA_VOLUME_ROCKER_WAKE_DISABLE,
                        prefs.getBoolean(PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_EXPANDABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_EXPANDABLE,
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_EXPANDABLE, true));
            } else if (key.equals(PREF_KEY_LINK_VOLUMES)) {
                intent.setAction(ACTION_PREF_LINK_VOLUMES_CHANGED);
                intent.putExtra(EXTRA_LINKED,
                        prefs.getBoolean(PREF_KEY_LINK_VOLUMES, true));
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_TYPE, prefs.getString(
                        PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT));
            } else if (key.equals(PREF_KEY_NOTIF_COLOR)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_COLOR, prefs.getInt(PREF_KEY_NOTIF_COLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_NOTIF_COLOR_MODE)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_COLOR_MODE, prefs.getString(
                        PREF_KEY_NOTIF_COLOR_MODE, NOTIF_BG_COLOR_MODE_OVERLAY));
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND_ALPHA)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_ALPHA, prefs.getInt(PREF_KEY_NOTIF_BACKGROUND_ALPHA, 60));
            } else if (key.equals(PREF_KEY_DISABLE_ROAMING_INDICATORS)) {
                intent.setAction(ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                intent.putExtra(EXTRA_INDICATORS_DISABLED,
                        prefs.getBoolean(PREF_KEY_DISABLE_ROAMING_INDICATORS, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                boolean enabled = prefs.getBoolean(PREF_KEY_PIE_CONTROL_ENABLE, false);
                intent.putExtra(EXTRA_PIE_ENABLE, enabled);
                if (!enabled) {
                    intent.putExtra(EXTRA_PIE_HWKEYS_DISABLE, false);
                }
            } else if (key.equals(PREF_KEY_PIE_CONTROL_SEARCH)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_SEARCH, prefs.getBoolean(PREF_KEY_PIE_CONTROL_SEARCH, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_MENU)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_MENU, prefs.getBoolean(PREF_KEY_PIE_CONTROL_MENU, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_TRIGGERS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                String[] triggers = prefs.getStringSet(
                        PREF_KEY_PIE_CONTROL_TRIGGERS, new HashSet<String>()).toArray(new String[0]);
                intent.putExtra(EXTRA_PIE_TRIGGERS, triggers);
            } else if (key.equals(PREF_KEY_PIE_CONTROL_TRIGGER_SIZE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_TRIGGER_SIZE, 
                        prefs.getInt(PREF_KEY_PIE_CONTROL_TRIGGER_SIZE, 5));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_SIZE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_SIZE, prefs.getInt(PREF_KEY_PIE_CONTROL_SIZE, 1000));
            } else if (key.equals(PREF_KEY_HWKEYS_DISABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_HWKEYS_DISABLE, prefs.getBoolean(PREF_KEY_HWKEYS_DISABLE, false));
            } else if (key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                intent.setAction(ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                intent.putExtra(EXTRA_BB_MODE, prefs.getString(
                        PREF_KEY_BUTTON_BACKLIGHT_MODE, BB_MODE_DEFAULT));
            } else if (key.equals(PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS)) {
                intent.setAction(ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                intent.putExtra(EXTRA_BB_NOTIF, prefs.getBoolean(
                        PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS, false));
            } else if (key.equals(PREF_KEY_QUICKAPP_DEFAULT)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_DEFAULT, prefs.getString(PREF_KEY_QUICKAPP_DEFAULT, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT1)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT1, prefs.getString(PREF_KEY_QUICKAPP_SLOT1, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT2, prefs.getString(PREF_KEY_QUICKAPP_SLOT2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT3, prefs.getString(PREF_KEY_QUICKAPP_SLOT3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT4, prefs.getString(PREF_KEY_QUICKAPP_SLOT4, null));
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
            } else if (pref == mPrefNotifImagePortrait) {
                setCustomNotifBgPortrait();
                return true;
            } else if (pref == mPrefNotifImageLandscape) {
                setCustomNotifBgLandscape();
                return true;
            } else if (pref == mPrefGbThemeDark) {
                File file = new File(getActivity().getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
                if (mPrefGbThemeDark.isChecked()) {
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (file.exists()) {
                        file.delete();
                    }
                }
                getActivity().recreate();
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

        @SuppressWarnings("deprecation")
        private void setCustomNotifBgPortrait() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("outputX", isPortrait ? width : height);
            intent.putExtra("outputY", isPortrait ? height : width);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, NOTIF_BG_IMAGE_PORTRAIT);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomNotifBgLandscape() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                  .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("aspectY", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("outputX", isPortrait ? height : width);
            intent.putExtra("outputY", isPortrait ? width : height);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, NOTIF_BG_IMAGE_LANDSCAPE);
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
            } else if (requestCode == NOTIF_BG_IMAGE_PORTRAIT) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(notifBgImagePortrait);
                    }
                    notifBgImagePortrait.setReadable(true, false);
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
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == NOTIF_BG_IMAGE_LANDSCAPE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(notifBgImageLandscape);
                    }
                    notifBgImageLandscape.setReadable(true, false);
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
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            }
        }
    }
}