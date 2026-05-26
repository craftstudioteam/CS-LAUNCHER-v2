package com.craftstudio.launcher.utils.file;

import android.app.Activity;
import android.widget.Toast;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.task.Task;
import com.craftstudio.launcher.task.TaskExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PasteFile {
    private static final PasteFile instance = new PasteFile();
    private final List<File> copyFiles = new ArrayList<>();
    private File mRoot = null;
    private PasteType pasteType = null;

    private PasteFile() {
    }

    public static PasteFile getInstance() {
        return instance;
    }

    public void setCopyFiles(List<File> files) {
        this.copyFiles.clear();
        this.copyFiles.addAll(files);
    }

    public void setPaste(File root, List<File> files, PasteType type) {
        setCopyFiles(files);
        this.mRoot = root;
        this.pasteType = type;
    }

    public PasteType getPasteType() {
        return pasteType;
    }

    public void pasteFiles(Activity activity, File target, FileCopyHandler.FileExtensionGetter fileExtensionGetter, Task<?> endTask) {
        if (copyFiles.isEmpty()) {
            TaskExecutors.runInUIThread(() -> Toast.makeText(activity, activity.getString(R.string.file_does_not_exist), Toast.LENGTH_SHORT).show());
            return;
        }
        new FileCopyHandler(activity, pasteType, copyFiles, mRoot, target, fileExtensionGetter, endTask.beforeStart(this::resetState)).start();
    }

    private void resetState() {
        pasteType = null;
        copyFiles.clear();
    }

    public enum PasteType {
        COPY, MOVE
    }
}
