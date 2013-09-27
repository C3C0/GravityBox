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
    private static final String TAG = "GB:NetworkModeTile";
    private static final boolean DEBUG = false;

    public static final String SETTING_NETWORK_MODE_TILE_MODE = "gb_network_mode_tile_mode";

    private TextView mTextView;
    private int mNetworkType;
    private int mDefaultNetworkType;
    private boolean mAllow3gOnly;
    private boolean mAllow2g3g;

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
            resolver.registerContentObserver(
                    Settings.System.getUriFor(SETTING_NETWORK_MODE_TILE_MODE), false, this);
        }

        @SuppressLint("NewApi")
        @Override
        public void onChange(boolean selfChange) {
            ContentResolver cr = mContext.getContentResolver();
            mNetworkType = Settings.Global.getInt(cr, 
                    PhoneWrapper.PREFERRED_NETWORK_MODE, PhoneWrapper.NT_WCDMA_PREFERRED);
            updateFlags(Settings.System.getInt(cr, SETTING_NETWORK_MODE_TILE_MODE, 0));

            if (DEBUG) log("SettingsObserver onChange; mNetworkType = " + mNetworkType +
                    "; mAllow3gOnly = " + mAllow3gOnly);

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
                        if (!mAllow2g3g) {
                            i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE,
                                    hasLte() ? mDefaultNetworkType : PhoneWrapper.NT_GSM_ONLY);
                        } else {
                            i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, PhoneWrapper.NT_WCDMA_PREFERRED);
                        }
                        break;
                    case PhoneWrapper.NT_GSM_ONLY:
                        i.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, mAllow3gOnly ?
                                    PhoneWrapper.NT_WCDMA_ONLY : PhoneWrapper.NT_WCDMA_PREFERRED);
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

        ContentResolver cr = mContext.getContentResolver();
        mNetworkType = Settings.Global.getInt(cr, 
                PhoneWrapper.PREFERRED_NETWORK_MODE, mDefaultNetworkType);

        updateFlags(Settings.System.getInt(cr, SETTING_NETWORK_MODE_TILE_MODE, 0));

        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();
    }

    private void updateFlags(int nmMode) {
        mAllow3gOnly = (nmMode == 0) || (nmMode == 2);
        mAllow2g3g = (nmMode < 2);
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