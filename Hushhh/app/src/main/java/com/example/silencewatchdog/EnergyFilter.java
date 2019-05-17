package com.example.silencewatchdog;

import android.util.Log;

public class EnergyFilter {
   // private final double LECTURER_VARIANCE_WEIGHT = 0.4;
    private double lecturerVariance = 6;
    private final int BUFFER_SIZE = 20;
    private double currentBuffer[];
    private double prevBufferAvg;
    private double currentBufferAvg;
    private int index;
    // TODO add user false positive of confidant of shhh to remove confident
    private double confidence;

    private final double CONFIDENCE_TRUE_POSITIVE_WEIGHT = 0.1;
    private final double CONFIDENCE_FALSE_POSITIVE_WEIGHT = 0.4;


    public EnergyFilter() {
        this.currentBuffer = new double[BUFFER_SIZE];
        this.index = 0;
        this.currentBufferAvg = 0;
        this.prevBufferAvg = 0;
        this.confidence = 1;
        this.lecturerVariance = currentBufferAvg;


    }

    public void reportFalsePositive() {
        if (this.confidence - CONFIDENCE_FALSE_POSITIVE_WEIGHT < 0) {
            this.confidence -= CONFIDENCE_FALSE_POSITIVE_WEIGHT;
        }
    }

    public boolean nextSample(short[] powers) {
        /*
         * update maxPower Per Session and normalize it
         * calculate variance and enter to buffer
         */
       // updateMaxPower(powers);
       // double[] normalizedPowers = normalizePowersBaseMaxPower(powers);

        double avgSumSquarePowers = this.getAvg(powers);
        this.enterNewAvgToBuffer(avgSumSquarePowers);

        return isToShhh();

    }

    private double avgSumOfSquareOfPowers(double powers[]) {
        double sum = 0;
        for (int i = 0; i < powers.length; i++) {
            sum += Math.pow(powers[i], 2);
        }
        return sum / powers.length;
    }

    private void enterNewAvgToBuffer(double avgOfPowers) {
        this.prevBufferAvg = this.currentBufferAvg;
        double oldVal =   this.currentBuffer[this.index % BUFFER_SIZE];
        this.currentBuffer[this.index % BUFFER_SIZE] = avgOfPowers;


        this.currentBufferAvg = this.avgSumOfSquareOfPowers(this.currentBuffer);
        //this.lecturerVariance = currentBufferAvg * LECTURER_VARIANCE_WEIGHT;


        //if the sample will cause shh or there is silence dont take it into the Buffer
        if(getSampleDelta() > lecturerVariance || getSampleDelta() < -lecturerVariance){
            this.currentBuffer[this.index % BUFFER_SIZE] = oldVal;
        }

        this.index++;


        Log.d("EnergyFilter","previous average: " + prevBufferAvg + ", current average: " + currentBufferAvg);
        Log.d("EnergyFilterDelta", "Delta = " + (currentBufferAvg - prevBufferAvg));
    }

    private double getAvg(short[] arr) {
        double sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }

        return sum / arr.length;
    }

    private boolean isToShhh() {
        // Need to sample more information before giving shhh
        if (this.index <= BUFFER_SIZE) {
            return false;
        } else if (getSampleDelta() < lecturerVariance) {
            return false;
        }

        // a positive hit was "found" if increment confidence
        if (this.confidence + CONFIDENCE_TRUE_POSITIVE_WEIGHT < 1) {
            this.confidence += CONFIDENCE_TRUE_POSITIVE_WEIGHT;
        }

        // take confidence into account
        if (Math.random() > this.confidence) {
            return false;
        }
        return true;
    }

    private double getSampleDelta() {
        return this.currentBufferAvg - this.prevBufferAvg;
    }

}
