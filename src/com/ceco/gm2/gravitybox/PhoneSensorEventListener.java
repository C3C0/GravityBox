package com.ceco.gm2.gravitybox;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class PhoneSensorEventListener implements SensorEventListener {
    // Our accelerometers are not quite accurate.
    private static final int FACE_UP_GRAVITY_THRESHOLD = 7;
    private static final int FACE_DOWN_GRAVITY_THRESHOLD = -7;
    private static final int SENSOR_SAMPLES = 3;
    private static final int MIN_ACCEPT_COUNT = SENSOR_SAMPLES - 1;

    private boolean mStopped;
    private boolean mWasFaceUp;
    private boolean[] mSamples = new boolean[SENSOR_SAMPLES];
    private int mSampleIndex;
    private ActionHandler mActionHandler;

    public interface ActionHandler {
        void onFaceDown();
        void onFaceUp();
    }

    public PhoneSensorEventListener(ActionHandler actionHandler) {
        mActionHandler = actionHandler;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int acc) {
    }

    public void reset() {
        mWasFaceUp = false;
        mStopped = false;
        for (int i = 0; i < SENSOR_SAMPLES; i++) {
            mSamples[i] = false;
        }
    }

    private boolean filterSamples() {
        int trues = 0;
        for (boolean sample : mSamples) {
            if(sample) {
                ++trues;
            }
        }
        return trues >= MIN_ACCEPT_COUNT;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Add a sample overwriting the oldest one. Several samples
        // are used to avoid the erroneous values the sensor sometimes
        // returns.
        float z = event.values[2];

        if (mStopped) {
            return;
        }

        if (!mWasFaceUp) {
            // Check if its face up enough.
            mSamples[mSampleIndex] = z > FACE_UP_GRAVITY_THRESHOLD;

            // face up
            if (filterSamples()) {
                mWasFaceUp = true;
                for (int i = 0; i < SENSOR_SAMPLES; i++) {
                    mSamples[i] = false;
                }
                if (mActionHandler != null) mActionHandler.onFaceUp();
            }
        } else {
            // Check if its face down enough.
            mSamples[mSampleIndex] = z < FACE_DOWN_GRAVITY_THRESHOLD;

            // face down
            if (filterSamples()) {
                mStopped = true;
                if (mActionHandler != null) mActionHandler.onFaceDown();
            }
        }

        mSampleIndex = ((mSampleIndex + 1) % SENSOR_SAMPLES);
    }
}