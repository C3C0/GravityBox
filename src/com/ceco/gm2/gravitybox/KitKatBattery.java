/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox;

import com.ceco.gm2.gravitybox.StatusBarIconManager.ColorInfo;
import com.ceco.gm2.gravitybox.StatusBarIconManager.IconManagerListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class KitKatBattery extends View implements IconManagerListener {
    public static final String TAG = "GB:KitKatBattery";

    public static final boolean SINGLE_DIGIT_PERCENT = false;
    public static final boolean SHOW_100_PERCENT = false;

    public static final int[] LEVELS = new int[] { 4, 15, 100 };
    public static final int[] COLORS = new int[] { 0xFFFF3300, 0xFFFF3300, 0xFFFFFFFF };
    public static final int BOLT_COLOR = 0xB2000000;
    public static final int FULL = 96;
    public static final int EMPTY = 4;

    public static final float SUBPIXEL = 0.4f;  // inset rects for softer edges

    private int[] mColors;

    private boolean mShowPercent = true;
    private Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextPaint, mBoltPaint;
    private int mButtonHeight;
    private float mTextHeight, mWarningTextHeight;

    private int mHeight;
    private int mWidth;
    private String mWarningString;
    private int mChargeColor;
    private final float[] mBoltPoints;
    private final Path mBoltPath = new Path();

    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mClipFrame = new RectF();
    private final Rect mBoltFrame = new Rect();

    private class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        int level = UNKNOWN_LEVEL;
        int plugType;
        boolean plugged;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                level = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));

                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                plugged = plugType != 0;

                postInvalidate();
            }
        }
    }

    BatteryTracker mTracker = new BatteryTracker();

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        final Intent sticky = getContext().registerReceiver(mTracker, filter);
        if (sticky != null) {
            // preload the battery level
            mTracker.onReceive(getContext(), sticky);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mTracker);
    }

    public KitKatBattery(Context context) {
        this(context, null, 0);
    }

    public KitKatBattery(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KitKatBattery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mWarningString = "!";

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setDither(true);
        mFramePaint.setStrokeWidth(0);
        mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

        mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setStrokeWidth(0);
        mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        mTextPaint.setTypeface(font);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        font = Typeface.create("sans-serif", Typeface.BOLD);
        mWarningTextPaint.setTypeface(font);
        mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

        mBoltPaint = new Paint();
        mBoltPaint.setAntiAlias(true);
        mBoltPoints = loadBoltPoints();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setColor(Build.VERSION.SDK_INT > 18 ? Color.WHITE :
                context.getResources().getColor(android.R.color.holo_blue_dark));
    }

    public void setColor(int mainColor) {
        COLORS[COLORS.length-1] = mainColor;

        final int N = LEVELS.length;
        mColors = new int[2*N];
        for (int i=0; i<N; i++) {
            mColors[2*i] = LEVELS[i];
            mColors[2*i+1] = COLORS[i];
        }

        mFramePaint.setColor(mainColor);
        mFramePaint.setAlpha(102);
        mTextPaint.setColor(Color.BLACK);
        mWarningTextPaint.setColor(COLORS[0]);
        mBoltPaint.setColor(BOLT_COLOR);
        mChargeColor = mainColor;
        invalidate();
    }

    public void setShowPercent(boolean show) {
        mShowPercent = show;
        invalidate();
    }

    private static float[] loadBoltPoints() {
        final int[] pts = new int[] { 73,0,392,0,201,259,442,259,4,703,157,334,0,334 };
        int maxX = 0, maxY = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        final float[] ptsF = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            ptsF[i] = (float)pts[i] / maxX;
            ptsF[i + 1] = (float)pts[i + 1] / maxY;
        }
        return ptsF;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        mWarningTextPaint.setTextSize(h * 0.75f);
        mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
    }

    private int getColorForLevel(int percent) {
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            if (percent <= thresh) return color;
        }
        return color;
    }

    @Override
    public void draw(Canvas c) {
        BatteryTracker tracker = mTracker;
        final int level = tracker.level;

        if (level == BatteryTracker.UNKNOWN_LEVEL) return;

        float drawFrac = (float) level / 100f;
        final int pt = getPaddingTop();
        final int pl = getPaddingLeft();
        final int pr = getPaddingRight();
        final int pb = getPaddingBottom();
        int height = mHeight - pt - pb;
        int width = mWidth - pl - pr;

        mButtonHeight = (int) (height * 0.12f);

        mFrame.set(0, 0, width, height);
        mFrame.offset(pl, pt);

        mButtonFrame.set(
                mFrame.left + width * 0.25f,
                mFrame.top,
                mFrame.right - width * 0.25f,
                mFrame.top + mButtonHeight + 5 /*cover frame border of intersecting area*/);

        mButtonFrame.top += SUBPIXEL;
        mButtonFrame.left += SUBPIXEL;
        mButtonFrame.right -= SUBPIXEL;

        mFrame.top += mButtonHeight;
        mFrame.left += SUBPIXEL;
        mFrame.top += SUBPIXEL;
        mFrame.right -= SUBPIXEL;
        mFrame.bottom -= SUBPIXEL;

        // first, draw the battery shape
        c.drawRect(mFrame, mFramePaint);

        // fill 'er up
        final int color = tracker.plugged ? mChargeColor : getColorForLevel(level);
        mBatteryPaint.setColor(color);

        if (level >= FULL) {
            drawFrac = 1f;
        } else if (level <= EMPTY) {
            drawFrac = 0f;
        }

        c.drawRect(mButtonFrame, drawFrac == 1f ? mBatteryPaint : mFramePaint);

        mClipFrame.set(mFrame);
        mClipFrame.top += (mFrame.height() * (1f - drawFrac));

        c.save(Canvas.CLIP_SAVE_FLAG);
        c.clipRect(mClipFrame);
        c.drawRect(mFrame, mBatteryPaint);
        c.restore();

        if (tracker.plugged) {
            // draw the bolt
            final int bl = (int)(mFrame.left + mFrame.width() / 4.5f);
            final int bt = (int)(mFrame.top + mFrame.height() / 6f);
            final int br = (int)(mFrame.right - mFrame.width() / 7f);
            final int bb = (int)(mFrame.bottom - mFrame.height() / 10f);
            if (mBoltFrame.left != bl || mBoltFrame.top != bt
                    || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                mBoltFrame.set(bl, bt, br, bb);
                mBoltPath.reset();
                mBoltPath.moveTo(
                        mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                        mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                for (int i = 2; i < mBoltPoints.length; i += 2) {
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                }
                mBoltPath.lineTo(
                        mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                        mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
            }
            c.drawPath(mBoltPath, mBoltPaint);
        } else if (level <= EMPTY) {
            final float x = mWidth * 0.5f;
            final float y = (mHeight + mWarningTextHeight) * 0.48f;
            c.drawText(mWarningString, x, y, mWarningTextPaint);
        } else if (mShowPercent && !(tracker.level == 100 && !SHOW_100_PERCENT)) {
            mTextPaint.setTextSize(height *
                    (SINGLE_DIGIT_PERCENT ? 0.75f
                            : (tracker.level == 100 ? 0.38f : 0.5f)));
            mTextPaint.setColor(level < 33 ? Color.WHITE : Color.BLACK);
            mTextHeight = -mTextPaint.getFontMetrics().ascent;

            final String str = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
            final float x = mWidth * 0.5f;
            final float y = (mHeight + mTextHeight) * 0.47f;
            c.drawText(str,
                    x,
                    y,
                    mTextPaint);
        }
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            setColor(colorInfo.coloringEnabled ?
                    colorInfo.iconColor[0] : colorInfo.defaultIconColor);
        }
    }
}
