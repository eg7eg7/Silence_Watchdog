package com.example.silencewatchdog;

public class EnergyFilter {
    private final int LECTURER_VARIANCE = 10;
    private final int BUFFER_SIZE = 20;
    private double currentBuffer[];
    private double pervBufferAvg;
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
        this.pervBufferAvg = 0;
        this.confidence = 1;

    }

    public void reportFalsePositive() {
        if (this.confidence - CONFIDENCE_FALSE_POSITIVE_WEIGHT < 0) {
            this.confidence -= CONFIDENCE_FALSE_POSITIVE_WEIGHT;
        }
    }

    public boolean nextSample(short[] powers) {
        /*
         * read next power add those powers to buffer and throw last added calculate
         * Delta (Delta = currentAvg - pervAvg) if Delta is Bigger than the variance
         * trigger
         */
        double avgSumSquarePowers = this.avgSumOfSquareOfPowers(powers);
        this.enterNewAvgToBuffer(avgSumSquarePowers);

        return isToShhh();

    }

    private double avgSumOfSquareOfPowers(short powers[]) {
        double sum = 0;
        for (int i = 0; i < powers.length; i++) {
            sum += Math.pow(powers[i], 2);
        }
        return sum / powers.length;
    }

    private void enterNewAvgToBuffer(double avgOfPowers) {
        this.pervBufferAvg = this.currentBufferAvg;

        this.currentBuffer[this.index % BUFFER_SIZE] = avgOfPowers;
        this.index++;

        this.currentBufferAvg = this.getAvg(this.currentBuffer);
    }

    private double getAvg(double arr[]) {
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
        } else if (getSampleDelta() < LECTURER_VARIANCE) {
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
        return this.currentBufferAvg - this.pervBufferAvg;
    }

}
