package com.example.silencewatchdog;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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


    private static String fileName = null;

    private boolean isThreadRun = true;
    float volume = 10000;
    private int bufferSize;
    private WatchdogMainActivity.SoundText text;

    private WatchdogMainActivity.RecordButton recordButton = null;
    private AudioRecord audio = null;
    private MediaRecorder recorder = null;

    private MediaPlayer player = null;
    private double amp;
    private TextView text_number = null;
    private int SAMPLE_DELAY = 75;



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
                amp = 20 * (float) (Math.log10(amp/0.05));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public double getAmplitude() {
        if (recorder != null)
            return recorder.getMaxAmplitude();
        else
            return 0;

    }

    private void stopRecording() {
        isThreadRun = false;
        audio.stop();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchdog_main);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";

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
            boolean mStartRecording = true;
            @Override
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    ToggleStartStopButton.setText("Stop recording");
                    startListenAudio();

                } else {
                    ToggleStartStopButton.setText("Start recording");
                }
                mStartRecording = !mStartRecording;
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
                    Thread.sleep(100);
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
