package com.ceco.gm2.gravitybox.adapters;

public class BasicListItem implements IBaseListAdapterItem {
    private String mText;
    private String mSubText;

    public BasicListItem(String text, String subText) {
        mText = text;
        mSubText = subText;
    }

    @Override
    public String getText() {
        return mText;
    }

    @Override
    public String getSubText() {
        return mSubText;
    }

    public void setText(String text) {
        mText = text;           
    }

    public void setSubText(String text) {
        mSubText = text;
    }
}