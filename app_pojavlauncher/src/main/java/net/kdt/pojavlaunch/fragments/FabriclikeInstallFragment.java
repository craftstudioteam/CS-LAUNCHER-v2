package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.animation.LayoutAnimationController;
import android.view.animation.AnimationUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.modloaders.FabriclikeDownloadTask;
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.FabricVersion;
import net.kdt.pojavlaunch.modloaders.FabricVersionAdapter;
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Future;

public abstract class FabriclikeInstallFragment extends Fragment implements ModloaderDownloadListener, CompoundButton.OnCheckedChangeListener {
    private final FabriclikeUtils mFabriclikeUtils;
    private final String mExtraTag;
    private Spinner mGameVersionSpinner;
    private FabricVersion[] mGameVersionArray;
    private Future<?> mGameVersionFuture;
    private String mSelectedGameVersion;
    private Spinner mLoaderVersionSpinner;
    private FabricVersion[] mLoaderVersionArray;
    private Future<?> mLoaderVersionFuture;
    private String mSelectedLoaderVersion;
    private ProgressBar mProgressBar;
    private Button mStartButton;
    private View mRetryView;
    private CheckBox mOnlyStableCheckbox;
    private int mSelectedLoaderPosition = -1;
    private int mSelectedGamePosition = -1;

    // UI references
    private android.widget.ViewFlipper mStepFlipper;
    private ListView mGameVerList;
    private ListView mLoaderVerList;
    private FrameLayout mInstallOverlay;
    private FrameLayout mSuccessOverlay;
    private TextView mInstallStatusTitle;
    private TextView mInstallStatusSubtitle;
    private ProgressBar mDeterminateProgress;
    private Button mSuccessDoneButton;

    protected FabriclikeInstallFragment(FabriclikeUtils mFabriclikeUtils, String mFragmentTag) {
        super(R.layout.fragment_fabric_install);
        this.mFabriclikeUtils = mFabriclikeUtils;
        this.mExtraTag = mFragmentTag + "_proxy";
    }

    private boolean isFragmentUiAvailable() {
        return isAdded() && getContext() != null && getActivity() != null && !isRemoving() && !isDetached();
    }

