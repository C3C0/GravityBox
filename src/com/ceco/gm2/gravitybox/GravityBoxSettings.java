package com.ceco.gm2.gravitybox;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class GravityBoxSettings extends Activity {
    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private ListPreference mBatteryStyle;
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
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updatePreferences();			
        }
    }
}
