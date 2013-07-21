package com.ceco.gm2.gravitybox.preference;

import java.util.ArrayList;
import java.util.List;

import com.ceco.gm2.gravitybox.GravityBoxResultReceiver;
import com.ceco.gm2.gravitybox.GravityBoxResultReceiver.Receiver;
import com.ceco.gm2.gravitybox.ModDisplay;
import com.ceco.gm2.gravitybox.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class AutoBrightnessDialogPreference extends DialogPreference 
            implements Receiver, OnItemSelectedListener, OnClickListener {

    private Spinner mSpinLevels;
    private ArrayAdapter<String> mSpinLevelsAdapter;
    private GravityBoxResultReceiver mReceiver;
    private int[] mLuxArray;
    private int[] mBrightnessArray;
    private EditText mTxtLux;
    private EditText mTxtBrightness;
    private Button mBtnSet;

    public AutoBrightnessDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dlgpref_autobrightness);

        mReceiver = new GravityBoxResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    protected void onBindDialogView(View view) {
        mSpinLevels = (Spinner) view.findViewById(R.id.spinLevels);
        mSpinLevels.setOnItemSelectedListener(this);

        mTxtLux = (EditText) view.findViewById(R.id.txtLux);
        mTxtBrightness = (EditText) view.findViewById(R.id.txtBrightness);

        mBtnSet = (Button) view.findViewById(R.id.btnSet);
        mBtnSet.setOnClickListener(this);

        if (getConfig()) {
            setData();
        }

        super.onBindDialogView(view);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) { } 

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            saveConfig();
            Intent intent = new Intent();
            intent.setAction(ModDisplay.ACTION_SET_AUTOBRIGHTNESS_CONFIG);
            intent.putExtra("config_autoBrightnessLevels", mLuxArray);
            intent.putExtra("config_autoBrightnessLcdBacklightValues", mBrightnessArray);
            getContext().sendBroadcast(intent);
            Toast.makeText(getContext(), R.string.pref_ab_config_saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.pref_ab_config_cancelled, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == ModDisplay.RESULT_AUTOBRIGHTNESS_CONFIG) {
            mLuxArray = resultData.getIntArray("config_autoBrightnessLevels");
            mBrightnessArray = resultData.getIntArray("config_autoBrightnessLcdBacklightValues");
            saveConfig();
            setData();
        }
    }

    private boolean getConfig() {
        String value = getPersistedString(null);
        if (value == null) {
            getSystemConfig();
            return false;
        }

        String[] luxArray = value.split("\\|")[0].split(",");
        String[] brightnessArray = value.split("\\|")[1].split(",");
        if (luxArray.length == 0 || brightnessArray.length == 0) {
            getSystemConfig();
            return false;
        }

        mLuxArray = new int[luxArray.length];
        int index = 0;
        for(String s : luxArray) {
            mLuxArray[index++] = Integer.valueOf(s);
        }

        mBrightnessArray = new int[brightnessArray.length];
        index = 0;
        for(String s : brightnessArray) {
            mBrightnessArray[index++] = Integer.valueOf(s);
        }

        return true;
    }

    private void getSystemConfig() {
        Intent intent = new Intent();
        intent.setAction(ModDisplay.ACTION_GET_AUTOBRIGHTNESS_CONFIG);
        intent.putExtra("receiver", mReceiver);
        getContext().sendBroadcast(intent);
    }

    private void setData() {
        List<String> items = new ArrayList<String>();
        String level = getContext().getString(R.string.level);

        for(int i = 1; i <= mLuxArray.length; i++) {
            items.add(level + " " + i);
        }
        mSpinLevelsAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, items);
        mSpinLevelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (mSpinLevels != null) {
            mSpinLevels.setAdapter(mSpinLevelsAdapter);
        }
    }

    private void saveConfig() {
        String value = "";
        for(int val : mLuxArray) {
            if (!value.isEmpty()) value += ",";
            value += String.valueOf(val);
        }
        value += "|";
        for(int val : mBrightnessArray) {
            if (!value.endsWith("|")) value += ",";
            value += String.valueOf(val);
        }

        persistString(value);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mTxtLux.setText(String.valueOf(mLuxArray[position]));
        mTxtBrightness.setText(String.valueOf(mBrightnessArray[position]));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mTxtLux.setText("");
        mTxtBrightness.setText("");
    }

    @Override
    public void onClick(View view) {
        int lux = -1;
        int brightness = -1;
        int position = mSpinLevels.getSelectedItemPosition();
        
        try {
            lux = Integer.valueOf(mTxtLux.getText().toString());
            brightness = Integer.valueOf(mTxtBrightness.getText().toString());
            if (lux < 0) {
                Toast.makeText(getContext(), R.string.pref_ab_number_error_negative, Toast.LENGTH_LONG).show();
                return;
            } else if (brightness < 20) {
                Toast.makeText(getContext(), R.string.pref_ab_brightness_too_low, Toast.LENGTH_LONG).show();
                return;
            } else if (brightness > 255) {
                Toast.makeText(getContext(), R.string.pref_ab_brightness_too_high, Toast.LENGTH_LONG).show();
                return;
            }

            boolean ascendingViolation = false;
            boolean descendingViolation = false;
            for (int i = position + 1; i < mLuxArray.length; i++) {
                ascendingViolation |= (lux > mLuxArray[i]);
            }
            for (int i = position + 1; i < mBrightnessArray.length; i++) {
                ascendingViolation |= (brightness > mBrightnessArray[i]);
            }
            for (int i = position - 1; i >= 0; i--) {
                descendingViolation |= (lux < mLuxArray[i]);
                descendingViolation |= (brightness < mBrightnessArray[i]);
            }
            if (ascendingViolation) {
                Toast.makeText(getContext(), R.string.pref_ab_number_not_ascending, Toast.LENGTH_LONG).show();
                return;
            }
            if (descendingViolation) {
                Toast.makeText(getContext(), R.string.pref_ab_number_not_descending, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) { 
            Toast.makeText(getContext(), R.string.pref_ab_number_error_general, Toast.LENGTH_LONG).show();
            return;
        }

        mLuxArray[position] = lux;
        mBrightnessArray[position] = brightness;
        Toast.makeText(getContext(), 
                String.format(getContext().getString(R.string.pref_ab_values_set), 
                        mSpinLevels.getSelectedItem().toString()), Toast.LENGTH_SHORT).show();
    }
}