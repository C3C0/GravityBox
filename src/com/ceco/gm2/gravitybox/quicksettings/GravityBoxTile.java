package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBox;
import com.ceco.gm2.gravitybox.GravityBoxSettings;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class GravityBoxTile extends AQuickSettingsTile {

    public GravityBoxTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(GravityBox.PACKAGE_NAME, GravityBoxSettings.class.getName());
                startActivity(i);
            }
        };
    }

    @Override
    protected void onTileCreate() {
        mDrawableId = mGbResources.getIdentifier("ic_launcher", "drawable", GravityBox.PACKAGE_NAME);
        mLabel = "GravityBox";

        int mTileLayoutId = mGbResources.getIdentifier("quick_settings_tile_gravity", "layout", GravityBox.PACKAGE_NAME);
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(mTileLayoutId, mTile);
    }

    @Override
    protected synchronized void updateTile() {
        TextView tv = (TextView) mTile.findViewById(
                mGbResources.getIdentifier("gravitybox_tileview", "id", GravityBox.PACKAGE_NAME));
        tv.setText(mLabel);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }
}