package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GeminiPhoneWrapper;
import com.ceco.gm2.gravitybox.GravityBox;

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
        mLabel = mGbResources.getString(mGbResources.getIdentifier(
                "qs_tile_network_mode", "string", GravityBox.PACKAGE_NAME));

        int mTileLayoutId = mGbResources.getIdentifier(
                "quick_settings_tile_network_mode", "layout", GravityBox.PACKAGE_NAME);
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(mTileLayoutId, mTile);

        mTextView = (TextView) mTile.findViewById(
                mGbResources.getIdentifier("network_mode_tileview", "id", GravityBox.PACKAGE_NAME));

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
                mDrawableId = mGbResources.getIdentifier(
                        "ic_qs_2g3g_on", "drawable", GravityBox.PACKAGE_NAME);
                break;
            case GeminiPhoneWrapper.NT_WCDMA_ONLY:
                mDrawableId = mGbResources.getIdentifier(
                        "ic_qs_3g_on", "drawable", GravityBox.PACKAGE_NAME);
                break;
            case GeminiPhoneWrapper.NT_GSM_ONLY:
                mDrawableId = mGbResources.getIdentifier(
                        "ic_qs_2g_on", "drawable", GravityBox.PACKAGE_NAME);
                break;
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}