/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.gm2.gravitybox.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.gm2.gravitybox.adapters.IconListAdapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

public class AppPickerPreference extends DialogPreference implements OnItemClickListener {
    public static final String SEPARATOR = "#C3C0#";

    private Context mContext;
    private ListView mListView;
    private ArrayList<IIconListAdapterItem> mListData;
    private EditText mSearch;
    private ProgressBar mProgressBar;
    private AsyncTask<Void,Void,Void> mAsyncTask;
    private String mDefaultSummaryText;
    private int mAppIconSizePx;
    private PackageManager mPackageManager;
    private Resources mResources;

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

    public AppPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mResources = mContext.getResources();
        mDefaultSummaryText = (String) getSummary();
        mAppIconSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, 
                mResources.getDisplayMetrics());
        mPackageManager = mContext.getPackageManager();

        setDialogLayoutResource(R.layout.app_picker_preference);
        setPositiveButtonText(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        mListView = (ListView) view.findViewById(R.id.icon_list);
        mListView.setOnItemClickListener(this);

        mSearch = (EditText) view.findViewById(R.id.input_search);
        mSearch.setVisibility(View.GONE);
        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) { }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) { }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                    int arg3) 
            {
                if(mListView.getAdapter() == null)
                    return;
                
                ((IconListAdapter)mListView.getAdapter()).getFilter().filter(arg0);
            }
        });

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        super.onBindView(view);

        setData();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAsyncTask.cancel(true);
            mAsyncTask = null;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            String value = getPersistedString(null);
            String appName = getAppNameFromValue(value);
            setSummary(appName == null ? mDefaultSummaryText : appName);
        } else {
            setValue(null);
            setSummary(mDefaultSummaryText);
        }
    } 

    public void setDefaultSummary(String summary) {
        mDefaultSummaryText = summary;
    }

    private void setData() {
        mAsyncTask = new AsyncTask<Void,Void,Void>() {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();

                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.refreshDrawableState();
                mListData = new ArrayList<IIconListAdapterItem>();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                List<ResolveInfo> appList = new ArrayList<ResolveInfo>();

                List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);
                Intent mainIntent = new Intent(Intent.ACTION_MAIN);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                for(PackageInfo pi : packages) {
                    if (this.isCancelled()) break;
                    mainIntent.setPackage(pi.packageName);
                    List<ResolveInfo> activityList = mPackageManager.queryIntentActivities(mainIntent, 0);
                    for(ResolveInfo ri : activityList) {
                        appList.add(ri);
                    }
                }

                Collections.sort(appList, new ResolveInfo.DisplayNameComparator(mPackageManager));
                mListData.add(new AppItem(mContext.getString(R.string.app_picker_none), null));
                for (ResolveInfo ri : appList) {
                    if (this.isCancelled()) break;
                    String appName = ri.loadLabel(mPackageManager).toString();
                    AppItem ai = new AppItem(appName, ri);
                    mListData.add(ai);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                mProgressBar.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
                mListView.setAdapter(new IconListAdapter(mContext, mListData));
                ((IconListAdapter)mListView.getAdapter()).notifyDataSetChanged();
            }
        }.execute();
    }

    private void setValue(String value){
        persistString(value);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppItem item = (AppItem) parent.getItemAtPosition(position);
        setValue(item.getValue());
        setSummary(item.getValue() == null ? mDefaultSummaryText : item.getAppName());
        getDialog().dismiss();
    }

    private String getAppNameFromValue(String value) {
        if (value == null) return null;

        try {
            String[] splitValue = value.split(SEPARATOR);
            ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
            ActivityInfo ai = mPackageManager.getActivityInfo(cn, 0);
            return (ai.loadLabel(mPackageManager).toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    class AppItem implements IIconListAdapterItem {
        private String mPackageName;
        private String mClassName;
        private String mAppName;
        private BitmapDrawable mAppIcon;
        private ResolveInfo mResolveInfo;

        public AppItem(String appName, ResolveInfo ri) {
            mAppName = appName;
            mResolveInfo = ri;
            if (mResolveInfo != null) {
                mPackageName = mResolveInfo.activityInfo.packageName;
                mClassName = mResolveInfo.activityInfo.name;
            }
        }

        public String getPackageName() {
            return mPackageName;
        }

        public String getClassName() {
            return mClassName;
        }

        public String getAppName() {
            return mAppName;
        }

        public String getValue() {
            if (mPackageName == null || mClassName == null) return null;

            return String.format("%1$s%2$s%3$s", mPackageName, SEPARATOR, mClassName);
        }

        @Override
        public String getText() {
            return mAppName;
        }

        @Override
        public String getSubText() {
            return null;
        }

        @Override
        public Drawable getIconLeft() {
            if (mResolveInfo == null) return null;

            if (mAppIcon == null) {
                final String key = getValue();
                mAppIcon = sAppIconCache.get(key);
                if (mAppIcon == null) {
                    Bitmap bitmap = ((BitmapDrawable)mResolveInfo.loadIcon(mPackageManager)).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(bitmap, mAppIconSizePx, mAppIconSizePx, false);
                    mAppIcon = new BitmapDrawable(mResources, bitmap);
                    sAppIconCache.put(key, mAppIcon);
                }
            }
            return mAppIcon;
        }

        @Override
        public Drawable getIconRight() {
            return null;
        }
    }
}
