package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class GravityBoxSettings extends Activity {
    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_PERCENT = 3;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    public static final int BATTERY_WARNING_POPUP = 1;
    public static final int BATTERY_WARNING_SOUND = 2;

    public static final String PREF_KEY_SIGNAL_ICON_AUTOHIDE = "pref_signal_icon_autohide";
    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";
    public static final String ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED = "gravitybox.intent.action.SIGNAL_ICON_AUTOHIDE_CHANGED";

    private static final List<String> rebootKeys = new ArrayList<String>();
    
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

            super.onPause();
        }

        private void updatePreferences() {
            mBatteryStyle.setSummary(mBatteryStyle.getEntry());
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());

            Set<String> autoHide =  mSignalIconAutohide.getValues();
            String summary = getString(R.string.signal_icon_autohide_summary);
            if (autoHide != null && autoHide.size() > 0) {
                summary = "";
                for (String str: autoHide) {
                    if (!summary.isEmpty()) summary += ", ";
                    summary += str.equals("sim1") ? getString(R.string.sim_slot_1) : getString(R.string.sim_slot_2);
                }
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
            }
            getActivity().sendBroadcast(intent);

            if (rebootKeys.contains(key))
                Toast.makeText(getActivity(), getString(R.string.reboot_required), Toast.LENGTH_SHORT).show();
        }
    }
}
