package com.craftstudio.launcher.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.databinding.DialogOperationFileBinding;
import com.craftstudio.launcher.task.Task;
import com.craftstudio.launcher.utils.file.FileTools;
import com.craftstudio.launcher.utils.file.PasteFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesDialog extends FullScreenDialog implements DraggableDialog.DialogInitializationListener {
    private final DialogOperationFileBinding binding = DialogOperationFileBinding.inflate(getLayoutInflater());
    private final FilesButton mFilesButton;
    private final Task<?> mEndTask;
    private final File mRoot;
    private final List<File> mSelectedFiles;
    private String mFileSuffix;
    private OnCopyButtonClickListener mCopyClick;
    private OnMoreButtonClickListener mMoreClick;

    public FilesDialog(@NonNull Context context, FilesButton filesButton, Task<?> endTask, File root, List<File> selectedFiles) {
        super(context);
        this.mFilesButton = filesButton;
        this.mEndTask = endTask;
        this.mRoot = root;
        this.mSelectedFiles = selectedFiles;
    }

    public FilesDialog(@NonNull Context context, FilesButton filesButton, Task<?> endTask, File root, File file) {
        super(context);
        this.mFilesButton = filesButton;
        this.mEndTask = endTask;
        this.mRoot = root;
        List<File> singleFileList = new ArrayList<>();
        singleFileList.add(file);
        this.mSelectedFiles = singleFileList;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(mFilesButton);
        handleButtons(mFilesButton, mEndTask);
    }

    private void init(FilesButton filesButton) {
        this.setCancelable(true);
        this.setContentView(binding.getRoot());

        binding.closeButton.setOnClickListener(v -> this.dismiss());

        if (filesButton.more) {
            binding.moreView.setOnClickListener(v -> {
                if (this.mMoreClick != null) this.mMoreClick.onButtonClick();
                closeDialog();
            });
        }

        DraggableDialog.initDialog(this);
    }

    private void handleButtons(FilesButton filesButton, Task<?> endTask) {
        binding.deleteView.setOnClickListener(view -> {
            DeleteDialog deleteDialog = new DeleteDialog(getContext(), endTask, mSelectedFiles);
            deleteDialog.show();
            closeDialog();
        });

        PasteFile pasteFile = PasteFile.getInstance();
        binding.copyView.setOnClickListener(v -> {
            if (this.mCopyClick != null) {
                pasteFile.setPaste(mRoot, mSelectedFiles, PasteFile.PasteType.COPY); // 复制模式
                this.mCopyClick.onButtonClick();
            }
            closeDialog();
        });
        binding.moveView.setOnClickListener(v -> {
            if (this.mCopyClick != null) {
                pasteFile.setPaste(mRoot, mSelectedFiles, PasteFile.PasteType.MOVE); // 移动模式
                this.mCopyClick.onButtonClick();
            }
            closeDialog();
        });

        if (mSelectedFiles.size() == 1) { //单选模式
            File file = mSelectedFiles.get(0);
            binding.shareView.setOnClickListener(view -> {
                FileTools.shareFile(getContext(), file);
                closeDialog();
            });
            binding.renameView.setOnClickListener(view -> {
                if (file.isFile()) {
                    FileTools.renameFileListener(getContext(), endTask, file, mFileSuffix == null ? file.getName().substring(file.getName().lastIndexOf('.')) : mFileSuffix);
                } else if (file.isDirectory()) {
                    FileTools.renameFileListener(getContext(), endTask, file);
                }
                closeDialog();
            });

            setButtonClickable(filesButton.share, binding.shareView);
            setButtonClickable(filesButton.rename, binding.renameView);
        } else {
            //多选模式禁止使用分享、重命名
            setButtonClickable(false, binding.shareView);
            setButtonClickable(false, binding.renameView);
        }

        setDialogTexts(filesButton, mSelectedFiles.get(0));

        setButtonClickable(filesButton.delete, binding.deleteView);
        setButtonClickable(filesButton.copy, binding.copyView);
        setButtonClickable(filesButton.move, binding.moveView);
        setButtonClickable(filesButton.more, binding.moreView);
    }

    private void setDialogTexts(FilesButton filesButton, File file) {
        if (filesButton.titleText != null) binding.titleView.setText(filesButton.titleText);
        if (filesButton.messageText != null) binding.messageView.setText(filesButton.messageText);
        if (filesButton.moreButtonText != null) binding.moreTextView.setText(filesButton.moreButtonText);
        if (file != null && file.isDirectory())
            binding.titleView.setText(getContext().getString(R.string.file_folder_tips));
    }

    private void closeDialog() {
        FilesDialog.this.dismiss();
    }

    //此方法要在设置点击事件之后调用，否则禁用按钮后按钮仍然能够点击
    private void setButtonClickable(boolean clickable, RelativeLayout button) {
        button.setClickable(clickable);
        button.setAlpha(clickable ? 1f : 0.5f);
    }

    public void setCopyButtonClick(OnCopyButtonClickListener click) {
        this.mCopyClick = click;
    }

    public void setMoreButtonClick(OnMoreButtonClickListener click) {
        this.mMoreClick = click;
    }

    public void setFileSuffix(String suffixes) {
        this.mFileSuffix = suffixes;
    }

    @Override
    public Window onInit() {
        return getWindow();
    }

    public interface OnCopyButtonClickListener {
        void onButtonClick();
    }

    public interface OnMoreButtonClickListener {
        void onButtonClick();
    }

    public static class FilesButton {
        private boolean copy, move, share, rename, delete, more;
        private String titleText, messageText, moreButtonText;

        public void setButtonVisibility(boolean copy, boolean move, boolean shareButton, boolean renameButton, boolean deleteButton, boolean moreButton) {
            this.copy = copy;
            this.move = move;
            this.share = shareButton;
            this.rename = renameButton;
            this.delete = deleteButton;
            this.more = moreButton;
        }

        public void setDialogText(String titleText, String messageText, String moreButtonText) {
            this.titleText = titleText;
            this.messageText = messageText;
            this.moreButtonText = moreButtonText;
        }

        public void setTitleText(String titleText) {
            this.titleText = titleText;
        }

        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }

        public void setMoreButtonText(String moreButtonText) {
            this.moreButtonText = moreButtonText;
        }
    }
}
