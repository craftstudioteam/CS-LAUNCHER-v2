package com.craftstudio.launcher.modloaders.modpacks.api;

import androidx.annotation.Nullable;

import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.setting.AllSettings;

import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ModDownloader {
    private static final ThreadLocal<byte[]> sThreadLocalBuffer = new ThreadLocal<>();
    private final ThreadPoolExecutor mDownloadPool;
    private final AtomicBoolean mTerminator = new AtomicBoolean(false);
    private final AtomicInteger mDownloadProgress = new AtomicInteger(0);
    private final AtomicLong mDownloadedSize = new AtomicLong(0);
    private final Object mExceptionSyncPoint = new Object();
    private final File mDestinationDirectory;
    private final boolean mUseFileCount;
    private IOException mFirstIOException;
    private long mTotalSize;

    public ModDownloader(File destinationDirectory) {
        this(destinationDirectory, false);
    }

    public ModDownloader(File destinationDirectory, boolean useFileCount) {
        int maxThreads = AllSettings.getMaxDownloadThreads().getValue();
        this.mDownloadPool = new ThreadPoolExecutor(
                Math.max(1, (int) (maxThreads / 2)),
                maxThreads,
                100,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );

        this.mDownloadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        this.mDestinationDirectory = destinationDirectory;
        this.mUseFileCount = useFileCount;
    }

    public void submitDownload(int fileSize, String relativePath, @Nullable String downloadHash, String... url) {
        if(mUseFileCount) mTotalSize += 1;
        else mTotalSize += fileSize;
        mDownloadPool.execute(new DownloadTask(url, new File(mDestinationDirectory, relativePath), downloadHash));
    }

    public void submitDownload(FileInfoProvider infoProvider) {
        if(!mUseFileCount) throw new RuntimeException("This method can only be used in a file-counting ModDownloader");
        mTotalSize += 1;
        mDownloadPool.execute(new FileInfoQueryTask(infoProvider));
    }

    public void awaitFinish(Tools.DownloaderFeedback feedback) throws IOException {
        awaitFinish(() -> feedback.updateProgress(mDownloadedSize.get(), mTotalSize));
    }

    public void awaitFinish(DownloadProgressListener listener) throws IOException {
        awaitFinish(() -> listener.feedback(mDownloadProgress.get(), (int) mTotalSize, mDownloadedSize.get()));
    }

    private void awaitFinish(OnFileDownloadedListener listener) throws IOException {
        try {
            mDownloadPool.shutdown();
            while(!mDownloadPool.awaitTermination(20, TimeUnit.MILLISECONDS) && !mTerminator.get()) {
                listener.downloaded();
            }
            if(mTerminator.get()) {
                mDownloadPool.shutdownNow();
                synchronized (mExceptionSyncPoint) {
                    if(mFirstIOException == null) mExceptionSyncPoint.wait();
                    throw mFirstIOException;
                }
            }
        }catch (InterruptedException e) {
            Logging.e("ModDownloader", Tools.printToString(e));
        }
    }

    private static byte[] getThreadLocalBuffer() {
        byte[] buffer = sThreadLocalBuffer.get();
        if(buffer != null) return buffer;
        buffer = new byte[8192];
        sThreadLocalBuffer.set(buffer);
        return buffer;
    }

    private void downloadFailed(IOException exception) {
        mTerminator.set(true);
        synchronized (mExceptionSyncPoint) {
            if(mFirstIOException == null) {
                mFirstIOException = exception;
                mExceptionSyncPoint.notify();
            }
        }
    }

    class FileInfoQueryTask implements Runnable {
        private final FileInfoProvider mFileInfoProvider;
        public FileInfoQueryTask(FileInfoProvider fileInfoProvider) {
            this.mFileInfoProvider = fileInfoProvider;
        }
        @Override
        public void run() {
            try {
                FileInfo fileInfo = mFileInfoProvider.getFileInfo();
                if(fileInfo == null) return;
                new DownloadTask(new String[]{fileInfo.url},
                        new File(mDestinationDirectory, fileInfo.relativePath), fileInfo.sha1).run();
            }catch (IOException e) {
                downloadFailed(e);
            }
        }
    }

    class DownloadTask implements Runnable, Tools.DownloaderFeedback {
        private final String[] mDownloadUrls;
        private final File mDestination;
        private final String mSha1;
        private long last = 0L;

        public DownloadTask(String[] downloadurls,
                            File downloadDestination, String downloadHash) {
            this.mDownloadUrls = downloadurls;
            this.mDestination = downloadDestination;
            this.mSha1 = downloadHash;
        }

        @Override
        public void run() {
            for(String sourceUrl : mDownloadUrls) {
                try {
                    DownloadUtils.ensureSha1(mDestination, mSha1, (Callable<Void>) () -> {
                        IOException exception = tryDownload(sourceUrl);
                        if(exception != null) {
                            throw exception;
                        }
                        return null;
                    });

                } catch (IOException e) {
                    downloadFailed(e);
                }
            }
        }

        private IOException tryDownload(String sourceUrl) throws InterruptedIOException {
            IOException exception = null;
            for (int i = 0; i < 5; i++) {
                try {
                    DownloadUtils.downloadFileMonitored(sourceUrl, mDestination, getThreadLocalBuffer(), this);
                    if (mUseFileCount) mDownloadProgress.addAndGet(1);
                    return null;
                } catch (InterruptedIOException e) {
                    throw e;
                } catch (IOException e) {
                    Logging.e("ModDownloader", Tools.printToString(e));
                    exception = e;
                }
                mDownloadedSize.addAndGet(-last);
                last = 0;
            }
            return exception;
        }

        @Override
        public void updateProgress(long curr, long max) {
            long size = curr - last;
            mDownloadedSize.addAndGet(size);
            last = curr;
        }
    }

    public static class FileInfo {
        public final String url;
        public final String relativePath;
        public final String sha1;

        public FileInfo(String url, String relativePath, @Nullable String sha1) {
            this.url = url;
            this.relativePath = relativePath;
            this.sha1 = sha1;
        }
    }

    public interface FileInfoProvider {
        FileInfo getFileInfo() throws IOException;
    }

    private interface OnFileDownloadedListener {
        void downloaded();
    }

    /**
     * 一个已下载文件数量、已下载文件总大小的监听器
     */
    public interface DownloadProgressListener {
        void feedback(int downloadedCount, int totalCount, long downloadedSize);
    }
}