    private void applyListAnimations(android.widget.ListView listView) {
        Context context = getContext();
        if (!isFragmentUiAvailable() || context == null || listView == null || listView.getAdapter() == null) return;
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
            context, R.anim.list_item_enter);
        listView.setLayoutAnimation(controller);
        listView.scheduleLayoutAnimation();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Install button
        mStartButton = view.findViewById(R.id.fabric_installer_start_button);
        mStartButton.setOnClickListener(this::onClickStart);
        mStartButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                        .setInterpolator(new FastOutSlowInInterpolator()).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120)
                        .setInterpolator(new FastOutSlowInInterpolator()).start();
                    break;
            }
            return false;
        });

        // Hidden spinners (preserve backend logic)
        mGameVersionSpinner = view.findViewById(R.id.fabric_installer_game_ver_spinner);
        mGameVersionSpinner.setOnItemSelectedListener(new GameVersionSelectedListener());
        mLoaderVersionSpinner = view.findViewById(R.id.fabric_installer_loader_ver_spinner);
        mLoaderVersionSpinner.setOnItemSelectedListener(new LoaderVersionSelectedListener());

        mProgressBar = view.findViewById(R.id.fabric_installer_progress_bar);
        mRetryView = view.findViewById(R.id.fabric_installer_retry_layout);
        mOnlyStableCheckbox = view.findViewById(R.id.fabric_installer_only_stable_checkbox);
        mOnlyStableCheckbox.setOnCheckedChangeListener(this);
        view.findViewById(R.id.fabric_installer_retry_button).setOnClickListener(this::onClickRetry);

        // ViewFlipper and lists
        mStepFlipper = view.findViewById(R.id.fabric_step_flipper);
        mGameVerList = view.findViewById(R.id.fabric_game_ver_list);
        mLoaderVerList = view.findViewById(R.id.fabric_loader_ver_list);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mGameVerList.setNestedScrollingEnabled(true);
            mLoaderVerList.setNestedScrollingEnabled(true);
        }

        // Overlays
        mInstallOverlay = view.findViewById(R.id.fabric_install_overlay);
        mSuccessOverlay = view.findViewById(R.id.fabric_success_overlay);
        mInstallStatusTitle = view.findViewById(R.id.fabric_install_status_title);
        mInstallStatusSubtitle = view.findViewById(R.id.fabric_install_status_subtitle);
        mDeterminateProgress = view.findViewById(R.id.fabric_install_determinate_progress);
        mSuccessDoneButton = view.findViewById(R.id.fabric_success_done_button);
        mSuccessDoneButton.setOnClickListener(v -> onSuccessDone());

        // Game version list click
        mGameVerList.setOnItemClickListener((parent, v, position, id) -> {
            if (!isFragmentUiAvailable()) return;
            FabricVersion selected = (FabricVersion) parent.getItemAtPosition(position);
            if (selected == null) return;

            mSelectedGamePosition = position;
            ArrayAdapter<FabricVersion> spinnerAdapter = (ArrayAdapter<FabricVersion>) mGameVersionSpinner.getAdapter();
            if (spinnerAdapter == null) return;
            for (int i = 0; i < spinnerAdapter.getCount(); i++) {
                if (spinnerAdapter.getItem(i) == selected) {
                    mGameVersionSpinner.setSelection(i);
                    break;
                }
            }

            TextView badge = view.findViewById(R.id.fabric_installer_label_loader_ver);
            if (badge != null) badge.setText(selected.version);

            Context context = getContext();
            if (!isFragmentUiAvailable() || context == null) return;
            mStepFlipper.setInAnimation(context, R.anim.screen_slide_in);
            mStepFlipper.setOutAnimation(context, R.anim.screen_slide_out);
            mStepFlipper.setDisplayedChild(1);

            // Reset loader selection when game version changes
            mSelectedLoaderPosition = -1;
            mSelectedLoaderVersion = null;
            if (mStartButton != null) {
                mStartButton.setEnabled(false);
            }
        });

        // Loader version list click
        mLoaderVerList.setOnItemClickListener((parent, v, position, id) -> {
            if (!isFragmentUiAvailable()) return;
            FabricVersion selected = (FabricVersion) parent.getItemAtPosition(position);
            if (selected == null) return;

            mSelectedLoaderPosition = position;
            mSelectedLoaderVersion = selected.version;

            ArrayAdapter<FabricVersion> spinnerAdapter = (ArrayAdapter<FabricVersion>) mLoaderVersionSpinner.getAdapter();
            if (spinnerAdapter != null) {
                for (int i = 0; i < spinnerAdapter.getCount(); i++) {
                    if (spinnerAdapter.getItem(i) == selected) {
                        mLoaderVersionSpinner.setSelection(i);
                        break;
                    }
                }
            }

            // Update list selection visuals
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child != null) {
                    child.setSelected(i == position);
                }
            }

            mStartButton.setEnabled(true);
            mStartButton.animate().alpha(1f).setDuration(150).start();
        });

        // Back button
        view.findViewById(R.id.fabric_step2_back_btn).setOnClickListener(v -> {
            Context context = getContext();
            if (!isFragmentUiAvailable() || context == null) return;
            mStartButton.clearAnimation();
            mStepFlipper.setInAnimation(context, R.anim.screen_slide_in);
            mStepFlipper.setOutAnimation(context, R.anim.screen_slide_out);
            mStepFlipper.setDisplayedChild(0);
        });
        view.findViewById(R.id.fabric_step3_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        // Proxy restore
        ModloaderListenerProxy proxy = getListenerProxy();
        if(proxy != null) {
            mStartButton.setEnabled(false);
            proxy.attachListener(this);
        }
        updateGameVersions();
    }

    @Override
    public void onStop() {
        cancelFutureChecked(mGameVersionFuture);
        cancelFutureChecked(mLoaderVersionFuture);
        ModloaderListenerProxy proxy = getListenerProxy();
        if(proxy != null) {
            proxy.detachListener();
        }
        super.onStop();
    }

    private void onClickStart(View v) {
        if (!isFragmentUiAvailable() || mSelectedGameVersion == null || mSelectedLoaderVersion == null) return;
        if(ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(v.getContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }

        // Show install overlay
        showInstallOverlay();

        ModloaderListenerProxy proxy = new ModloaderListenerProxy();
        FabriclikeDownloadTask fabricDownloadTask = new FabriclikeDownloadTask(proxy, mFabriclikeUtils,
                mSelectedGameVersion, mSelectedLoaderVersion, true);
        proxy.attachListener(this);
        setListenerProxy(proxy);
        mStartButton.setEnabled(false);
        new Thread(fabricDownloadTask).start();
    }

    private void showInstallOverlay() {
        if (mInstallOverlay == null || !isFragmentUiAvailable()) return;
        mInstallOverlay.setAlpha(0f);
        mInstallOverlay.setVisibility(View.VISIBLE);
        mInstallOverlay.animate().alpha(1f).setDuration(200).start();
        updateInstallStatus("Installing " + mFabriclikeUtils.getName() + "...", "Downloading loader metadata");
    }

    private void hideInstallOverlay() {
        if (mInstallOverlay == null) return;
        mInstallOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> mInstallOverlay.setVisibility(View.GONE)).start();
    }

    private void updateInstallStatus(String title, String subtitle) {
        if (mInstallStatusTitle != null) mInstallStatusTitle.setText(title);
        if (mInstallStatusSubtitle != null) mInstallStatusSubtitle.setText(subtitle);
    }

    private void showSuccessOverlay() {
        if (mSuccessOverlay == null || !isFragmentUiAvailable()) return;
        hideInstallOverlay();
        mSuccessOverlay.setAlpha(0f);
        mSuccessOverlay.setVisibility(View.VISIBLE);
        mSuccessOverlay.animate().alpha(1f).setDuration(250).start();

        TextView msg = mSuccessOverlay.findViewById(R.id.fabric_success_message);
        if (msg != null && mSelectedGameVersion != null && mSelectedLoaderVersion != null) {
            msg.setText(mFabriclikeUtils.getName() + " " + mSelectedLoaderVersion + " for Minecraft " + mSelectedGameVersion + " installed successfully.");
        }
    }

    private void onSuccessDone() {
        if (!isFragmentUiAvailable()) return;
        getParentFragmentManager().popBackStackImmediate();
    }

    private void onClickRetry(View v) {
        if (!isFragmentUiAvailable()) return;
        mStartButton.setEnabled(false);
        mRetryView.setVisibility(View.GONE);
        mLoaderVersionSpinner.setAdapter(null);
        if(mGameVersionArray == null) {
            mGameVersionSpinner.setAdapter(null);
            updateGameVersions();
            return;
        }
        updateLoaderVersions();
    }

    private void restoreUiState() {
        if (!isFragmentUiAvailable()) return;
        hideInstallOverlay();
        mStartButton.setVisibility(View.VISIBLE);
        mStartButton.setEnabled(mSelectedLoaderVersion != null);
        mStartButton.animate().alpha(1f).translationY(0f).setDuration(200).start();
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDownloadFinished(File downloadedFile) {
        Tools.runOnUiThread(()->{
            if (!isFragmentUiAvailable()) return;
            ModloaderListenerProxy proxy = getListenerProxy();
            if (proxy != null) proxy.detachListener();
            setListenerProxy(null);
            showSuccessOverlay();
        });
    }

    @Override
    public void onDataNotAvailable() {
        Tools.runOnUiThread(()->{
            Context context = getContext();
            if (!isFragmentUiAvailable() || context == null) return;
            ModloaderListenerProxy proxy = getListenerProxy();
            if (proxy != null) proxy.detachListener();
            setListenerProxy(null);
            restoreUiState();
            Tools.dialog(context,
                    context.getString(R.string.global_error),
                    context.getString(R.string.fabric_dl_cant_read_meta, mFabriclikeUtils.getName()));
        });
    }

    @Override
    public void onDownloadError(Exception e) {
        Tools.runOnUiThread(()-> {
            Context context = getContext();
            if (!isFragmentUiAvailable() || context == null) return;
            ModloaderListenerProxy proxy = getListenerProxy();
            if (proxy != null) proxy.detachListener();
            setListenerProxy(null);
            restoreUiState();
            Tools.showError(context, e);
        });
    }

    private void cancelFutureChecked(Future<?> future) {
        if(future != null && !future.isCancelled()) future.cancel(true);
    }

    private void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mStartButton.setEnabled(false);
    }

    private void stopLoading() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Nullable
    private ArrayAdapter<FabricVersion> createAdapter(FabricVersion[] fabricVersions, boolean onlyStable, boolean showBadges) {
        Context context = getContext();
        if (!isFragmentUiAvailable() || context == null || fabricVersions == null) return null;
        ArrayList<FabricVersion> filteredVersions = new ArrayList<>(fabricVersions.length);
        for(FabricVersion fabricVersion : fabricVersions) {
            if(!onlyStable || fabricVersion.stable) filteredVersions.add(fabricVersion);
        }
        filteredVersions.trimToSize();
        int layoutRes = showBadges ? R.layout.list_item_fabric_version : R.layout.item_fabric_version;
        return new FabricVersionAdapter(context, layoutRes, filteredVersions, showBadges);
    }

    private void onException(Future<?> myFuture, Exception e) {
        Tools.runOnUiThread(()->{
            Context context = getContext();
            if(myFuture.isCancelled() || !isFragmentUiAvailable() || context == null) return;
            stopLoading();
            if(e != null) Tools.showError(context, e);
            mRetryView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (!isFragmentUiAvailable()) return;
        updateGameSpinner();
        updateLoaderSpinner();
    }

    class LoaderVersionSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (!isFragmentUiAvailable() || adapterView.getAdapter() == null) return;
            mSelectedLoaderVersion = ((FabricVersion) adapterView.getAdapter().getItem(i)).version;
            mStartButton.setEnabled(mSelectedGameVersion != null && mSelectedLoaderVersion != null);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            if (!isFragmentUiAvailable()) return;
            mSelectedLoaderVersion = null;
            mStartButton.setEnabled(false);
        }
    }

    class LoadLoaderVersionsTask implements SelfReferencingFuture.FutureInterface {
        @Override
        public void run(Future<?> myFuture) {
            Log.i("LoadLoaderVersions", "Starting...");
            try {
                mLoaderVersionArray = mFabriclikeUtils.downloadLoaderVersions(mSelectedGameVersion);
                if(mLoaderVersionArray != null) onFinished(myFuture);
                else onException(myFuture, null);
            }catch (IOException e) {
                onException(myFuture, e);
            }
        }
        private void onFinished(Future<?> myFuture) {
            Tools.runOnUiThread(()->{
                if(myFuture.isCancelled() || !isFragmentUiAvailable()) return;
                stopLoading();
                updateLoaderSpinner();
            });
        }
    }

    private void updateLoaderVersions() {
        if (!isFragmentUiAvailable()) return;
        startLoading();
        mLoaderVersionFuture = new SelfReferencingFuture(new LoadLoaderVersionsTask()).startOnExecutor(PojavApplication.sExecutorService);
    }

    private void updateLoaderSpinner() {
        if(!isFragmentUiAvailable() || mLoaderVersionArray == null) return;
        mLoaderVersionSpinner.setAlpha(0f);
        ArrayAdapter<FabricVersion> adapter = createAdapter(mLoaderVersionArray, false, true);
        if (adapter == null) return;
        mLoaderVersionSpinner.setAdapter(adapter);
        mLoaderVersionSpinner.animate().alpha(1f).setDuration(250).start();
        if (mLoaderVerList != null) {
            mLoaderVerList.setAdapter(adapter);
            applyListAnimations(mLoaderVerList);
        }

        // Auto-select latest stable loader if available, else first item
        if (adapter.getCount() > 0) {
            int selectedIndex = 0;
            for (int i = 0; i < adapter.getCount(); i++) {
                FabricVersion ver = adapter.getItem(i);
                if (ver != null && ver.stable) {
                    selectedIndex = i;
                    break;
                }
            }
            mLoaderVersionSpinner.setSelection(selectedIndex);
            mSelectedLoaderVersion = adapter.getItem(selectedIndex).version;
            mStartButton.setEnabled(mSelectedGameVersion != null);
        } else {
            mSelectedLoaderVersion = null;
            mStartButton.setEnabled(false);
        }
    }

    class GameVersionSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (!isFragmentUiAvailable() || adapterView.getAdapter() == null) return;
            mSelectedGameVersion = ((FabricVersion) adapterView.getAdapter().getItem(i)).version;
            cancelFutureChecked(mLoaderVersionFuture);
            updateLoaderVersions();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            if (!isFragmentUiAvailable()) return;
            mSelectedGameVersion = null;
            if(mLoaderVersionFuture != null) mLoaderVersionFuture.cancel(true);
            adapterView.setAdapter(null);
        }

    }

    class LoadGameVersionsTask implements SelfReferencingFuture.FutureInterface {
        @Override
        public void run(Future<?> myFuture) {
            try {
                mGameVersionArray = mFabriclikeUtils.downloadGameVersions();
                if(mGameVersionArray != null) onFinished(myFuture);
                else onException(myFuture, null);
            }catch (IOException e) {
                onException(myFuture, e);
            }
        }
        private void onFinished(Future<?> myFuture) {
            Tools.runOnUiThread(()->{
                if(myFuture.isCancelled() || !isFragmentUiAvailable()) return;
                stopLoading();
                updateGameSpinner();
            });
        }
    }

    private void updateGameVersions() {
        if (!isFragmentUiAvailable()) return;
        startLoading();
        mGameVersionFuture = new SelfReferencingFuture(new LoadGameVersionsTask()).startOnExecutor(PojavApplication.sExecutorService);
    }

    private void updateGameSpinner() {
        if(!isFragmentUiAvailable() || mGameVersionArray == null) return;
        mGameVersionSpinner.setAlpha(0f);
        ArrayAdapter<FabricVersion> adapter = createAdapter(mGameVersionArray, mOnlyStableCheckbox.isChecked(), false);
        if (adapter == null) return;
        mGameVersionSpinner.setAdapter(adapter);
        if (adapter.getCount() == 0) {
            mSelectedGameVersion = null;
            mStartButton.setEnabled(false);
        }
        mGameVersionSpinner.animate().alpha(1f).setDuration(250).start();
        if (mGameVerList != null) {
            mGameVerList.setAdapter(adapter);
            applyListAnimations(mGameVerList);
        }
    }

    private ModloaderListenerProxy getListenerProxy() {
        return (ModloaderListenerProxy) ExtraCore.getValue(mExtraTag);
    }
    private void setListenerProxy(ModloaderListenerProxy listenerProxy) {
        ExtraCore.setValue(mExtraTag, listenerProxy);
    }
}
