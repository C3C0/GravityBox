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

package com.ceco.gm2.gravitybox.adapters;

import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

public class IconCheckListAdapter extends ArrayAdapter<IIconCheckListAdapterItem> {
    private Context mContext;
    private List<IIconCheckListAdapterItem> mData = null;
    private boolean mSubtextEnabled;

    public IconCheckListAdapter(Context context, List<IIconCheckListAdapterItem> objects) {
        super(context, R.layout.simple_list_item_2_multiple_choice, objects);

        mContext = context;
        mData = new ArrayList<IIconCheckListAdapterItem>(objects);
        mSubtextEnabled = true;
    }

    static class ViewHolder {
        CheckedTextView text;
        TextView subText;
    }

    public void setSubtextEnabled(boolean enabled) {
        mSubtextEnabled = enabled;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = 
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.simple_list_item_2_multiple_choice, parent, false);

            holder = new ViewHolder();
            holder.text = (CheckedTextView) row.findViewById(R.id.text1);
            holder.text.setCompoundDrawablePadding(10);
            holder.subText = (TextView) row.findViewById(R.id.text2);
            if (!mSubtextEnabled) {
                holder.subText.setVisibility(View.GONE);
            }
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        IIconCheckListAdapterItem item = mData.get(position);

        holder.text.setText(item.getText());
        holder.text.setCompoundDrawablesWithIntrinsicBounds(
                item.getIconLeft(), null, item.getIconRight(), null);
        holder.text.setChecked(item.isChecked());
        holder.subText.setText(item.getSubText());

        return row;
    }
}
