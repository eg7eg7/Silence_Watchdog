package com.example.silencewatchdog;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioAnalyzer {
    private static final int SAMPLE_DELAY = 75;
    private int SAMPLE_RATE = 44100;
    private AudioRecord audio;
    private int bufferSize;
    private double amplitude = -1;
    private Thread thread;

    public void findAudioRecord() {

        try {
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }
    }




    public void stop() {
        if (thread != null)
            thread.interrupt();
        thread = null;

        audio.stop();
        audio.release();
    }

    public void start() {
        findAudioRecord();
        thread = new Thread(new Runnable() {
            public void run() {
                while (thread != null && !thread.isInterrupted()) {
                    //Let's make the thread sleep for a the approximate sampling time
                    readAudioBuffer();//After this call we can get the last value assigned to the lastLevel variable

                    try {
                        Thread.sleep(SAMPLE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }


                }
            }
        });
        thread.start();
    }

    /**
     * Functionality that gets the sound level out of the sample
     */
    private void readAudioBuffer() {

        try {
            short[] buffer = new short[bufferSize];

            int bufferReadResult;

            if (audio != null) {

                // Sense the voice...
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                Log.d("AudioAnalyzer", "readAudioBuffer: "+bufferReadResult);

                if(bufferReadResult == 0)
                {
                    amplitude = 0;
                }
                double sumLevel = 0;
                for (int i = 0; i < bufferReadResult; i++) {
                    sumLevel += buffer[i];
                }

                amplitude = Math.abs((sumLevel / bufferReadResult));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getMaxSoundPressure() {
        return amplitude;
    }

    public double getMaxDecibels() {
        return 20 * (float) (Math.log10(amplitude / 0.1));
    }
}
