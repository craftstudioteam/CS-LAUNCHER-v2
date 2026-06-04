package com.kdt.mcgui;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.animation.ObjectAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;
import androidx.constraintlayout.widget.ConstraintLayout;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressService;

import java.util.ArrayList;


/** Class staring at specific values and automatically show something if the progress is present
 * Since progress is posted in a specific way, The packing/unpacking is handheld by the class
 *
 * This class relies on ExtraCore for its behavior.
 */
public class ProgressLayout extends ConstraintLayout implements View.OnClickListener, TaskCountListener{
    public static final String UNPACK_RUNTIME = "unpack_runtime";
    public static final String DOWNLOAD_MINECRAFT = "download_minecraft";
    public static final String DOWNLOAD_VERSION_LIST = "download_verlist";
    public static final String AUTHENTICATE_MICROSOFT = "authenticate_microsoft";
    public static final String INSTALL_MODPACK = "install_modpack";
    public static final String EXTRACT_COMPONENTS = "extract_components";
    public static final String EXTRACT_SINGLE_FILES = "extract_single_files";

    public ProgressLayout(@NonNull Context context) {
        super(context);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private final ArrayList<LayoutProgressListener> mMap = new ArrayList<>();
    private LinearLayout mLinearLayout;
    private TextView mTaskNumberDisplayer;
    private ImageView mFlipArrow;



    public void observe(String progressKey){
        mMap.add(new LayoutProgressListener(progressKey));
    }

    public void cleanUpObservers() {
        for(LayoutProgressListener progressListener : mMap) {
            ProgressKeeper.removeListener(progressListener.progressKey, progressListener);
        }
    }

    public boolean hasProcesses(){
        return ProgressKeeper.getTaskCount() > 0;
    }


    private View mProgressSpinner;
    private Runnable mRotationRunnable;
    private void init(){
        inflate(getContext(), R.layout.view_progress, this);
        mLinearLayout = findViewById(R.id.progress_linear_layout);
        mTaskNumberDisplayer = findViewById(R.id.progress_textview);
        mFlipArrow = findViewById(R.id.progress_flip_arrow);
        mProgressSpinner = findViewById(R.id.progress_generic_progressbar);
        setBackgroundColor(getResources().getColor(R.color.background_bottom_bar));
        setOnClickListener(this);

        // Cinematic infinite rotation animation for the circular loader
        mRotationRunnable = new Runnable() {
            @Override
            public void run() {
                if (mProgressSpinner != null) {
                    mProgressSpinner.animate()
                            .rotation(mProgressSpinner.getRotation() + 360f)
                            .setDuration(1200)
                            .setInterpolator(null)
                            .withEndAction(mRotationRunnable)
                            .start();
                }
            }
        };
        mRotationRunnable.run();
    }
    public static void setProgress(String progressKey, int progress){
        ProgressKeeper.submitProgress(progressKey, progress, -1, (Object)null);
    }

    /** Update the text and progress content */
    public static void setProgress(String progressKey, int progress, @StringRes int resource, Object... message){
        ProgressKeeper.submitProgress(progressKey, progress, resource, message);
    }

    /** Update the text and progress content */
    public static void setProgress(String progressKey, int progress, String message){
        setProgress(progressKey,progress, -1, message);
    }

    /** Update the text and progress content */
    public static void clearProgress(String progressKey){
        setProgress(progressKey, -1, -1);
    }

    @Override
    public void onClick(View v) {
        mLinearLayout.setVisibility(mLinearLayout.getVisibility() == GONE ? VISIBLE : GONE);
        mFlipArrow.setRotation(mLinearLayout.getVisibility() == GONE? 0 : 180);
    }

    @Override
    public void onUpdateTaskCount(int tc) {
        post(()->{
            if(tc > 0) {
                mTaskNumberDisplayer.setText(getContext().getString(R.string.progresslayout_tasks_in_progress, tc));
                setVisibility(VISIBLE);
            }else
                setVisibility(GONE);
        });
    }

    class LayoutProgressListener implements ProgressListener {
        final String progressKey;
        final TextProgressBar textView;
        final LinearLayout.LayoutParams params;
        public LayoutProgressListener(String progressKey) {
            this.progressKey = progressKey;
            textView = new TextProgressBar(getContext());
            textView.setTextPadding(getContext().getResources().getDimensionPixelOffset(R.dimen._6sdp));
            params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen._20sdp));
            params.bottomMargin = getResources().getDimensionPixelOffset(R.dimen._6sdp);
            ProgressKeeper.addListener(progressKey, this);
        }
        @Override
        public void onProgressStarted() {
            post(()-> {
                Log.i("ProgressLayout", "onProgressStarted");
                mLinearLayout.addView(textView, params);
            });
        }

        @Override
        public void onProgressUpdated(int progress, int resid, Object... va) {
            post(()-> {
                // Smooth transition at locked 60 FPS via ObjectAnimator
                int current = textView.getProgress();
                if (progress != current && progress >= 0) {
                    ObjectAnimator.ofInt(textView, "progress", current, progress)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                } else {
                    textView.setProgress(progress);
                }
                if(resid != -1) textView.setText(getContext().getString(resid, va));
                else if(va.length > 0 && va[0] != null)textView.setText((String)va[0]);
                else textView.setText("");
            });
        }

        @Override
        public void onProgressEnded() {
            post(()-> {
                mLinearLayout.removeView(textView);
            });
        }
    }
}
