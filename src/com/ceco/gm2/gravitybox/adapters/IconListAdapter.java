package com.ceco.gm2.gravitybox.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class IconListAdapter extends ArrayAdapter<IIconListAdapterItem> {
    private Context mContext;
    List<IIconListAdapterItem> mData = null;

    public IconListAdapter(Context context, List<IIconListAdapterItem> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);

        mContext = context;
        mData = new ArrayList<IIconListAdapterItem>(objects);
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

        IIconListAdapterItem item = mData.get(position);

        holder.text.setText(item.getText());
        holder.text.setCompoundDrawablesWithIntrinsicBounds(
                item.getIconLeft(), null, item.getIconRight(), null);

        return row;
    }
}