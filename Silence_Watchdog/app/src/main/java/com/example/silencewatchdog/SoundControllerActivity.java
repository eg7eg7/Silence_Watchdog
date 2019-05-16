package com.example.silencewatchdog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

public class SoundControllerActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner sound_selector;
    private Button backBtn;
    private Button playBtn;
    private ArrayAdapter<CharSequence> adapter;
    private SharedPreferences preferences;
    private MediaPlayer soundTest;
    Editor prefEditor;
    private float count;
    private SeekBar soundSeekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_controller);


        backBtn = findViewById(R.id.backBtn);
        playBtn = findViewById(R.id.playBtn);
        adapter = ArrayAdapter.createFromResource(this, R.array.soundModes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        preferences = getApplicationContext().getSharedPreferences("silence_app", 0);
        prefEditor = preferences.edit();

        sound_selector = findViewById(R.id.soundSpinner);
        sound_selector.setAdapter(adapter);
        sound_selector.setOnItemSelectedListener(this);
        sound_selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (sound_selector.getSelectedItem()+""){
                    case "shhh":
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.shhh);
                        prefEditor.putString("current_sound", "shhh");
                        prefEditor.commit();
                        break;
                    case "shhh_2":
                            soundTest = MediaPlayer.create(getApplicationContext(), R.raw.shhh_2);
                        prefEditor.putString("current_sound", "shhh_2");
                        prefEditor.commit();
                            break;
                    case "shhhtwice":
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.shhhtwice);
                        prefEditor.putString("current_sound", "shhhtwice");
                        prefEditor.commit();
                        break;
                    case "shutup_man":
                        prefEditor.putString("current_sound", "shutup_man");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.shutup_man);
                        break;
                    case "powerfulshhh":
                        prefEditor.putString("current_sound", "powerfulshhh");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.powerfulshhh);
                        break;
                    case "pullyourselftogether_man":
                        prefEditor.putString("current_sound", "pullyourselftogether_man");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.pullyourselftogether_man);
                        break;
                    case "pullyourselftogether_women":
                        prefEditor.putString("current_sound", "pullyourselftogether_women");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.pullyourselftogether_women);
                        break;
                    case "shhh_man":
                        prefEditor.putString("current_sound", "shhh_man");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.shhh_man);
                        break;
                    case "shutup_women":
                        prefEditor.putString("current_sound", "shutup_women");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.shutup_women);
                        break;
                    case "stoptalking":
                        prefEditor.putString("current_sound", "stoptalking");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.stoptalking);
                        break;
                    case "stoptalking_women":
                        prefEditor.putString("current_sound", "stoptalking_women");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.stoptalking_women);
                        break;
                    case "stopthat_women":
                        prefEditor.putString("current_sound", "stopthat_women");
                        prefEditor.commit();
                        soundTest = MediaPlayer.create(getApplicationContext(), R.raw.stopthat_women);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        soundSeekBar = findViewById(R.id.soundSeekBar);
         soundSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                count = progress;
                prefEditor.putString("volume", progress+"");
                prefEditor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float vol = count / 100f;
                soundTest.setVolume(vol,vol);
                soundTest.start();
            }
        });
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
