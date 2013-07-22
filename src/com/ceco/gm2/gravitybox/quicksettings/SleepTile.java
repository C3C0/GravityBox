package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBox;

import de.robv.android.xposed.XposedBridge;
import android.content.Context;
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
    }

    @Override
    protected void onTileCreate() {
        mDrawableId = mGbResources.getIdentifier("ic_qs_sleep", "drawable", GravityBox.PACKAGE_NAME);
        mLabel = mGbResources.getString(mGbResources.getIdentifier(
                "qs_tile_sleep", "string", GravityBox.PACKAGE_NAME));

        int mTileLayoutId = mGbResources.getIdentifier("quick_settings_tile_sleep", "layout", GravityBox.PACKAGE_NAME);
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(mTileLayoutId, mTile);
    }

    @Override
    protected void updateTile() {
        TextView tv = (TextView) mTile.findViewById(
                mGbResources.getIdentifier("sleep_tileview", "id", GravityBox.PACKAGE_NAME));
        tv.setText(mLabel);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

}