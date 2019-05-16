package com.example.silencewatchdog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.Manifest;

import java.util.logging.Handler;

public class WatchdogMainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner mode_selector;
    private Button ToggleStartStopButton;
    private Button soundControlBtn;
    private ArrayAdapter<CharSequence> adapter;
    private SeekBar threshold_Seeker;
    private TextView threshold_indicator_text;
    private boolean isMonitoringSilence=false;
    private AudioAnalyzer audioAnalyze = new AudioAnalyzer();
    private TextView noise_level=null;
    private Thread thread;

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};



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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        setContentView(R.layout.activity_watchdog_main);

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

        threshold_Seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //set textView's text
                threshold_indicator_text.setText(""+progress + " dB");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

        });
        soundControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent soundControllerIntent = new Intent(getApplicationContext(), SoundControllerActivity.class);
                startActivity(soundControllerIntent);
            }
        });
        ToggleStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMonitoringSilence) {
                    stopMonitoring();
                } else
                    startMonitoring();
            }
        });
    }

    private void stopMonitoring() {
        isMonitoringSilence = false;
        ToggleStartStopButton.setText("Start");
        // TODO change button to blue
        audioAnalyze.stop();
        thread.interrupt();
        thread=null;
    }

    private void startMonitoring() {
        isMonitoringSilence = true;
        ToggleStartStopButton.setText("Stop");
        audioAnalyze.start();
        thread = new Thread(new Task());
        thread.start();
    }

    class Task implements Runnable {
        @Override
        public void run() {
            while(thread !=null && !thread.isInterrupted())
            {
                try {
                    Thread.sleep(75);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        noise_level.setText(audioAnalyze.getMaxSoundPressure() + "");

                    }
                });

            }


        }
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
