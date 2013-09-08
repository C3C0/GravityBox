package com.ceco.gm2.gravitybox.preference;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.TypedValue;

public class AppIconLoader extends AsyncTask<ResolveInfo,Void,Drawable> {

    private Context mContext;
    private int mSizePx;
    private AppIconLoaderListener mListener;
    private static Map<String, SoftReference<Drawable>> sAppIconCache = 
            new HashMap<String, SoftReference<Drawable>>();

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

    public static boolean isCachedIcon(String key) {
        return (sAppIconCache.containsKey(key) && sAppIconCache.get(key).get() != null);
    }

    public static Drawable getCachedIcon(String key) {
        return sAppIconCache.get(key).get();
    }

    @Override
    protected Drawable doInBackground(ResolveInfo... params) {
        if (params[0] == null) return null;

        try {
            PackageManager pm = mContext.getPackageManager();
            Bitmap bitmap = ((BitmapDrawable)params[0].loadIcon(pm)).getBitmap();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, mSizePx, mSizePx, true);
            Drawable appIcon = new BitmapDrawable(mContext.getResources(), scaledBitmap);
            sAppIconCache.put(mListener.getCachedIconKey(), new SoftReference<Drawable>(appIcon));
            return appIcon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Drawable icon) {
        mListener.onAppIconLoaded(icon);
    }
}
