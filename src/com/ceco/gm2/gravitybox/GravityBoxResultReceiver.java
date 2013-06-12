package com.ceco.gm2.gravitybox;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class GravityBoxResultReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }
        
    public GravityBoxResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }     
    } 
}