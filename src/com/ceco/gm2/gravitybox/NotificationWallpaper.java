package com.ceco.gm2.gravitybox;

import java.io.File;

import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

class NotificationWallpaper extends FrameLayout {

    private final String TAG = "NotificationWallpaperUpdater";

    private ImageView mNotificationWallpaperImage;
    private String mNotifBgImagePathPortrait;
    private String mNotifBgImagePathLandscape;
    private float mAlpha;
    private boolean mEnabled;

    Context mContext;

    Bitmap bitmapWallpaper;

    public NotificationWallpaper(Context context) {
        super(context);
        mContext = context;

        try {
            Context gbContext = context.createPackageContext(GravityBox.PACKAGE_NAME, 0);
            mNotifBgImagePathPortrait = gbContext.getFilesDir() + "/notifwallpaper";
            mNotifBgImagePathLandscape = gbContext.getFilesDir() + "/notifwallpaper_landscape";
        } catch (NameNotFoundException e) {
            mNotifBgImagePathPortrait = "";
            mNotifBgImagePathLandscape = "";
            XposedBridge.log(e);
        }

        mAlpha = 0.6f;
        mEnabled = false;
    }

    public void setAlpha(float alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        mAlpha = alpha;
    }

    public void setAlpha(int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 100) alpha = 100;
        mAlpha = (float)alpha / (float)100;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public void updateNotificationWallpaper() {
        if (!mEnabled) {
            if (mNotificationWallpaperImage != null) {
                removeView(mNotificationWallpaperImage);
            }
            return;
        }

        boolean isLandscape = false;
        File file = new File(mNotifBgImagePathPortrait);
        File fileLandscape = new File(mNotifBgImagePathLandscape);
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
        switch(orientation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                isLandscape = false;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                isLandscape = true;
                break;
        }

        if (file.exists()) {
            if (mNotificationWallpaperImage != null) {
                removeView(mNotificationWallpaperImage);
            }

            mNotificationWallpaperImage = new ImageView(getContext());
            if (isLandscape && !fileLandscape.exists()) {
                 mNotificationWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
            }else {
                 mNotificationWallpaperImage.setScaleType(ScaleType.CENTER);
            }
            addView(mNotificationWallpaperImage, -1, -1);
            if (isLandscape && fileLandscape.exists()) {
                bitmapWallpaper = BitmapFactory.decodeFile(mNotifBgImagePathLandscape);
            }else {
                bitmapWallpaper = BitmapFactory.decodeFile(mNotifBgImagePathPortrait);
            }
            Drawable d = new BitmapDrawable(getResources(), bitmapWallpaper);
            d.setAlpha(mAlpha == 0 ? 255 : (int) ((1-mAlpha) * 255));
            mNotificationWallpaperImage.setImageDrawable(d);
        } else {
            if (mNotificationWallpaperImage != null) {
                removeView(mNotificationWallpaperImage);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (bitmapWallpaper != null)
            bitmapWallpaper.recycle();

        System.gc();
        super.onDetachedFromWindow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
            updateNotificationWallpaper();
    }
}

