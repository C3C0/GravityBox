package com.ceco.gm2.gravitybox;

import java.io.File;

import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

class NotificationWallpaper extends FrameLayout {

    private static final String TAG = "NotificationWallpaper";

    private ImageView mNotificationWallpaperImage;
    private String mNotifBgImagePathPortrait;
    private String mNotifBgImagePathLandscape;
    private String mBgType;
    private int mColor;
    private String mColorMode;
    private float mAlpha;
    private Context mContext;
    Bitmap mBitmapWallpaper = null;

    public NotificationWallpaper(Context context) {
        super(context);
        mContext = context;

        try {
            Context gbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, 0);
            mNotifBgImagePathPortrait = gbContext.getFilesDir() + "/notifwallpaper";
            mNotifBgImagePathLandscape = gbContext.getFilesDir() + "/notifwallpaper_landscape";
        } catch (NameNotFoundException e) {
            mNotifBgImagePathPortrait = "";
            mNotifBgImagePathLandscape = "";
            XposedBridge.log(e);
        }

        mBgType = GravityBoxSettings.NOTIF_BG_DEFAULT;
        mColorMode = GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY;
        mAlpha = 0.6f;
    }

    public String getType() {
        return mBgType;
    }

    public void setType(String type) {
        mBgType = type;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color; 
    }

    public String getColorMode() {
        return mColorMode;
    }

    public void setColorMode(String colorMode) {
        mColorMode = colorMode;
    }

    public float getAlpha() {
        return mAlpha;
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

    public void updateNotificationWallpaper() {
        if (mNotificationWallpaperImage != null) {
            removeView(mNotificationWallpaperImage);
        }

        if (mBgType.equals(GravityBoxSettings.NOTIF_BG_DEFAULT)) return;

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

        mNotificationWallpaperImage = new ImageView(getContext());

        Drawable d = null;
        if (mBgType.equals(GravityBoxSettings.NOTIF_BG_IMAGE) && file.exists()) {
            if (isLandscape && !fileLandscape.exists()) {
                mNotificationWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
            } else {
                mNotificationWallpaperImage.setScaleType(ScaleType.CENTER);
            }
            if (isLandscape && fileLandscape.exists()) {
                mBitmapWallpaper = BitmapFactory.decodeFile(mNotifBgImagePathLandscape);
            } else {
                mBitmapWallpaper = BitmapFactory.decodeFile(mNotifBgImagePathPortrait);
            }
            if (mBitmapWallpaper != null) {
                d = new BitmapDrawable(getResources(), mBitmapWallpaper);
            }
        } else if (mBgType.equals(GravityBoxSettings.NOTIF_BG_COLOR)) {
            if (mColorMode.equals(GravityBoxSettings.NOTIF_BG_COLOR_MODE_UNDERLAY)) {
                ViewParent parent = getParent();
                if (parent != null && parent instanceof FrameLayout) {
                    Drawable pd = ((FrameLayout)parent).getBackground(); 
                    if (pd != null) {
                        pd.setColorFilter(mColor, PorterDuff.Mode.SRC_ATOP);
                        pd.setAlpha(mAlpha == 0 ? 255 : (int) ((1-mAlpha) * 255));
                    }
                }
            } else if (mColorMode.equals(GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY)) {
                d = new ColorDrawable();
                ((ColorDrawable)d).setColor(mColor);
            }
        }

        if (d != null) {
            d.setAlpha(mAlpha == 0 ? 255 : (int) ((1-mAlpha) * 255));
            mNotificationWallpaperImage.setImageDrawable(d);
            addView(mNotificationWallpaperImage, -1, -1);
        } else {
            mNotificationWallpaperImage = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mBitmapWallpaper != null)
            mBitmapWallpaper.recycle();

        System.gc();
        super.onDetachedFromWindow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
            updateNotificationWallpaper();
    }
}

