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

import de.robv.android.xposed.XposedHelpers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class VolumeTile extends AQuickSettingsTile {

    public VolumeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels");
                AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startActivity(android.provider.Settings.ACTION_SOUND_SETTINGS);
                return true;
            }
        };
    }

    @Override
    protected void onTileCreate() {
        mDrawableId = R.drawable.ic_qs_volume;
        mLabel = mGbContext.getString(R.string.qs_tile_volume);

        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_volume, mTile);
    }

    @Override
    protected synchronized void updateTile() {
        TextView tv = (TextView) mTile.findViewById(R.id.volume_tileview);
        tv.setText(mLabel);
        if (mTileStyle == KITKAT) {
            Drawable d = mGbResources.getDrawable(mDrawableId).mutate();
            d.setColorFilter(KK_COLOR_ON, PorterDuff.Mode.SRC_ATOP);
            tv.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
        }
    }
}