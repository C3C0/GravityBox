package com.ceco.gm2.gravitybox.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.Filter;

public class BaseListAdapterFilter<T extends IBaseListAdapterItem> extends Filter {
    private IBaseListAdapterFilterable<T> mTarget;

    public interface IBaseListAdapterFilterable<T> {
        public List<T> getOriginalData();
        public List<T> getFilteredData();
        public void onFilterPublishResults(List<T> results);
    }

    public BaseListAdapterFilter(IBaseListAdapterFilterable<T> target) {
        mTarget = target;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        String search = constraint.toString().toLowerCase(Locale.getDefault());
        ArrayList<T> original = new ArrayList<T>(mTarget.getOriginalData());

        if(search == null || search.length() == 0) {
            results.values = original;
            results.count = original.size();
        } else {
            final ArrayList<T> nlist = new ArrayList<T>();
            for (int i = 0; i < original.size(); i++) {
                final T item = original.get(i);
                final String val = item.getText().toLowerCase(Locale.getDefault());

                if(val.contains(search))
                    nlist.add(item);
            }

            results.values = nlist;
            results.count = nlist.size();
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        mTarget.onFilterPublishResults((ArrayList<T>) results.values);
    }
}
