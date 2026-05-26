package com.craftstudio.launcher.multirt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.craftstudio.launcher.R;

public class MultiRTConfigDialog {
    private AlertDialog mDialog;
    private RecyclerView mDialogView;

    /**
     * Show the dialog, refreshes the adapter data before showing it
     */
    public void show() {
        refresh();
        mDialog.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    //only used to completely refresh the list, it is necessary
    public void refresh() {
        RecyclerView.Adapter<?> adapter = mDialogView.getAdapter();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /**
     * Build the dialog behavior and style
     */
    public void prepare(Context activity, ActivityResultLauncher<Object> installJvmLauncher) {
        mDialogView = new RecyclerView(activity);
        mDialogView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        RTRecyclerViewAdapter adapter = new RTRecyclerViewAdapter(MultiRTUtils.getRuntimes());
        mDialogView.setAdapter(adapter);

        mDialog = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                .setTitle(R.string.multirt_config_title)
                .setView(mDialogView)
                .setPositiveButton(R.string.multirt_import, (dialog, which) -> installJvmLauncher.launch(null))
                .setNeutralButton(R.string.multirt_delete_runtime, null)
                .create();

        // Custom button behavior without dismiss
        mDialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(view -> {
                boolean isEditing = !adapter.getIsEditing();
                adapter.setIsEditing(isEditing);
                button.setText(isEditing ? R.string.multirt_swap_setdefault : R.string.multirt_delete_runtime);
            });
        });
    }
}
