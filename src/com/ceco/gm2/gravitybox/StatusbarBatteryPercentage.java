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

package com.ceco.gm2.gravitybox;

import com.ceco.gm2.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.gm2.gravitybox.StatusBarIconManager.IconManagerListener;

import android.widget.TextView;

public class StatusbarBatteryPercentage implements IconManagerListener {
    private TextView mPercentage;
    private int mDefaultColor;

    public StatusbarBatteryPercentage(TextView clockView) {
        mPercentage = clockView;
        mDefaultColor = mPercentage.getCurrentTextColor();
    }

    public TextView getView() {
        return mPercentage;
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            if (colorInfo.coloringEnabled) {
                mPercentage.setTextColor(colorInfo.iconColor[0]);
            } else {
                if (colorInfo.followStockBatteryColor && colorInfo.stockBatteryColor != null) {
                    mPercentage.setTextColor(colorInfo.stockBatteryColor);
                } else {
                    mPercentage.setTextColor(mDefaultColor);
                }
            }
        } else if ((flags & StatusBarIconManager.FLAG_LOW_PROFILE_CHANGED) != 0) {
            mPercentage.setAlpha(colorInfo.lowProfile ? 0.5f : 1);
        }
    }
}
