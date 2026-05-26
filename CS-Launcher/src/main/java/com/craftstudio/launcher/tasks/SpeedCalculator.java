package com.craftstudio.launcher.tasks;
/**
 * A simple class to calculate the average Internet speed using a simple moving average.
 */
public class SpeedCalculator {
    private long mLastMillis;
    private long mLastBytes;
    private int mIndex;
    private final long[] mPreviousInputs;
    private long mSum;

    public SpeedCalculator() {
        this(64);
    }
    public SpeedCalculator(int averageDepth) {
        mPreviousInputs = new long[averageDepth];
    }
    private long addToAverage(long speed) {
        mSum -= mPreviousInputs[mIndex];
        mSum += speed;
        mPreviousInputs[mIndex] = speed;
        if(++mIndex == mPreviousInputs.length) mIndex = 0;
        return mSum / mPreviousInputs.length;
    }
    /**
     * Update the current amount of bytes downloaded.
     * @param bytes the new amount of bytes downloaded
     * @return the current download speed in bytes per second
     */
    public long feed(long bytes) {
        long millis = System.currentTimeMillis();
        long deltaBytes = bytes - mLastBytes;
        long deltaMillis = millis - mLastMillis;
        mLastBytes = bytes;
        mLastMillis = millis;

        if (deltaMillis <= 0) return 0;
        long speed = deltaBytes * 1000L / deltaMillis;
        speed = Math.min(speed, Long.MAX_VALUE / 2);

        return addToAverage(speed);
    }
}