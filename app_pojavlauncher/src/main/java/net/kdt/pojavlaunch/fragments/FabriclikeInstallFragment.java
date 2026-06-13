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
    protected FabriclikeInstallFragment(FabriclikeUtils mFabriclikeUtils, String mFragmentTag) {
        super(R.layout.fragment_fabric_install);
        this.mFabriclikeUtils = mFabriclikeUtils;
        this.mExtraTag = mFragmentTag + "_proxy";
    }

    private android.widget.ViewFlipper mStepFlipper;
    private android.widget.ListView mGameVerList;
    private android.widget.ListView mLoaderVerList;

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
        mStartButton = view.findViewById(R.id.fabric_installer_start_button);
        mStartButton.setOnClickListener(this::onClickStart);
        mStartButton.setOnTouchListener((v, event) -> {
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
        mGameVersionSpinner = view.findViewById(R.id.fabric_installer_game_ver_spinner);
        mGameVersionSpinner.setOnItemSelectedListener(new GameVersionSelectedListener());
        mLoaderVersionSpinner = view.findViewById(R.id.fabric_installer_loader_ver_spinner);
        mLoaderVersionSpinner.setOnItemSelectedListener(new LoaderVersionSelectedListener());
        mProgressBar = view.findViewById(R.id.fabric_installer_progress_bar);
        mRetryView = view.findViewById(R.id.fabric_installer_retry_layout);
        mOnlyStableCheckbox = view.findViewById(R.id.fabric_installer_only_stable_checkbox);
        mOnlyStableCheckbox.setOnCheckedChangeListener(this);
        view.findViewById(R.id.fabric_installer_retry_button).setOnClickListener(this::onClickRetry);
        ((TextView)view.findViewById(R.id.fabric_installer_label_loader_ver)).setText(getString(R.string.fabric_dl_loader_version, mFabriclikeUtils.getName()));
        
        mStepFlipper = view.findViewById(R.id.fabric_step_flipper);
        mGameVerList = view.findViewById(R.id.fabric_game_ver_list);
        mLoaderVerList = view.findViewById(R.id.fabric_loader_ver_list);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mGameVerList.setNestedScrollingEnabled(true);
            mLoaderVerList.setNestedScrollingEnabled(true);
        }

        mGameVerList.setOnItemClickListener((parent, v, position, id) -> {
            if (!isFragmentUiAvailable()) return;
            FabricVersion selected = (FabricVersion) parent.getItemAtPosition(position);
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
            
            android.view.animation.Animation pulseAnim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.pulse_animation);
            mStartButton.startAnimation(pulseAnim);
        });

        mLoaderVerList.setOnItemClickListener((parent, v, position, id) -> {
            if (!isFragmentUiAvailable()) return;
            FabricVersion selected = (FabricVersion) parent.getItemAtPosition(position);
            ArrayAdapter<FabricVersion> spinnerAdapter = (ArrayAdapter<FabricVersion>) mLoaderVersionSpinner.getAdapter();
            if (spinnerAdapter == null) return;
            for (int i = 0; i < spinnerAdapter.getCount(); i++) {
                if (spinnerAdapter.getItem(i) == selected) {
                    mLoaderVersionSpinner.setSelection(i);
                    break;
                }
            }
        });

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
        ModloaderListenerProxy proxy = new ModloaderListenerProxy();
        FabriclikeDownloadTask fabricDownloadTask = new FabriclikeDownloadTask(proxy, mFabriclikeUtils,
                mSelectedGameVersion, mSelectedLoaderVersion, true);
        proxy.attachListener(this);
        setListenerProxy(proxy);
        mStartButton.setEnabled(false);
        mStartButton.animate().alpha(0f).translationY(20f).setDuration(250).withEndAction(() -> mStartButton.setVisibility(View.INVISIBLE)).start();
        mProgressBar.setAlpha(0f);
        mProgressBar.setTranslationY(20f);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.animate().alpha(1f).translationY(0f).setDuration(300).start();
        new Thread(fabricDownloadTask).start();
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
        mStartButton.setVisibility(View.VISIBLE);
        mStartButton.setEnabled(true);
        mStartButton.animate().alpha(1f).translationY(0f).setDuration(250).start();
        mProgressBar.animate().alpha(0f).translationY(20f).setDuration(200).withEndAction(() -> mProgressBar.setVisibility(View.GONE)).start();
    }

    @Override
    public void onDownloadFinished(File downloadedFile) {
        Tools.runOnUiThread(()->{
            if (!isFragmentUiAvailable()) return;
            ModloaderListenerProxy proxy = getListenerProxy();
            if (proxy != null) proxy.detachListener();
            setListenerProxy(null);
            restoreUiState();
            // This works because the due to the fact that we have transitioned here
            // without adding a transaction to the back stack, which caused the previous
            // transaction to be amended (i guess?? thats how the back stack dump looks like)
            // we can get back to the main fragment with just one back stack pop.
            // For some reason that amendment causes the transaction to lose its tag
            // so we cant use the tag here.
            getParentFragmentManager().popBackStackImmediate();
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
        // The "visibility on" is managed by the spinners
    }

    @Nullable
    private ArrayAdapter<FabricVersion> createAdapter(FabricVersion[] fabricVersions, boolean onlyStable) {
        Context context = getContext();
        if (!isFragmentUiAvailable() || context == null || fabricVersions == null) return null;
        ArrayList<FabricVersion> filteredVersions = new ArrayList<>(fabricVersions.length);
        for(FabricVersion fabricVersion : fabricVersions) {
            if(!onlyStable || fabricVersion.stable) filteredVersions.add(fabricVersion);
        }
        filteredVersions.trimToSize();
        return new ArrayAdapter<>(context, R.layout.item_fabric_version, filteredVersions);
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
            mStartButton.setEnabled(mSelectedGameVersion != null);
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
        ArrayAdapter<FabricVersion> adapter = createAdapter(mLoaderVersionArray, false);
        if (adapter == null) return;
        mLoaderVersionSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0 && mSelectedGameVersion != null) {
            mSelectedLoaderVersion = adapter.getItem(0).version;
            mStartButton.setEnabled(true);
        } else {
            mSelectedLoaderVersion = null;
            mStartButton.setEnabled(false);
        }
        mLoaderVersionSpinner.animate().alpha(1f).setDuration(300).start();
        if (mLoaderVerList != null) {
            mLoaderVerList.setAdapter(adapter);
            applyListAnimations(mLoaderVerList);
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
        ArrayAdapter<FabricVersion> adapter = createAdapter(mGameVersionArray, mOnlyStableCheckbox.isChecked());
        if (adapter == null) return;
        mGameVersionSpinner.setAdapter(adapter);
        if (adapter.getCount() == 0) {
            mSelectedGameVersion = null;
            mStartButton.setEnabled(false);
        }
        mGameVersionSpinner.animate().alpha(1f).setDuration(300).start();
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
