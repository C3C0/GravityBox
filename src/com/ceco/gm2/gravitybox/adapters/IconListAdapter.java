package com.ceco.gm2.gravitybox.adapters;

import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.adapters.BaseListAdapterFilter.IBaseListAdapterFilterable;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class IconListAdapter extends ArrayAdapter<IIconListAdapterItem>
                             implements IBaseListAdapterFilterable<IIconListAdapterItem> {
    private Context mContext;
    private List<IIconListAdapterItem> mData = null;
    private List<IIconListAdapterItem> mFilteredData = null;
    private android.widget.Filter mFilter;

    public IconListAdapter(Context context, List<IIconListAdapterItem> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);

        mContext = context;
        mData = new ArrayList<IIconListAdapterItem>(objects);
        mFilteredData = new ArrayList<IIconListAdapterItem>(objects);
    }

    static class ViewHolder {
        TextView text;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = 
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);

            holder = new ViewHolder();
            holder.text = (TextView) row.findViewById(android.R.id.text1);
            holder.text.setCompoundDrawablePadding(10);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        IIconListAdapterItem item = mFilteredData.get(position);

        holder.text.setText(item.getText());
        holder.text.setCompoundDrawablesWithIntrinsicBounds(
                item.getIconLeft(), null, item.getIconRight(), null);

        return row;
    }

    @Override
    public android.widget.Filter getFilter() {
        if(mFilter == null)
            mFilter = new BaseListAdapterFilter<IIconListAdapterItem>(this);

        return mFilter;
    }

    @Override
    public List<IIconListAdapterItem> getOriginalData() {
        return mData;
    }

    @Override
    public List<IIconListAdapterItem> getFilteredData() {
        return mFilteredData;
    }

    @Override
    public void onFilterPublishResults(List<IIconListAdapterItem> results) {
        mFilteredData = results;
        clear();
        for (int i = 0; i < mFilteredData.size(); i++)
        {
            IIconListAdapterItem item = mFilteredData.get(i);
            add(item);
        }
    }
}