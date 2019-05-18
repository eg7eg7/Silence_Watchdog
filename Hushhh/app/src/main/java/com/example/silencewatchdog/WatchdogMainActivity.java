package com.example.silencewatchdog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
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

import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.text.DecimalFormat;

public class WatchdogMainActivity extends AppCompatActivity {

    private Button ToggleStartStopButton;
    private Button soundControlBtn;
    private Button report_false_record_btn;

    private MediaPlayer quiet_sound;

    private Spinner mode_selector;

    private Spinner lecturer_selector;

    private TextView lecturerText;
    private TextView Threshold_text;
    private TextView max_threshold_text_id;
    private TextView min_threshold_text;
    private TextView threshold_indicator_text;
    private TextView noise_level = null;
    private TextView noise_level_text_view = null;

    private ArrayAdapter<CharSequence> adapter;
    private ArrayAdapter<CharSequence> adapter2;

    private SeekBar threshold_Seeker;

    private EnergyFilter energyfilter;

    private Thread thread;
    private boolean isThreadRun = false;

    private final int sampleRate = 44100;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SharedPreferences preferences;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private SharedPreferences.Editor prefEditor;

    private double current_threshold;
    private int bufferSize;
    private float volume = (float) 100;
    private AudioRecord audio = null;
    //last amplitude in dB
    private double amp;
    //delay between two samples in the mic
    private final int SAMPLE_DELAY = 300;
    private short[] buffer;
    private int buffer_size_read;
    //delay between two silencers
    private final int DELAY_GAP = 5000;
    private final int INITIAL_THRESHOLD = 50;
    private boolean lastSilenced = false;
    boolean isStartRecording = false;
    private String current_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchdog_main);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        initGUIelements();

        arrayListSpinnerAdaptor();

        preferences = getApplicationContext().getSharedPreferences("silence_app", 0);
        prefEditor = preferences.edit();
        quiet_sound = new MediaPlayer();
        quiet_sound.setAudioStreamType(AudioManager.STREAM_MUSIC);
        buffer_size_read = 1;
        threshold_Seeker.setProgress(INITIAL_THRESHOLD);
        current_threshold = INITIAL_THRESHOLD;
        threshold_indicator_text.setText(threshold_Seeker.getProgress() + "");
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
                if (isStartRecording)
                    toggleButtonFunction();
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


            @Override
            public void onClick(View v) {
                toggleButtonFunction();

            }
        });

    }

    public void toggleButtonFunction() {
        isStartRecording = !isStartRecording;
        onRecord(isStartRecording);
        if (isStartRecording) {
            if (mode_selector.getSelectedItem().equals("Classroom"))
                report_false_record_btn.setVisibility(View.VISIBLE);
            ToggleStartStopButton.setText("Stop");
            energyfilter = new EnergyFilter();


        } else {
            if (mode_selector.getSelectedItem().equals("Classroom"))
                report_false_record_btn.setVisibility(View.GONE);
            ToggleStartStopButton.setText("Start");
        }
    }

    private void arrayListSpinnerAdaptor() {
        adapter = ArrayAdapter.createFromResource(this, R.array.modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode_selector.setAdapter(adapter);

        adapter2 = ArrayAdapter.createFromResource(this, R.array.lecturer, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lecturer_selector.setAdapter(adapter2);
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
            listenToMicrophoneStart();
            startListenAudio();
        } else {
            listenToMicrophoneStop();
        }
    }

    private void listenToMicrophoneStop() {
        isThreadRun = false;
        thread.interrupt();
        thread = null;
        audio.stop();

    }

    public void updateMediaPlayer() {
        final String sound_key = this.getApplicationContext().getString(R.string.PREF_SOUNDNAME_KEY);
        final String volume_key = this.getApplicationContext().getString(R.string.PREF_VOLUME_KEY);

        String vol_string = preferences.getString(volume_key, "100");
        volume = Float.parseFloat(vol_string);
        String sound_name = preferences.getString(sound_key, "shhh");
        switch (sound_name) {
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

    private void listenToMicrophoneStart() {
        isThreadRun = true;


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
                        if (lastSilenced) {
                            Thread.sleep(DELAY_GAP);
                            lastSilenced = false;
                        } else
                            Thread.sleep(SAMPLE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    readAudioBuffer();//After this call we can get the last value assigned to the lastLevel variable
                    RequestSilence();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (current_mode.equals("Custom"))
                                noise_level.setText(new DecimalFormat("##.##").format(amp));
                            else if (current_mode.equals("Classroom"))
                                noise_level.setText(new DecimalFormat("##.##").format(energyfilter.getLastDelta()));
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
        current_mode = mode_selector.getSelectedItem() + "";
        updateAmplitude(buffer_size_read);

        switch (current_mode) {
            case "Custom":
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

    public void playSound() {
        lastSilenced = true;
        quiet_sound.start();
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
        if (bufferReadNum == 0) {
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


    public void showToast() {
        StyleableToast.makeText(this, "You Reported on a false record", R.style.toast).show();
    }

    private void initGUIelements() {
        lecturer_selector = findViewById(R.id.lecturer_spinner);
        lecturerText = findViewById(R.id.lecturerText);
        mode_selector = findViewById(R.id.silence_mode_id);
        soundControlBtn = findViewById(R.id.soundControllerBtn);
        threshold_Seeker = findViewById(R.id.seekbar_id);
        ToggleStartStopButton = findViewById(R.id.startBtn);
        noise_level = findViewById(R.id.noise_level_id);
        threshold_indicator_text = findViewById(R.id.threshold_indicator_id);
        Threshold_text = findViewById(R.id.Threshold_text);
        max_threshold_text_id = findViewById(R.id.max_thres_text_id);
        min_threshold_text = findViewById(R.id.min_threshold_text);
        report_false_record_btn = findViewById(R.id.report_false_record_btn);
        threshold_indicator_text = findViewById(R.id.threshold_indicator_id);
        noise_level_text_view = findViewById(R.id.noise_level_text_view);
    }

    private void modeController() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {


                report_false_record_btn.setVisibility(View.GONE);
                if (mode_selector.getSelectedItem().equals("Classroom")) {
                    lecturerText.setVisibility(View.VISIBLE);
                    lecturer_selector.setVisibility(View.VISIBLE);
                    threshold_Seeker.setVisibility(View.GONE);
                    Threshold_text.setVisibility(View.GONE);
                    max_threshold_text_id.setVisibility(View.GONE);
                    min_threshold_text.setVisibility(View.GONE);
                    threshold_indicator_text.setVisibility(View.GONE);
                    noise_level_text_view.setText(R.string.Classroom_DEBUG_TEXT);


                } else {
                    lecturerText.setVisibility(View.GONE);
                    lecturer_selector.setVisibility(View.GONE);
                    threshold_Seeker.setVisibility(View.VISIBLE);
                    Threshold_text.setVisibility(View.VISIBLE);
                    max_threshold_text_id.setVisibility(View.VISIBLE);
                    min_threshold_text.setVisibility(View.VISIBLE);
                    threshold_indicator_text.setVisibility(View.VISIBLE);
                    noise_level_text_view.setText(R.string.Custom_DEBUG_TEXT);
                }
            }
        });
    }

}
