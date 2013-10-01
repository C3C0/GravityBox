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

import com.ceco.gm2.gravitybox.GravityBoxSettings;
import com.ceco.gm2.gravitybox.ModExpandedDesktop;
import com.ceco.gm2.gravitybox.R;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ExpandedDesktopTile extends AQuickSettingsTile {

    private TextView mTextView;
    private int mMode;
    private boolean mExpanded;
    private Handler mHandler;
    private SettingsObserver mSettingsObserver;

    public ExpandedDesktopTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMode != GravityBoxSettings.ED_DISABLED) {
                    collapsePanels();
                    // give panels chance to collapse before changing expanded desktop state
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Settings.System.putInt(mGbContext.getContentResolver(),
                                    ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                    (mExpanded ? 0 : 1));
                        }
                    }, 300);
                }
            }
        };
    }

    class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                   ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_MODE) , false, this);
            cr.registerContentObserver(Settings.System.getUriFor(
                   ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE), false, this);
        }

        @Override 
        public void onChange(boolean selfChange) {
            updateResources();
        }
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_expanded_desktop, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.expanded_tileview);

        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
    }

    @Override
    protected synchronized void updateTile() {
        mMode = Settings.System.getInt(mContext.getContentResolver(),
                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_MODE, 0);
        mExpanded = (Settings.System.getInt(mContext.getContentResolver(),
                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0) == 1)
                && (mMode > 0);

        if (mExpanded) {
            mLabel = mGbContext.getString(R.string.quick_settings_expanded_desktop_expanded);
            mDrawableId = R.drawable.ic_qs_expanded_desktop_on;
        } else {
            mLabel = (mMode == GravityBoxSettings.ED_DISABLED) ? 
                    mGbContext.getString(R.string.quick_settings_expanded_desktop_disabled) :
                        mGbContext.getString(R.string.quick_settings_expanded_desktop_normal);
            mDrawableId = R.drawable.ic_qs_expanded_desktop_off;
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}