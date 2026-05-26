package com.craftstudio.launcher.feature.update;

import static com.craftstudio.launcher.task.TaskExecutors.runInUIThread;
import static com.craftstudio.launcher.utils.file.FileTools.formatFileSize;
import static com.craftstudio.launcher.utils.path.UrlManager.TIME_OUT;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.ui.dialog.ProgressDialog;
import com.craftstudio.launcher.utils.ZHTools;
import com.craftstudio.launcher.utils.path.UrlManager;

import com.craftstudio.launcher.Tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public final class UpdateLauncher {
    private final Context context;
    private final LauncherVersion launcherVersion;
    private final String destinationFilePath;
    private final Call call;
    private ProgressDialog dialog;
    private Timer timer;
    private boolean isCanceled = false;

    public UpdateLauncher(Context context, LauncherVersion launcherVersion) {
        this.context = context;
        this.launcherVersion = launcherVersion;

        this.destinationFilePath = UpdateUtils.sApkFile.getAbsolutePath();
        this.call = new OkHttpClient.Builder()
                .writeTimeout(TIME_OUT.getFirst(), TIME_OUT.getSecond())
                .build()
                .newCall(
                        UrlManager.createRequestBuilder(UpdateUtils.getDownloadUrl(launcherVersion)).build()
                );
    }

    public void start() {
        this.call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                UpdateUtils.showFailToast(context, context.getString(R.string.update_fail));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    UpdateUtils.showFailToast(context, context.getString(R.string.update_fail_code, response.code()));
                    throw new IOException("Unexpected code " + response);
                } else {
                    File outputFile = new File(UpdateLauncher.this.destinationFilePath);
                    Objects.requireNonNull(response.body());
                    try (InputStream inputStream = response.body().byteStream();
                         OutputStream outputStream = Files.newOutputStream(outputFile.toPath())
                    ) {
                        byte[] buffer = new byte[1024 * 1024];
                        int bytesRead;

                        runInUIThread(() -> {
                            UpdateLauncher.this.dialog = new ProgressDialog(UpdateLauncher.this.context, () -> {
                                UpdateLauncher.this.stop();
                                return true;
                            });
                            UpdateLauncher.this.dialog.show();
                        });

                        final long[] downloadedSize = new long[1];
                        final long[] lastSize = {0};
                        final long[] lastTime = {ZHTools.getCurrentTimeMillis()};

                        //限制刷新速度
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                long size = downloadedSize[0];
                                long currentTime = ZHTools.getCurrentTimeMillis();
                                double timeElapsed = (currentTime - lastTime[0]) / 1000.0;
                                long sizeChange = size - lastSize[0];
                                long rate = (long) (sizeChange / timeElapsed);

                                lastSize[0] = size;
                                lastTime[0] = currentTime;

                                String formattedDownloaded = formatFileSize(size);
                                String totalSize = formatFileSize(UpdateUtils.getFileSize(launcherVersion.getFileSize()));
                                handleDialog(dialog -> {
                                    dialog.updateProgress(size, UpdateUtils.getFileSize(launcherVersion.getFileSize()));
                                    dialog.updateRate(rate > 0 ? rate : 0L);
                                    dialog.updateText(String.format(context.getString(R.string.update_downloading), formattedDownloaded, totalSize));
                                });
                            }
                        }, 0, 120);

                        try {
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                                downloadedSize[0] += bytesRead;
                            }
                            finish(outputFile);
                        } catch (Exception e) {
                            handleDownloadError(e);
                        }
                    } catch (Exception e) {
                        handleDownloadError(e);
                    }
                }
            }
        });
    }

    private void finish(File outputFile) {
        handleDialog(Dialog::dismiss);
        timer.cancel();

        UpdateUtils.installApk(context, outputFile);
    }

    public void handleDialog(Consumer<ProgressDialog> func) {
        if (UpdateLauncher.this.dialog != null) {
            runInUIThread(() -> func.accept(UpdateLauncher.this.dialog));
        }
    }

    private void handleDownloadError(Exception e) {
        if (isCanceled) {
            //已经取消了下载，不处理取消带来的任何异常
            return;
        }

        handleDialog(Dialog::dismiss);
        runInUIThread(() -> Tools.showError(context, R.string.update_fail, e));
        timer.cancel();
        FileUtils.deleteQuietly(UpdateUtils.sApkFile);
        Logging.e("Update Launcher", "There was an exception downloading the update!", e);
    }

    private void stop() {
        this.isCanceled = true;
        this.call.cancel();
        this.timer.cancel();
        FileUtils.deleteQuietly(UpdateUtils.sApkFile);
    }
}
