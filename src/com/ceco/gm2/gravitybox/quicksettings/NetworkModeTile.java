package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.PhoneWrapper;
import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedBridge;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class NetworkModeTile extends AQuickSettingsTile {
    private static final String TAG = "NetworkModeTile";

    private TextView mTextView;
    private int mNetworkType;
    private int mDefaultNetworkType;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @SuppressLint("NewApi")
        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(PhoneWrapper.PREFERRED_NETWORK_MODE), false, this);
        }

        @SuppressLint("NewApi")
        @Override
        public void onChange(boolean selfChange) {
            mNetworkType = Settings.Global.getInt(mContext.getContentResolver(), 
                    PhoneWrapper.PREFERRED_NETWORK_MODE, PhoneWrapper.NT_WCDMA_PREFERRED);
            log("SettingsObserver onChange; mNetworkType = " + mNetworkType);
            updateResources();
        }
    }

    public NetworkModeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent i = new Intent(PhoneWrapper.ACTION_CHANGE_NETWORK_TYPE);
                switch (mNetworkType) {
                    case PhoneWrapper.NT_WCDMA_PREFERRED:
                    case PhoneWrapper.NT_GSM_WCDMA_AUTO:
                        i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, 
                                hasLte() ? mDefaultNetworkType : 
                                    PhoneWrapper.NT_GSM_ONLY);
                        break;
                    case PhoneWrapper.NT_WCDMA_ONLY:
                        i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, 
                                PhoneWrapper.NT_WCDMA_PREFERRED);
                        break;
                    case PhoneWrapper.NT_GSM_ONLY:
                        i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, 
                                PhoneWrapper.NT_WCDMA_ONLY);
                        break;
                    default:
                        if (hasLte()) {
                            i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, 
                                    PhoneWrapper.NT_GSM_ONLY);
                        } else {
                            log("onClick: Unknown or unsupported network type: mNetworkType = " + mNetworkType);
                        }
                        break;
                }
                if (i.hasExtra(PhoneWrapper.EXTRA_NETWORK_TYPE)) {
                    mContext.sendBroadcast(i);
                }
            }
        };
    }

    @SuppressLint("NewApi")
    @Override
    protected void onTileCreate() {
        mLabel = mGbResources.getString(R.string.qs_tile_network_mode);

        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_network_mode, mTile);

        mTextView = (TextView) mTile.findViewById(R.id.network_mode_tileview);

        mDefaultNetworkType = PhoneWrapper.getDefaultNetworkType();
        mNetworkType = Settings.Global.getInt(mContext.getContentResolver(), 
                PhoneWrapper.PREFERRED_NETWORK_MODE, mDefaultNetworkType);

        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();
    }

    @Override
    protected synchronized void updateTile() {

        switch (mNetworkType) {
            case PhoneWrapper.NT_WCDMA_PREFERRED:
            case PhoneWrapper.NT_GSM_WCDMA_AUTO:
                mDrawableId = R.drawable.ic_qs_2g3g_on;
                break;
            case PhoneWrapper.NT_WCDMA_ONLY:
                mDrawableId = R.drawable.ic_qs_3g_on;
                break;
            case PhoneWrapper.NT_GSM_ONLY:
                mDrawableId = R.drawable.ic_qs_2g_on;
                break;
            default:
                if (mNetworkType >= PhoneWrapper.NT_LTE_CDMA_EVDO 
                        && mNetworkType < PhoneWrapper.NT_MODE_UNKNOWN) {
                    mDrawableId = R.drawable.ic_qs_lte;
                } else {
                    mDrawableId = R.drawable.ic_qs_unexpected_network;
                    log("updateTile: Unknown or unsupported network type: mNetworkType = " + mNetworkType);
                }
                break;
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    private boolean hasLte() {
        return (mDefaultNetworkType >= PhoneWrapper.NT_LTE_CDMA_EVDO && 
                mDefaultNetworkType < PhoneWrapper.NT_MODE_UNKNOWN);
    }
}