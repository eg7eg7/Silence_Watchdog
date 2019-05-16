package com.example.silencewatchdog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.Manifest;

import java.net.URI;

public class WatchdogMainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private MediaPlayer quiet_sound;
    private Spinner mode_selector;
    private Button ToggleStartStopButton;
    private Button soundControlBtn;
    private ArrayAdapter<CharSequence> adapter;
    private SeekBar threshold_Seeker;
    private TextView threshold_indicator_text;
    private TextView noise_level = null;
    private Thread thread;
    private boolean isThreadRun = false;
    private double current_threshold;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SharedPreferences preferences;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private SharedPreferences.Editor prefEditor;
    private int bufferSize;
    private float volume = (float) 100;
    private AudioRecord audio = null;
    private double amp;
    private int SAMPLE_DELAY = 75;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchdog_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        mode_selector = findViewById(R.id.silence_mode_id);
        soundControlBtn = findViewById(R.id.soundControllerBtn);
        ToggleStartStopButton = findViewById(R.id.startBtn);
        adapter = ArrayAdapter.createFromResource(this, R.array.modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode_selector.setAdapter(adapter);
        mode_selector.setOnItemSelectedListener(this);
        threshold_Seeker = findViewById(R.id.seekbar_id);
        threshold_indicator_text = findViewById(R.id.threshold_indicator_id);
        noise_level = findViewById(R.id.noise_level_id);

        preferences = getApplicationContext().getSharedPreferences("silence_app", 0);
        prefEditor = preferences.edit();
        threshold_Seeker.setProgress(60);
        threshold_Seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //set textView's text
                threshold_indicator_text.setText("" + progress);
                current_threshold = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
        soundControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent soundControllerIntent = new Intent(getApplicationContext(), SoundControllerActivity.class);
                startActivity(soundControllerIntent);
            }
        });
        ToggleStartStopButton.setOnClickListener(new View.OnClickListener() {
            boolean mStartRecording = true;

            @Override
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    ToggleStartStopButton.setText("Stop");
                    startListenAudio();

                } else {
                    ToggleStartStopButton.setText("Start");
                }
                mStartRecording = !mStartRecording;
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }


    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        String vol_string = preferences.getString("volume", "100");
        volume = Float.parseFloat(vol_string);

        String sound_name = preferences.getString("current_sound", "shhh");
        switch (sound_name){
            case "shhh":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shhh);
                break;
            case "shhh_2":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shhh_2);
                break;
            case "shhhTwice":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shhhtwice);
                break;
            case "shutUp":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shutup);
                break;

        }
        float volume_d = volume/100f;
        quiet_sound.setVolume(volume_d,volume_d);
        int sampleRate = 44100;
        try {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        } catch (Exception e) {
            Log.e("TrackingFlow", "Exception", e);
        }


        isThreadRun = true;
        audio.startRecording();
    }

    private void startListenAudio() {
        thread = new Thread(new Runnable() {
            public void run() {
                while (thread != null && !thread.isInterrupted()) {
                    //Let's make the thread sleep for a the approximate sampling time
                    try {
                        Thread.sleep(SAMPLE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    readAudioBuffer();//After this call we can get the last value assigned to the lastLevel variable
                    RequestSilence();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            noise_level.setText(amp + "");
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void RequestSilence() {
        if(current_threshold < amp)
        {
            quiet_sound.start();
        }
    }

    private void readAudioBuffer() {

        try {
            short[] buffer = new short[bufferSize];

            int bufferReadResult;

            if (audio != null) {

                // Sense the voice...
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                double sumLevel = 0;
                for (int i = 0; i < bufferReadResult; i++) {
                    sumLevel += buffer[i];
                }
                amp = Math.abs((sumLevel / bufferReadResult));
                amp = getDecibels();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getDecibels() {
        return 20 * (float) (Math.log10(amp / 0.1));
    }

    private void stopRecording() {
        isThreadRun = false;
        audio.stop();

    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
