package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.animation.LayoutAnimationController;
import android.view.animation.AnimationUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;

import java.io.File;
import java.io.IOException;

public abstract class ModVersionListFragment<T> extends Fragment implements Runnable, View.OnClickListener, ExpandableListView.OnChildClickListener, ModloaderDownloadListener {
    private final String mExtraTag;
    private ExpandableListView mExpandableListView;
    private ProgressBar mProgressBar;
    private LayoutInflater mInflater;
    private View mRetryView;

    public ModVersionListFragment(String mFragmentTag) {
        super(R.layout.fragment_mod_version_list);
        this.mExtraTag = mFragmentTag + "_proxy";
    }

    private void applyListAnimations(ExpandableListView listView) {
        Context context = getContext();
        if (listView == null || context == null || !isAdded()) return;
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
            context, R.anim.list_item_enter);
        listView.setLayoutAnimation(controller);
        listView.scheduleLayoutAnimation();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView)view.findViewById(R.id.title_textview)).setText(getTitleText());
        mProgressBar = view.findViewById(R.id.mod_dl_list_progress);
        mExpandableListView = view.findViewById(R.id.mod_dl_expandable_version_list);
        mExpandableListView.setOnChildClickListener(this);
        mRetryView = view.findViewById(R.id.mod_dl_retry_layout);
        view.findViewById(R.id.forge_installer_retry_button).setOnClickListener(this);

        android.widget.ViewFlipper stepFlipper = view.findViewById(R.id.mod_dl_step_flipper);
        android.widget.Button startButton = view.findViewById(R.id.mod_dl_start_button);
        if (startButton != null) {
            startButton.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                            .setInterpolator(new FastOutSlowInInterpolator()).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new FastOutSlowInInterpolator()).start();
                        break;
                }
                return false;
            });
            startButton.setOnClickListener(v -> {
                Object selectedVersion = net.kdt.pojavlaunch.extra.ExtraCore.getValue("SELECTED_MOD_VERSION_" + mExtraTag);
                if(selectedVersion == null) return;
                startButton.setEnabled(false);
                startButton.clearAnimation();
                ModloaderListenerProxy proxy = new ModloaderListenerProxy();
                Runnable downloadTask = createDownloadTask(selectedVersion, proxy);
                setTaskProxyValue(proxy);
                proxy.attachListener(this);
                mExpandableListView.setEnabled(false);
                new Thread(downloadTask).start();
            });
        }
        
        View backBtn = view.findViewById(R.id.mod_dl_step2_back_btn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                if(startButton != null) startButton.clearAnimation();
                if(stepFlipper != null) {
                    stepFlipper.setInAnimation(requireContext(), R.anim.screen_slide_in);
                    stepFlipper.setOutAnimation(requireContext(), R.anim.screen_slide_out);
                    stepFlipper.setDisplayedChild(0);
                }
            });
        }

        ModloaderListenerProxy taskProxy = getTaskProxy();
        if(taskProxy != null) {
            mExpandableListView.setEnabled(false);
            if(startButton != null) startButton.setEnabled(false);
            if(stepFlipper != null) stepFlipper.setDisplayedChild(1);
            taskProxy.attachListener(this);
        }
        new Thread(this).start();
    }

    @Override
    public void onStop() {
        ModloaderListenerProxy taskProxy = getTaskProxy();
        if(taskProxy != null) taskProxy.detachListener();
        super.onStop();
    }

    @Override
    public void run() {
        try {
            T versions = loadVersionList();
            Tools.runOnUiThread(()->{
                if(versions != null) {
                    mExpandableListView.setAdapter(createAdapter(versions, mInflater));
                    applyListAnimations(mExpandableListView);
                }else{
                    mRetryView.setVisibility(View.VISIBLE);
                }
                mProgressBar.setVisibility(View.GONE);
            });
        }catch (IOException e) {
            Tools.runOnUiThread(()-> {
                if (getContext() != null) {
                    Tools.showError(getContext(), e);
                    mRetryView.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        mRetryView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        new Thread(this).start();
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
        if(ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(expandableListView.getContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return true;
        }
        Object forgeVersion = expandableListView.getExpandableListAdapter().getChild(i, i1);
        net.kdt.pojavlaunch.extra.ExtraCore.setValue("SELECTED_MOD_VERSION_" + mExtraTag, forgeVersion);

        String versionStr = "";
        if (forgeVersion instanceof net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion) {
            versionStr = ((net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion)forgeVersion).versionName;
        } else {
            versionStr = forgeVersion.toString();
        }

        TextView badge = requireView().findViewById(R.id.mod_dl_label_version);
        if (badge != null) badge.setText(versionStr);

        android.widget.ViewFlipper stepFlipper = requireView().findViewById(R.id.mod_dl_step_flipper);
        if (stepFlipper != null) {
            stepFlipper.setInAnimation(requireContext(), R.anim.screen_slide_in);
            stepFlipper.setOutAnimation(requireContext(), R.anim.screen_slide_out);
            stepFlipper.setDisplayedChild(1);
        }

        android.widget.Button startButton = requireView().findViewById(R.id.mod_dl_start_button);
        if (startButton != null) {
            startButton.setEnabled(true);
            startButton.startAnimation(android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation));
        }
        return true;
    }

    @Override
    public void onDownloadFinished(File downloadedFile) {
        Tools.runOnUiThread(()->{
            Context context = getContext();
            if (context == null || !isAdded()) return;
            getTaskProxy().detachListener();
            deleteTaskProxy();
            mExpandableListView.setEnabled(true);
            // Read the comment in FabricInstallFragment.onDownloadFinished() to see how this works
            getParentFragmentManager().popBackStackImmediate();
            onDownloadFinished(context, downloadedFile);
        });
    }

    @Override
    public void onDataNotAvailable() {
        Tools.runOnUiThread(()->{
            Context context = getContext();
            if (context == null || !isAdded()) return;
            getTaskProxy().detachListener();
            deleteTaskProxy();
            mExpandableListView.setEnabled(true);
            Tools.dialog(context,
                    context.getString(R.string.global_error),
                    context.getString(getNoDataMsg()));
        });
    }

    @Override
    public void onDownloadError(Exception e) {
        Tools.runOnUiThread(()->{
            Context context = getContext();
            if (context == null || !isAdded()) return;
            getTaskProxy().detachListener();
            deleteTaskProxy();
            mExpandableListView.setEnabled(true);
            android.widget.Button startBtn = requireView().findViewById(R.id.mod_dl_start_button);
            if (startBtn != null) {
                startBtn.setEnabled(true);
                startBtn.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.pulse_animation));
            }
            Tools.showError(context, e);
        });
    }

    private void setTaskProxyValue(ModloaderListenerProxy proxy) {
        ExtraCore.setValue(mExtraTag, proxy);
    }
    private void deleteTaskProxy(){
        ExtraCore.removeValue(mExtraTag);
    }

    private ModloaderListenerProxy getTaskProxy() {
        return (ModloaderListenerProxy) ExtraCore.getValue(mExtraTag);
    }

    public abstract int getTitleText();
    public abstract int getNoDataMsg();

    public abstract T loadVersionList() throws IOException;

    public abstract ExpandableListAdapter createAdapter(T versionList, LayoutInflater layoutInflater);
    public abstract Runnable createDownloadTask(Object selectedVersion, ModloaderListenerProxy listenerProxy);
    public abstract void onDownloadFinished(Context context, File downloadedFile);
}
