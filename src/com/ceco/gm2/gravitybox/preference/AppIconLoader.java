package com.ceco.gm2.gravitybox.preference;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.util.TypedValue;

public class AppIconLoader extends AsyncTask<ResolveInfo,Void,BitmapDrawable> {

    private Context mContext;
    private int mSizePx;
    private AppIconLoaderListener mListener;
    private static LruCache<String, BitmapDrawable> sAppIconCache;

    static {
        final int cacheSize = Math.min((int)Runtime.getRuntime().maxMemory() / 6, 4194304);
        sAppIconCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable d) {
                return d.getBitmap().getByteCount();
            }
        };
    }

    public interface AppIconLoaderListener {
        String getCachedIconKey();
        void onAppIconLoaded(Drawable icon);
    }

    public AppIconLoader(Context context, int size, AppIconLoaderListener listener) {
        mContext = context;
        if (mContext == null) {
            throw new IllegalArgumentException("GravityBox AppIconLoader: context cannot be null");
        }

        mSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, 
                mContext.getResources().getDisplayMetrics());

        mListener = listener;
        if (mListener == null) {
            throw new IllegalArgumentException("GravityBox AppIconLoader: listener cannot be null");
        }
    }

    public static Drawable getCachedIcon(String key) {
        return (key == null) ? null : sAppIconCache.get(key);
    }

    @Override
    protected BitmapDrawable doInBackground(ResolveInfo... params) {
        if (params[0] == null) return null;

        try {
            PackageManager pm = mContext.getPackageManager();
            Bitmap bitmap = ((BitmapDrawable)params[0].loadIcon(pm)).getBitmap();
            bitmap = Bitmap.createScaledBitmap(bitmap, mSizePx, mSizePx, true);
            BitmapDrawable appIcon = new BitmapDrawable(mContext.getResources(), bitmap);
            bitmap = null;
            return appIcon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(BitmapDrawable icon) {
        sAppIconCache.put(mListener.getCachedIconKey(), icon);
        mListener.onAppIconLoaded(icon);
    }
}
