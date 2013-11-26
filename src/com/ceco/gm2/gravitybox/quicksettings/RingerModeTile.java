/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.Utils;

import de.robv.android.xposed.XposedBridge;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class RingerModeTile extends AQuickSettingsTile {
    private static final String TAG = "GB:RingerModeTile";
    private static final boolean DEBUG = false;

    public static final String SETTING_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    public static final String SETTING_MODE_RINGER = "mode_ringer";

    // Define the available ringer modes
    private final Ringer mSilentRinger = new Ringer(AudioManager.RINGER_MODE_SILENT, false);
    private final Ringer mVibrateRinger = new Ringer(AudioManager.RINGER_MODE_VIBRATE, true);
    private final Ringer mSoundRinger = new Ringer(AudioManager.RINGER_MODE_NORMAL, false);
    private final Ringer mSoundVibrateRinger = new Ringer(AudioManager.RINGER_MODE_NORMAL, true);

    private Ringer[] mRingers;
    private int mRingersIndex;
    private int[] mRingerValues;
    private int mRingerValuesIndex;
    private TextView mTextView;
    private boolean mHasVibrator;

    private AudioManager mAudioManager;
    private SettingsObserver mSettingsObserver;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Received broadcast: " + intent.toString());
            updateResources();
        }
    };

    public RingerModeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                toggleState();
                updateResources();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startActivity(android.provider.Settings.ACTION_SOUND_SETTINGS);
                return true;
            }
        };

        mHasVibrator = Utils.hasVibrator(mContext);
        if (mHasVibrator) {
            mRingers = new Ringer[] { 
                    mSilentRinger, mVibrateRinger, mSoundRinger, mSoundVibrateRinger };
            mRingerValues = new int[] { 0, 1, 2, 3 };
        } else {
            mRingers = new Ringer[] { mSilentRinger, mSoundRinger };
            mRingerValues = new int[] { 0, 1 };
        }
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_ringer_mode, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.ringer_mode_tileview);

        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected synchronized void updateTile() {
        // The title does not change
        mLabel = mGbContext.getString(R.string.qs_tile_ringer_mode);

        // The icon will change depending on index
        findCurrentState();
        switch (mRingersIndex) {
            case 0:
                mDrawableId = R.drawable.ic_qs_ring_off;
                break;
            case 1:
                mDrawableId = mHasVibrator ? 
                        R.drawable.ic_qs_vibrate_on : R.drawable.ic_qs_ring_on;
                break;
            case 2:
                mDrawableId = R.drawable.ic_qs_ring_on;
                break;
            case 3:
                mDrawableId = R.drawable.ic_qs_ring_vibrate_on;
                break;
        }

        for (int i = 0; i < mRingerValues.length; i++) {
            if (mRingersIndex == mRingerValues[i]) {
                mRingerValuesIndex = i;
                break;
            }
        }

        mTextView.setText(mLabel);
        if (mTileStyle == KITKAT) {
            Drawable d = mGbResources.getDrawable(mDrawableId).mutate();
            d.setColorFilter(mDrawableId == R.drawable.ic_qs_ring_off ?
                    KK_COLOR_OFF : KK_COLOR_ON, PorterDuff.Mode.SRC_ATOP);
            mTextView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        } else {
            mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
        }
    }

    protected void toggleState() {
        mRingerValuesIndex++;
        if (mRingerValuesIndex > mRingerValues.length - 1) {
            mRingerValuesIndex = 0;
        }

        mRingersIndex = mRingerValues[mRingerValuesIndex];
        if (mRingersIndex > mRingers.length - 1) {
            mRingersIndex = 0;
        }

        Ringer ringer = mRingers[mRingersIndex];
        ringer.execute(mContext);
    }

    private void findCurrentState() {
        ContentResolver resolver = mContext.getContentResolver();
        boolean vibrateWhenRinging = mHasVibrator ? Settings.System.getInt(resolver,
                SETTING_VIBRATE_WHEN_RINGING, 0) == 1 : false;
        int ringerMode = mAudioManager.getRingerMode();

        Ringer ringer = new Ringer(ringerMode, vibrateWhenRinging);
        for (int i = 0; i < mRingers.length; i++) {
            if (mRingers[i].equals(ringer)) {
                mRingersIndex = i;
                break;
            }
        }
    }

    private class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_MODE_RINGER), false, this);
            if (mHasVibrator) {
                resolver.registerContentObserver(Settings.System.getUriFor(
                        SETTING_VIBRATE_WHEN_RINGING), false, this);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) log("SettingsObserver onChange()");
            updateResources();
        }
    }

    private class Ringer {
        final boolean mVibrateWhenRinging;
        final int mRingerMode;

        Ringer( int ringerMode, boolean vibrateWhenRinging) {
            mVibrateWhenRinging = vibrateWhenRinging;
            mRingerMode = ringerMode;
        }

        void execute(Context context) {
            // If we are setting a vibrating state, vibrate to indicate it
            if (mVibrateWhenRinging) {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(250);
            }

            // Set the desired state
            ContentResolver resolver = context.getContentResolver();
            Settings.System.putInt(resolver, SETTING_VIBRATE_WHEN_RINGING,
                    mVibrateWhenRinging ? 1 : 0);
            mAudioManager.setRingerMode(mRingerMode);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o.getClass() != getClass()) {
                return false;
            }
            Ringer r = (Ringer) o;
            if ((mRingerMode == AudioManager.RINGER_MODE_SILENT || mRingerMode == AudioManager.RINGER_MODE_VIBRATE)
                    && (r.mRingerMode == mRingerMode))
                return true;
            return r.mVibrateWhenRinging == mVibrateWhenRinging
                    && r.mRingerMode == mRingerMode;
        }
    }
}
