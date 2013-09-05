package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedHelpers;

import android.content.Context;
import android.media.AudioManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class VolumeTile extends AQuickSettingsTile {

    public VolumeTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels");
                AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startActivity(android.provider.Settings.ACTION_SOUND_SETTINGS);
                return true;
            }
        };
    }

    @Override
    protected void onTileCreate() {
        mDrawableId = R.drawable.ic_qs_volume;
        mLabel = mGbContext.getString(R.string.qs_tile_volume);

        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_volume, mTile);
    }

    @Override
    protected synchronized void updateTile() {
        TextView tv = (TextView) mTile.findViewById(R.id.volume_tileview);
        tv.setText(mLabel);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}