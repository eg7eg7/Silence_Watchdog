package com.example.silencewatchdog;

import android.content.Intent;
import android.content.SharedPreferences;
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

//import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.text.DecimalFormat;

public class WatchdogMainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Button ToggleStartStopButton;
    private Button soundControlBtn;
    private Button report_false_record_btn;

    private MediaPlayer quiet_sound;

    private Spinner mode_selector;

    private TextView Threshold_text;
    private TextView max_thres_text_id;
    private TextView textView5;
    private TextView threshold_indicator_text;
    private TextView noise_level = null;

    private ArrayAdapter<CharSequence> adapter;

    private SeekBar threshold_Seeker;

    private EnergyFilter energyfilter;

    private Thread thread;
    private boolean isThreadRun = false;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SharedPreferences preferences;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private SharedPreferences.Editor prefEditor;




    private double current_threshold;
    private int bufferSize;
    private float volume = (float) 100;
    private AudioRecord audio = null;
    private double amp;
    private int SAMPLE_DELAY = 300;
    private short[] buffer;
    private int buffer_size_read;
    private final int DELAY_GAP = 5000;
    private final int INITIAL_THRESHOLD = 50;

    private short session_max;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchdog_main);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        initGUIelements();

        arrayListSpinnerAdaptor();

        preferences = getApplicationContext().getSharedPreferences("silence_app", 0);
        prefEditor = preferences.edit();

        buffer_size_read = 1;
        threshold_Seeker.setProgress(INITIAL_THRESHOLD);
        current_threshold = INITIAL_THRESHOLD;
        threshold_indicator_text.setText(threshold_Seeker.getProgress()+"");
        threshold_Seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //set textView's text
                current_threshold = progress;
                threshold_indicator_text.setText("" + progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //DO NOTHING
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //DO NOTHING
            }

        });

        report_false_record_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                energyfilter.reportFalsePositive();
                showToast();
            }
        });

        mode_selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modeController();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do NOTHING
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
            boolean isStartRecording = true;

            @Override
            public void onClick(View v) {
                onRecord(isStartRecording);
                if (isStartRecording) {
                    report_false_record_btn.setVisibility(View.VISIBLE);
                    ToggleStartStopButton.setText("Stop");
                    energyfilter = new EnergyFilter();
                    startListenAudio();

                } else {
                    report_false_record_btn.setVisibility(View.GONE);
                    ToggleStartStopButton.setText("Start");
                }
                isStartRecording = !isStartRecording;
            }
        });

    }

    private void arrayListSpinnerAdaptor() {
        adapter = ArrayAdapter.createFromResource(this, R.array.modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode_selector.setAdapter(adapter);
        mode_selector.setOnItemSelectedListener(this);
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

    private void stopRecording() {
        isThreadRun = false;
        thread.interrupt();
        audio.stop();

    }

    public void updateMediaPlayer() {
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
            case "shhhtwice":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shhhtwice);
                break;
            case "shutup_man":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shutup_man);
                break;
            case "powerfulshhh":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.powerfulshhh);
                break;
            case "pullyourselftogether_man":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.pullyourselftogether_man);
                break;
            case "pullyourselftogether_women":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.pullyourselftogether_women);
                break;
            case "shhh_man":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shhh_man);
                break;
            case "shutup_women":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shutup_women);
                break;
            case "stoptalking":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.stoptalking);
                break;
            case "stopthat_women":
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.stopthat_women);
                break;
            default:
                quiet_sound = MediaPlayer.create(getApplicationContext(), R.raw.shhh);

        }
        float volume_d = volume / 100f;
        quiet_sound.setVolume(volume_d, volume_d);
    }
    private void startRecording() {
        isThreadRun = true;


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
                            noise_level.setText(new DecimalFormat("##.##").format(amp));
                        }
                    });


                }
            }
        });
        thread.start();
    }

    private void RequestSilence() {
        if (!isThreadRun)
            return;
        updateMediaPlayer();
        String mode = mode_selector.getSelectedItem() + "";
        updateAmplitude(buffer_size_read);

        switch (mode) {
            case "Custom":
                //updateAmplitude(buffer_size_read);
                if (current_threshold < amp) {
                    playSound();
                }
                break;
            case "Classroom":

                if (energyfilter.nextSample(buffer)) {
                    playSound();
                }
                break;
        }

    }
    public void playSound()
    {
        int k;
        quiet_sound.start();

        try {
            thread.sleep(DELAY_GAP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void readAudioBuffer() {

        try {
            buffer = new short[bufferSize];
            if (audio != null) {
                // Sense the voice...
                buffer_size_read = audio.read(buffer, 0, bufferSize);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAmplitude(int bufferReadNum) {
        if(bufferReadNum == 0){
            Log.d("updateAmplitude", "bufferReadNum = 0 return");
            return;
        }
        double sumLevel = 0;
        for (int i = 0; i < bufferReadNum; i++) {
            sumLevel += buffer[i];
        }
        amp = Math.abs((sumLevel / bufferReadNum));
        amp = getDecibels();
    }

    public double getDecibels() {
        return 20 * (float) (Math.log10(amp / 0.1));
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    public void showToast(){
        //TODO remove comment and make it work
        //StyleableToast.makeText(this, "You Reported on a false record", R.style.toast).show();
    }

    private void initGUIelements() {
        mode_selector = findViewById(R.id.silence_mode_id);
        soundControlBtn = findViewById(R.id.soundControllerBtn);
        threshold_Seeker = findViewById(R.id.seekbar_id);
        ToggleStartStopButton = findViewById(R.id.startBtn);
        noise_level = findViewById(R.id.noise_level_id);
        threshold_indicator_text = findViewById(R.id.threshold_indicator_id);
        Threshold_text = findViewById(R.id.Threshold_text);
        max_thres_text_id = findViewById(R.id.max_thres_text_id);
        textView5 = findViewById(R.id.textView5);
        report_false_record_btn = findViewById(R.id.report_false_record_btn);
        threshold_indicator_text = findViewById(R.id.threshold_indicator_id);
    }

    private void modeController(){
        report_false_record_btn.setVisibility(View.GONE);
        if(mode_selector.getSelectedItem().equals("Classroom")){
            threshold_Seeker.setVisibility(View.GONE);
            Threshold_text.setVisibility(View.GONE);
            max_thres_text_id.setVisibility(View.GONE);
            textView5.setVisibility(View.GONE);
            threshold_indicator_text.setVisibility(View.GONE);

        }
        else{
            threshold_Seeker.setVisibility(View.VISIBLE);
            Threshold_text.setVisibility(View.VISIBLE);
            max_thres_text_id.setVisibility(View.VISIBLE);
            textView5.setVisibility(View.VISIBLE);
            threshold_indicator_text.setVisibility(View.VISIBLE);
        }
    }
}
