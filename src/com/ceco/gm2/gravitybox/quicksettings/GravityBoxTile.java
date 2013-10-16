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

import com.ceco.gm2.gravitybox.GravityBox;
import com.ceco.gm2.gravitybox.GravityBoxSettings;
import com.ceco.gm2.gravitybox.R;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class GravityBoxTile extends AQuickSettingsTile {

    public GravityBoxTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(GravityBox.PACKAGE_NAME, GravityBoxSettings.class.getName());
                startActivity(i);
            }
        };

        if (Build.VERSION.SDK_INT > 16) {
            mOnLongClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent i = new Intent();
                    i.setClassName(GravityBox.PACKAGE_NAME, TileOrderActivity.class.getName());
                    startActivity(i);
                    return true;
                }
            };
        }
    }

    @Override
    protected void onTileCreate() {
        mDrawableId = R.drawable.ic_qs_gravitybox;
        mLabel = "GravityBox";

        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_gravity, mTile);
    }

    @Override
    protected synchronized void updateTile() {
        TextView tv = (TextView) mTile.findViewById(R.id.gravitybox_tileview);
        tv.setText(mLabel);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}