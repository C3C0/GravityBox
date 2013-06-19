package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
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
    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";

    public static final String PREF_KEY_VOL_KEY_CURSOR_CONTROL = "pref_vol_key_cursor_control";
    public static final int VOL_KEY_CURSOR_CONTROL_OFF = 0;
    public static final int VOL_KEY_CURSOR_CONTROL_ON = 1;
    public static final int VOL_KEY_CURSOR_CONTROL_ON_REVERSE = 2;

    public static final String PREF_KEY_RECENTS_CLEAR_ALL = "pref_recents_clear_all";
    public static final String PREF_KEY_CALLER_FULLSCREEN_PHOTO = "pref_caller_fullscreen_photo";
    public static final String PREF_KEY_FIX_DATETIME_CRASH = "pref_fix_datetime_crash";
    public static final String PREF_KEY_FIX_CALLER_ID_PHONE = "pref_fix_caller_id_phone";
    public static final String PREF_KEY_FIX_CALLER_ID_MMS = "pref_fix_caller_id_mms";
    public static final String PREF_KEY_FIX_CALENDAR = "pref_fix_calendar";
    public static final String PREF_KEY_STATUSBAR_BGCOLOR = "pref_statusbar_bgcolor";
    public static final String PREF_KEY_FIX_TTS_SETTINGS = "pref_fix_tts_settings";
    public static final String PREF_KEY_FIX_DEV_OPTS = "pref_fix_dev_opts";
    public static final String PREF_KEY_ABOUT_GRAVITYBOX = "pref_about_gb";
    public static final String PREF_KEY_ABOUT_XPOSED = "pref_about_xposed";
    public static final String PREF_KEY_ABOUT_DONATE = "pref_about_donate";
    public static final String PREF_KEY_CRT_OFF_EFFECT = "pref_crt_off_effect";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";
    public static final String ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED = "gravitybox.intent.action.SIGNAL_ICON_AUTOHIDE_CHANGED";

    public static final String ACTION_PREF_STATUSBAR_BGCOLOR_CHANGED = "gravitybox.intent.action.STATUSBAR_BGCOLOR_CHANGED";
    public static final String EXTRA_SB_BGCOLOR = "bgColor";

    public static final String ACTION_PREF_QUICKSETTINGS_CHANGED = "gravitybox.intent.action.QUICKSETTINGS_CHANGED";
    public static final String EXTRA_QS_PREFS = "qsPrefs";

    private static final List<String> rebootKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_FIX_DATETIME_CRASH,
            PREF_KEY_FIX_CALENDAR,
            PREF_KEY_FIX_CALLER_ID_PHONE,
            PREF_KEY_FIX_CALLER_ID_MMS,
            PREF_KEY_FIX_TTS_SETTINGS,
            PREF_KEY_FIX_DEV_OPTS
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
            }
            getActivity().sendBroadcast(intent);

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
    }
}
