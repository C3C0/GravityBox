package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.R;
import de.robv.android.xposed.XposedBridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class GpsTile extends AQuickSettingsTile {
    private static final String TAG = "GpsTile";

    private boolean mGpsEnabled;
    private TextView mTextView;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private BroadcastReceiver mLocationManagerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mGpsEnabled = Settings.Secure.isLocationProviderEnabled(
                    mContext.getContentResolver(), LocationManager.GPS_PROVIDER);
            log("Broadcast received: mGpsEnabled = " + mGpsEnabled);
            updateResources();
        }
    };

    public GpsTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Settings.Secure.setLocationProviderEnabled(
                        mContext.getContentResolver(), LocationManager.GPS_PROVIDER, !mGpsEnabled);
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startActivity(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                return false;
            }
        };

        mGpsEnabled = Settings.Secure.isLocationProviderEnabled(
                mContext.getContentResolver(), LocationManager.GPS_PROVIDER);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        mContext.registerReceiver(mLocationManagerReceiver, intentFilter);
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_gps, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.gps_tileview);
    }

    @Override
    protected synchronized void updateTile() {
        if (mGpsEnabled) {
            mLabel = mGbContext.getString(R.string.qs_tile_gps_enabled);
            mDrawableId = R.drawable.ic_qs_gps_enable;
        } else {
            mLabel = mGbContext.getString(R.string.qs_tile_gps_disabled);
            mDrawableId = R.drawable.ic_qs_gps_disable;
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}
