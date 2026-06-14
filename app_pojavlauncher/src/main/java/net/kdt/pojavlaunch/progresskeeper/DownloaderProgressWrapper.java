package net.kdt.pojavlaunch.progresskeeper;

import static net.kdt.pojavlaunch.Tools.BYTE_TO_MB;

import net.kdt.pojavlaunch.Tools;

public class DownloaderProgressWrapper implements Tools.DownloaderFeedback {

    private final int mProgressString;
    private final String mProgressRecord;
    public String extraString = null;

    public String modName = null;
    public String modVersion = null;
    public String modIconUrl = null;
    public String contentType = null;

    private long mStartTime = -1;
    private long mLastSpeedUpdateTime = -1;
    private int mLastSpeedBytes = 0;
    private double mCurrentSpeed = 0; // MB/s

    /**
     * A simple wrapper to send the downloader progress to ProgressKeeper
     * @param progressString the string that will be used in the progress reporter
     * @param progressRecord the record for ProgressKeeper
     */
    public DownloaderProgressWrapper(int progressString, String progressRecord) {
        this.mProgressString = progressString;
        this.mProgressRecord = progressRecord;
    }

    public DownloaderProgressWrapper(int progressString, String progressRecord, String modName, String modVersion, String modIconUrl, String contentType) {
        this.mProgressString = progressString;
        this.mProgressRecord = progressRecord;
        this.modName = modName;
        this.modVersion = modVersion;
        this.modIconUrl = modIconUrl;
        this.contentType = contentType;
    }

    @Override
    public void updateProgress(int curr, int max) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (mStartTime == -1) {
            mStartTime = now;
            mLastSpeedUpdateTime = now;
            mLastSpeedBytes = curr;
        }

        long timeDiff = now - mLastSpeedUpdateTime;
        if (timeDiff >= 1000) {
            int bytesDiff = curr - mLastSpeedBytes;
            mCurrentSpeed = ((double) bytesDiff / 1024.0 / 1024.0) / (timeDiff / 1000.0);
            mLastSpeedUpdateTime = now;
            mLastSpeedBytes = curr;
        }

        double currentMB = (double) curr / 1024.0 / 1024.0;
        double totalMB = (double) max / 1024.0 / 1024.0;
        double remainingSec = -1;
        if (mCurrentSpeed > 0) {
            remainingSec = (double) (max - curr) / 1024.0 / 1024.0 / mCurrentSpeed;
        }

        Object[] va = new Object[9];
        va[0] = extraString;
        va[1] = currentMB;
        va[2] = totalMB;
        va[3] = mCurrentSpeed;
        va[4] = remainingSec;
        va[5] = modName;
        va[6] = modVersion;
        va[7] = modIconUrl;
        va[8] = contentType;

        int percentage = max > 0 ? (int) Math.max(((float) curr / max * 100), 0) : 0;
        ProgressKeeper.submitProgress(mProgressRecord, percentage, mProgressString, va);
    }
}
