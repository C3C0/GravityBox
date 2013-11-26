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
import com.ceco.gm2.gravitybox.TorchService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class TorchTile extends AQuickSettingsTile {

    private TextView mTextView;
    private int mTorchStatus = TorchService.TORCH_STATUS_OFF;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TorchService.ACTION_TORCH_STATUS_CHANGED) &&
                    intent.hasExtra(TorchService.EXTRA_TORCH_STATUS)) {
                mTorchStatus = intent.getIntExtra(TorchService.EXTRA_TORCH_STATUS,
                        TorchService.TORCH_STATUS_OFF);
                updateResources();
            }
        }
        
    };

    public TorchTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                toggleState();
            }
        };
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_torch, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.torch_tileview);

        IntentFilter intentFilter = new IntentFilter(TorchService.ACTION_TORCH_STATUS_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void updateTile() {
        if (mTorchStatus == TorchService.TORCH_STATUS_ON) {
            mDrawableId = R.drawable.ic_qs_torch_on;
            mLabel = mGbResources.getString(R.string.quick_settings_torch_on);
        } else {
            mDrawableId = R.drawable.ic_qs_torch_off;
            mLabel = mGbResources.getString(R.string.quick_settings_torch_off);
        }

        mTextView.setText(mLabel);
        if (mTileStyle == KITKAT) {
            Drawable d = mGbResources.getDrawable(mDrawableId).mutate();
            d.setColorFilter(mTorchStatus == TorchService.TORCH_STATUS_ON ? 
                    KK_COLOR_ON : KK_COLOR_OFF, PorterDuff.Mode.SRC_ATOP);
            mTextView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        } else {
            mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
        }
    }

    private void toggleState() {
        Intent si = new Intent(mGbContext, TorchService.class);
        si.setAction(TorchService.ACTION_TOGGLE_TORCH);
        mGbContext.startService(si);
    }
}