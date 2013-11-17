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

public class StatusbarClock implements IconManagerListener {
    private TextView mClock;
    private int mDefaultClockColor;
    private int mOriginalPaddingLeft;

    public StatusbarClock(TextView clockView) {
        mClock = clockView;
        mDefaultClockColor = mClock.getCurrentTextColor();
        mOriginalPaddingLeft = mClock.getPaddingLeft();
    }

    public TextView getView() {
        return mClock;
    }

    public void resetOriginalPaddingLeft() {
        if (mClock != null) {
            mClock.setPadding(mOriginalPaddingLeft, 0, 0, 0);
        }
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            if (colorInfo.coloringEnabled) {
                mClock.setTextColor(colorInfo.iconColor[0]);
            } else {
                if (colorInfo.followStockBatteryColor && colorInfo.stockBatteryColor != null) {
                    mClock.setTextColor(colorInfo.stockBatteryColor);
                } else {
                    mClock.setTextColor(mDefaultClockColor);
                }
            }
        }
    }
}
