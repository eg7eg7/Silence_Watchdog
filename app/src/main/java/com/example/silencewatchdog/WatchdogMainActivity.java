package com.example.silencewatchdog;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class WatchdogMainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner mode_selector;
    private Button ToggleStartStopButton;
    private Button soundControlBtn;
    private ArrayAdapter<CharSequence> adapter;
    private SeekBar threshold_Seeker;
    private TextView threshold_indicator_text;
    private boolean isMonitoringSilence=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void startMonitoring() {
        isMonitoringSilence = true;
        ToggleStartStopButton.setText("Stop");

        //TODO change button to red

        monitor();

    }

    private void monitor() {
        //TODO create a new thread that runs this code
        new Thread(new Task()).start();
    }

    class Task implements Runnable {
        @Override
        public void run() {
            boolean down = true;
            while (isMonitoringSilence) {
                int num = threshold_Seeker.getProgress();
                if (num == 0)
                    down = false;
                else if (num == R.integer.MAX_THRESHOLD)
                    down = true;

                if (!down)
                    threshold_Seeker.setProgress(num - 1);
                else
                    threshold_Seeker.setProgress(num + 1);
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
