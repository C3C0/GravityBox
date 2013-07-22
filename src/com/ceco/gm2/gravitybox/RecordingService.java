package com.ceco.gm2.gravitybox;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.IBinder;

public class RecordingService extends Service {
    public static final String ACTION_RECORDING_START = "gravitybox.intent.action.RECORDING_START";
    public static final String ACTION_RECORDING_STOP = "gravitybox.intent.action.RECORDING_STOP";
    public static final String ACTION_RECORDING_STATUS_CHANGED = "gravitybox.intent.action.RECORDING_STATUS_CHANGED";
    public static final String EXTRA_RECORDING_STATUS = "recordingStatus";
    public static final String EXTRA_STATUS_MESSAGE = "statusMessage";
    public static final String EXTRA_AUDIO_FILENAME = "audioFileName";
    
    public static final int RECORDING_STATUS_IDLE = 0;
    public static final int RECORDING_STATUS_STARTED = 1;
    public static final int RECORDING_STATUS_STOPPED = 2;
    public static final int RECORDING_STATUS_ERROR = -1;

    private MediaRecorder mRecorder;
    private int mRecordingStatus = RECORDING_STATUS_IDLE;
    private Notification mRecordingNotif;
    private PendingIntent mPendingIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRecordingStatus = RECORDING_STATUS_IDLE;

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.quick_settings_qr_recording));
        builder.setContentText(getString(R.string.quick_settings_qr_recording_notif));
        builder.setSmallIcon(R.drawable.ic_qs_qr_recording);
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_qs_qr_recording);
        builder.setLargeIcon(b);
        Intent intent = new Intent(ACTION_RECORDING_STOP);
        mPendingIntent = PendingIntent.getService(this, 0, intent, 0);
        builder.setContentIntent(mPendingIntent);
        mRecordingNotif = builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_RECORDING_START) &&
                    intent.hasExtra(EXTRA_AUDIO_FILENAME)) {
                String audioFileName = intent.getStringExtra(EXTRA_AUDIO_FILENAME);
                startRecording(audioFileName);
                return START_STICKY;
            } else if (intent.getAction().equals(ACTION_RECORDING_STOP)) {
                stopRecording();
                return START_STICKY;
            }
        }

        stopSelf();
        return START_NOT_STICKY;
    }

    MediaRecorder.OnErrorListener mOnErrorListener = new MediaRecorder.OnErrorListener() {

        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            mRecordingStatus = RECORDING_STATUS_ERROR;

            String statusMessage = "Error in MediaRecorder while recording: " + what + "; " + extra;
            Intent i = new Intent(ACTION_RECORDING_STATUS_CHANGED);
            i.putExtra(EXTRA_RECORDING_STATUS, mRecordingStatus);
            i.putExtra(EXTRA_STATUS_MESSAGE, statusMessage);
            sendBroadcast(i);
            stopForeground(true);
        }
    };

    private void startRecording(String audioFileName) {
        String statusMessage = "";
        try {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(audioFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOnErrorListener(mOnErrorListener);
            mRecorder.prepare();
            mRecorder.start();
            mRecordingStatus = RECORDING_STATUS_STARTED;
            startForeground(1, mRecordingNotif);
        } catch (Exception e) {
            e.printStackTrace();
            mRecordingStatus = RECORDING_STATUS_ERROR;
            statusMessage = e.getMessage();
        } finally {
            Intent i = new Intent(ACTION_RECORDING_STATUS_CHANGED);
            i.putExtra(EXTRA_RECORDING_STATUS, mRecordingStatus);
            i.putExtra(EXTRA_STATUS_MESSAGE, statusMessage);
            sendBroadcast(i);        
        }
    }

    private void stopRecording() {
        if (mRecorder == null) return;

        String statusMessage = "";
        try {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            mRecordingStatus = RECORDING_STATUS_STOPPED;
        } catch (Exception e) {
            e.printStackTrace();
            mRecordingStatus = RECORDING_STATUS_ERROR;
            statusMessage = e.getMessage();
        } finally {
            Intent i = new Intent(ACTION_RECORDING_STATUS_CHANGED);
            i.putExtra(EXTRA_RECORDING_STATUS, mRecordingStatus);
            i.putExtra(EXTRA_STATUS_MESSAGE, statusMessage);
            sendBroadcast(i);
            stopForeground(true);
        }
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }
}