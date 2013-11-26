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

import com.ceco.gm2.gravitybox.ModHwKeys;
import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SleepTile extends AQuickSettingsTile {

    public SleepTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    pm.goToSleep(SystemClock.uptimeMillis());
                } catch(Exception e) {
                    XposedBridge.log(e);
                }
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(ModHwKeys.ACTION_SHOW_POWER_MENU);
                mContext.sendBroadcast(intent);
                collapsePanels();
                return true;
            }
        };
    }

    @Override
    protected void onTileCreate() {
        mDrawableId = R.drawable.ic_qs_sleep;
        mLabel = mGbResources.getString(R.string.qs_tile_sleep);

        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_sleep, mTile);
    }

    @Override
    protected void updateTile() {
        TextView tv = (TextView) mTile.findViewById(R.id.sleep_tileview);
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