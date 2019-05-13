package com.example.dell.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.IOException;

import android.widget.TextView;

import java.text.DecimalFormat;

public class MainActivity2 extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private Thread thread;
    private boolean isThreadRun = true;
    float volume = 10000;
    private SoundText text;

    private RecordButton recordButton = null;
    private MediaRecorder recorder = null;

    private MediaPlayer player = null;

    private TextView text_number = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            DecimalFormat df1 = new DecimalFormat("####.0");

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        isThreadRun = true;
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //recorder.setOutputFile(fileName);
        recorder.setOutputFile("/dev/null");
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();

        getAmplitude();

        //double a=Math.log10(recorder.getMaxAmplitude() / 2700.0);
    }

    private void startListenAudio() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isThreadRun) {
                    //if(bListener) {
                    volume = recorder.getMaxAmplitude();  //Get the sound pressure value
                    if (volume > 0 && volume < 1000000) {
                        World.setDbCount(20 * (float) (Math.log10(volume)));  //Change the sound pressure value to the decibel value
                        // Update with thread
                        Message message = new Message();
                        message.what = 1;
                        //handler.sendMessage(message);
                        //TextView t1 = findViewById(R.id.textView);
                        //t1.setText((20 * (float) (Math.log10(volume))) + "");
                        //playButton.setText((20 * (float) (Math.log10(volume))) + "");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                text.setText(World.dbCount+"");
                            }
                        });
                   }

                    // }
                    //if(refreshed){
                    //    Thread.sleep(1200);
                    //    refreshed=false;
                    //}else{
                    //    Thread.sleep(200);
                    // }
                }
            }
        });
        thread.start();
    }

    public double getAmplitude() {
        if (recorder != null)
            return  recorder.getMaxAmplitude();
        else
            return 0;

    }

    private void stopRecording() {
        isThreadRun = false;
        //thread = null;
        recorder.stop();
        recorder.release();
        recorder = null;

    }

    class RecordButton extends android.support.v7.widget.AppCompatButton {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                    startListenAudio();

                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
               // setText(Math.log10(recorder.getMaxAmplitude() / 2700.0)+"");

            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }


    class SoundText extends android.support.v7.widget.AppCompatTextView {
        boolean mStartPlaying = true;

        public SoundText(Context context) {
            super(context);
            setText("");
        }
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Record to the external cache directory for visibility
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        LinearLayout ll = new LinearLayout(this);
        recordButton = new RecordButton(this);
        ll.addView(recordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
//        playButton = new PlayButton(this);
//        ll.addView(playButton,
//                new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));
//        setContentView(ll);

        text = new SoundText(this);
        ll.addView(text,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        setContentView(ll);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
            isThreadRun = false;
            thread = null;
        }
    }
}

