package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBox;
import com.ceco.gm2.gravitybox.GravityBoxResultReceiver;
import com.ceco.gm2.gravitybox.GravityBoxResultReceiver.Receiver;
import com.ceco.gm2.gravitybox.GravityBoxService;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SyncTile extends AQuickSettingsTile {

    private TextView mTextView;
    private Handler mHandler;
    private Object mSyncObserverHandle = null;
    private GravityBoxResultReceiver mReceiver;
    private boolean mSyncState;

    public SyncTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                toggleState();
            }
        };

        mHandler = new Handler();
    }

    @Override
    protected void onTileCreate() {
        int mTileLayoutId = mGbResources.getIdentifier("quick_settings_tile_sync", "layout", GravityBox.PACKAGE_NAME);
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(mTileLayoutId, mTile);
        mTextView = (TextView) mTile.findViewById(
                mGbResources.getIdentifier("sync_tileview", "id", GravityBox.PACKAGE_NAME));

        mReceiver = new GravityBoxResultReceiver(mHandler);
        mReceiver.setReceiver(new Receiver() {

            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == GravityBoxService.RESULT_SYNC_STATUS) {
                    mSyncState = resultData.getBoolean(GravityBoxService.KEY_SYNC_STATUS);
                    updateResources();
                }
            }
            
        });

        mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncObserver);
    }

    @Override
    protected void onTilePostCreate() {
        getSyncState();
    }

    @Override
    protected void updateTile() {
        if (mSyncState) {
            mDrawableId = mGbResources.getIdentifier("ic_qs_sync_on", "drawable", GravityBox.PACKAGE_NAME);
            mLabel = mGbResources.getString(
                    mGbResources.getIdentifier("quick_settings_sync_on", "string", GravityBox.PACKAGE_NAME));
        } else {
            mDrawableId = mGbResources.getIdentifier("ic_qs_sync_off", "drawable", GravityBox.PACKAGE_NAME);
            mLabel = mGbResources.getString(
                    mGbResources.getIdentifier("quick_settings_sync_off", "string", GravityBox.PACKAGE_NAME));            
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    private void getSyncState() {
        Intent si = new Intent(mGbContext, GravityBoxService.class);
        si.setAction(GravityBoxService.ACTION_GET_SYNC_STATUS);
        si.putExtra("receiver", mReceiver);
        mGbContext.startService(si);
    }

    private void toggleState() {
        Intent si = new Intent(mGbContext, GravityBoxService.class);
        si.setAction(GravityBoxService.ACTION_TOGGLE_SYNC);
        mGbContext.startService(si);
    }

    private SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            // update state/view if something happened
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getSyncState();
                }
            });
        }
    };
}