package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GeminiPhoneWrapper;
import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedBridge;

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

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(GeminiPhoneWrapper.PREFERRED_NETWORK_MODE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            mNetworkType = Settings.Global.getInt(mContext.getContentResolver(), 
                    GeminiPhoneWrapper.PREFERRED_NETWORK_MODE, GeminiPhoneWrapper.NT_WCDMA_PREFERRED);
            log("SettingsObserver onChange; mNetworkType = " + mNetworkType);
            updateResources();
        }
    }

    public NetworkModeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent i = new Intent(GeminiPhoneWrapper.ACTION_CHANGE_NETWORK_TYPE);
                switch (mNetworkType) {
                    case GeminiPhoneWrapper.NT_WCDMA_PREFERRED:
                    case GeminiPhoneWrapper.NT_GSM_WCDMA_AUTO:
                        i.putExtra(GeminiPhoneWrapper.EXTRA_NETWORK_TYPE, 
                                GeminiPhoneWrapper.NT_GSM_ONLY);
                        break;
                    case GeminiPhoneWrapper.NT_WCDMA_ONLY:
                        i.putExtra(GeminiPhoneWrapper.EXTRA_NETWORK_TYPE, 
                                GeminiPhoneWrapper.NT_WCDMA_PREFERRED);
                        break;
                    case GeminiPhoneWrapper.NT_GSM_ONLY:
                        i.putExtra(GeminiPhoneWrapper.EXTRA_NETWORK_TYPE, 
                                GeminiPhoneWrapper.NT_WCDMA_ONLY);
                        break;
                }
                mContext.sendBroadcast(i);
            }
        };
    }

    @Override
    protected void onTileCreate() {
        mLabel = mGbResources.getString(R.string.qs_tile_network_mode);

        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_network_mode, mTile);

        mTextView = (TextView) mTile.findViewById(R.id.network_mode_tileview);

        mNetworkType = Settings.Global.getInt(mContext.getContentResolver(), 
                GeminiPhoneWrapper.PREFERRED_NETWORK_MODE, GeminiPhoneWrapper.NT_WCDMA_PREFERRED);
        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();
    }

    @Override
    protected synchronized void updateTile() {

        switch (mNetworkType) {
            case GeminiPhoneWrapper.NT_WCDMA_PREFERRED:
            case GeminiPhoneWrapper.NT_GSM_WCDMA_AUTO:
                mDrawableId = R.drawable.ic_qs_2g3g_on;
                break;
            case GeminiPhoneWrapper.NT_WCDMA_ONLY:
                mDrawableId = R.drawable.ic_qs_3g_on;
                break;
            case GeminiPhoneWrapper.NT_GSM_ONLY:
                mDrawableId = R.drawable.ic_qs_2g_on;
                break;
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}