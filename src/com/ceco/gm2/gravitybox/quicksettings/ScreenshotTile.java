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

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ScreenshotTile extends AQuickSettingsTile {

    private TextView mTextView;

    public ScreenshotTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsePanels();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(ModHwKeys.ACTION_SCREENSHOT);
                        mContext.sendBroadcast(intent);
                    }
                }, 1000);
            }
        };
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_screenshot, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.screenshot_tileview);
    }

    @Override
    protected synchronized void updateTile() {
        mDrawableId = R.drawable.ic_qs_screenshot;
        mLabel = mGbContext.getString(R.string.qs_tile_screenshot);
        mTextView.setText(mLabel);
        if (mTileStyle == KITKAT) {
            Drawable d = mGbResources.getDrawable(mDrawableId).mutate();
            d.setColorFilter(KK_COLOR_ON, PorterDuff.Mode.SRC_ATOP);
            mTextView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        } else {
            mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
        }
    }
}
