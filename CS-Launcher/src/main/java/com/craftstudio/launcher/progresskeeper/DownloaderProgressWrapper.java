package com.craftstudio.launcher.progresskeeper;

import static com.craftstudio.launcher.Tools.BYTE_TO_MB;

import com.craftstudio.launcher.utils.ZHTools;
import com.craftstudio.launcher.utils.file.FileTools;

import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.tasks.SpeedCalculator;

public class DownloaderProgressWrapper implements Tools.DownloaderFeedback {
    private final SpeedCalculator mSpeedCalculator = new SpeedCalculator(128);
    private final int mProgressString;
    private final String mProgressRecord;
    private long progressUpdateTime = 0;

    /**
     * A simple wrapper to send the downloader progress to ProgressKeeper
     * @param progressString the string that will be used in the progress reporter
     * @param progressRecord the record for ProgressKeeper
     */
    public DownloaderProgressWrapper(int progressString, String progressRecord) {
        this.mProgressString = progressString;
        this.mProgressRecord = progressRecord;
    }

    @Override
    public void updateProgress(long curr, long max) {
        long currentTime = ZHTools.getCurrentTimeMillis();
        if (currentTime - progressUpdateTime < 150) return;
        progressUpdateTime = currentTime;

        Object[] va;
        va = new Object[3];
        va[0] = curr / BYTE_TO_MB;
        va[1] = max / BYTE_TO_MB;
        va[2] = FileTools.formatFileSize(mSpeedCalculator.feed(curr));
        // the allocations are fine because thats how java implements variadic arguments in bytecode: an array of whatever
        ProgressKeeper.submitProgress(mProgressRecord, (int) Math.max((float) curr / max * 100, 0), mProgressString, va);
    }
}
