package com.ceco.gm2.gravitybox;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.content.ContentResolver;

class NotificationWallpaper extends FrameLayout {

    private final String TAG = "NotificationWallpaperUpdater";

    private final String NOTIF_WALLPAPER_IMAGE_PATH = "/data/data/com.ceco.gm2.gravitybox/files/lockwallpaper";
    private final String NOTIF_WALLPAPER_IMAGE_PATH_LANDSCAPE = "";

    private ImageView mNotificationWallpaperImage;
    private float wallpaperAlpha;
    private int mCreationOrientation = 2;

    Context mContext;

    Bitmap bitmapWallpaper;

    public NotificationWallpaper(Context context) {
        super(context);
        mContext = context;
        setNotificationWallpaper();
    }

    public void setNotificationWallpaper() {
        boolean isLandscape = false;
        File file = new File(NOTIF_WALLPAPER_IMAGE_PATH);
        File fileLandscape = new File(NOTIF_WALLPAPER_IMAGE_PATH_LANDSCAPE);
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
            wallpaperAlpha = 0.7f;

            mNotificationWallpaperImage = new ImageView(getContext());
            if (isLandscape && !fileLandscape.exists()) {
                 mNotificationWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
            }else {
                 mNotificationWallpaperImage.setScaleType(ScaleType.CENTER);
            }
            addView(mNotificationWallpaperImage, -1, -1);
            if (isLandscape && fileLandscape.exists()) {
                bitmapWallpaper = BitmapFactory.decodeFile(NOTIF_WALLPAPER_IMAGE_PATH_LANDSCAPE);
            }else {
                bitmapWallpaper = BitmapFactory.decodeFile(NOTIF_WALLPAPER_IMAGE_PATH);
            }
            Drawable d = new BitmapDrawable(getResources(), bitmapWallpaper);
            d.setAlpha((int) ((1-wallpaperAlpha) * 255));
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
            setNotificationWallpaper();
    }
}

