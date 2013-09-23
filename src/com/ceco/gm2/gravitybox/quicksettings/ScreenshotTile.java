package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.ModHwKeys;
import com.ceco.gm2.gravitybox.R;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ScreenshotTile extends AQuickSettingsTile {

    private TextView mTextView;

    public ScreenshotTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsePanels();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(ModHwKeys.ACTION_SCREENSHOT);
                        mContext.sendBroadcast(intent);
                    }
                }, 1000);
            }
        };
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_screenshot, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.screenshot_tileview);
    }

    @Override
    protected synchronized void updateTile() {
        mDrawableId = R.drawable.ic_qs_screenshot;
        mLabel = mGbContext.getString(R.string.qs_tile_screenshot);
        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}
