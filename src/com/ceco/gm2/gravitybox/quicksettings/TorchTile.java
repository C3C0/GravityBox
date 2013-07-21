package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBox;
import com.ceco.gm2.gravitybox.TorchService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        int mTileLayoutId = mGbResources.getIdentifier("quick_settings_tile_torch", "layout", GravityBox.PACKAGE_NAME);
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(mTileLayoutId, mTile);
        mTextView = (TextView) mTile.findViewById(
                mGbResources.getIdentifier("torch_tileview", "id", GravityBox.PACKAGE_NAME));

        IntentFilter intentFilter = new IntentFilter(TorchService.ACTION_TORCH_STATUS_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void updateTile() {
        if (mTorchStatus == TorchService.TORCH_STATUS_ON) {
            mDrawableId = mGbResources.getIdentifier("ic_qs_torch_on", "drawable", GravityBox.PACKAGE_NAME);
            mLabel = mGbResources.getString(
                    mGbResources.getIdentifier("quick_settings_torch_on", "string", GravityBox.PACKAGE_NAME));
        } else {
            mDrawableId = mGbResources.getIdentifier("ic_qs_torch_off", "drawable", GravityBox.PACKAGE_NAME);
            mLabel = mGbResources.getString(
                    mGbResources.getIdentifier("quick_settings_torch_off", "string", GravityBox.PACKAGE_NAME));            
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    private void toggleState() {
        Intent si = new Intent(mGbContext, TorchService.class);
        si.setAction(TorchService.ACTION_TOGGLE_TORCH);
        mGbContext.startService(si);
    }
}