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

package com.ceco.gm2.gravitybox.quicksettings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ceco.gm2.gravitybox.GravityBoxSettings;
import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.TouchInterceptor;
import com.ceco.gm2.gravitybox.Utils;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class TileOrderActivity extends ListActivity {
    public static final String PREF_KEY_TILE_ORDER = "pref_qs_tile_order";

    private ListView mTileList;
    private TileAdapter mTileAdapter;
    private Context mContext;
    private Resources mResources;
    private SharedPreferences mPrefs;
    private Map<String, String> mTileTexts;
    private int mTextAppearanceResId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        File file = new File(getFilesDir() + "/" + GravityBoxSettings.FILE_THEME_DARK_FLAG);
        mTextAppearanceResId = android.R.style.TextAppearance_Holo_Medium_Inverse;
        if (file.exists()) {
            this.setTheme(android.R.style.Theme_Holo);
            mTextAppearanceResId = android.R.style.TextAppearance_Holo_Medium;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_tile_list_activity);

        mContext = getApplicationContext();
        mResources = mContext.getResources();
        final String prefsName = mContext.getPackageName() + "_preferences";
        mPrefs = mContext.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);

        mTileList = getListView();
        ((TouchInterceptor) mTileList).setDropListener(mDropListener);
        mTileAdapter = new TileAdapter(mContext);
        setListAdapter(mTileAdapter);

        String[] allTileKeys = Utils.isMtkDevice() ? 
                mResources.getStringArray(R.array.qs_tile_values) :
                    mResources.getStringArray(Build.VERSION.SDK_INT > 18 ?
                            R.array.qs_tile_aosp_values_kk : R.array.qs_tile_aosp_values);
        String[] allTileNames = Utils.isMtkDevice() ?
                mResources.getStringArray(R.array.qs_tile_entries) :
                    mResources.getStringArray(Build.VERSION.SDK_INT > 18 ? 
                            R.array.qs_tile_aosp_entries_kk : R.array.qs_tile_aosp_entries);
        mTileTexts = new HashMap<String, String>();
        for (int i = 0; i < allTileKeys.length; i++) {
            mTileTexts.put(allTileKeys[i], allTileNames[i]);
        }

        if (mPrefs.getString(PREF_KEY_TILE_ORDER, null) == null) {
            mPrefs.edit().putString(PREF_KEY_TILE_ORDER, Utils.join(allTileKeys, ",")).commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateTileList(mPrefs);
    }

    @Override
    public void onDestroy() {
        ((TouchInterceptor) mTileList).setDropListener(null);
        setListAdapter(null);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // reload our tiles and invalidate the views for redraw
        mTileAdapter.reloadTiles();
        mTileList.invalidateViews();
    }

    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
            // get the current tile list
            List<String> tiles = getOrderedTileList();

            // move the tile
            if (from < tiles.size()) {
                String tile = tiles.remove(from);

                if (to <= tiles.size()) {
                    tiles.add(to, tile);

                    // save our tiles
                    setCurrentTileKeys(tiles);

                    // tell our adapter/listview to reload
                    mTileAdapter.reloadTiles();
                    mTileList.invalidateViews();
                }
            }
        }
    };

    public static String updateTileList(SharedPreferences prefs) {
        List<String> activeTileList = new ArrayList<String>(
                prefs.getStringSet(GravityBoxSettings.PREF_KEY_QUICK_SETTINGS, 
                        new HashSet<String>()));
        String tiles = prefs.getString(PREF_KEY_TILE_ORDER, "");
        List<String> orderedTileList = 
                new ArrayList<String>(Arrays.asList(tiles.split(",")));
 
        // remove those missing in active tile list
        for (int i = orderedTileList.size() - 1; i >= 0; i--) {
            if (!activeTileList.contains(orderedTileList.get(i))) {
                orderedTileList.remove(i);
            }
        }

        // add those missing in ordered tile list
        for (int i = 0; i < activeTileList.size(); i++) {
            if (!orderedTileList.contains(activeTileList.get(i))) {
                orderedTileList.add(activeTileList.get(i));
            }
        }

        // save new ordered tile list
        String[] newList = new String[orderedTileList.size()];
        newList = orderedTileList.toArray(newList);
        final String value = Utils.join(newList, ",");
        prefs.edit().putString(PREF_KEY_TILE_ORDER, value).commit();
        return value;
    } 

    private List<String> getOrderedTileList() {
        String tiles = mPrefs.getString(PREF_KEY_TILE_ORDER, "");
        return new ArrayList<String>(Arrays.asList(tiles.split(",")));
    }

    private void setCurrentTileKeys(List<String> list) {
        String[] newList = new String[list.size()];
        newList = list.toArray(newList);
        final String value = Utils.join(newList, ",");
        mPrefs.edit().putString(PREF_KEY_TILE_ORDER, value).commit();
        Intent intent = new Intent(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED);
        intent.putExtra(GravityBoxSettings.EXTRA_QS_PREFS, value);
        mContext.sendBroadcast(intent);
    }

    private class TileAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        private List<String> mTiles;

        public TileAdapter(Context c) {
            mContext = c;
            mInflater = LayoutInflater.from(mContext);

            reloadTiles();
        }

        public void reloadTiles() {
            mTiles = getOrderedTileList();
        }

        public int getCount() {
            return mTiles.size();
        }

        public Object getItem(int position) {
            return mTiles.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final View v;
            if (convertView == null) {
                v = mInflater.inflate(R.layout.order_tile_list_item, null);
            } else {
                v = convertView;
            }

            String tileKey = mTiles.get(position);

            final TextView name = (TextView) v.findViewById(R.id.name);
            name.setTextAppearance(mContext, mTextAppearanceResId);
            final ImageView icon = (ImageView) v.findViewById(R.id.icon);

            name.setText(mTileTexts.get(tileKey));

            // no icon
            icon.setVisibility(View.GONE);

            return v;
        }
    }
}
