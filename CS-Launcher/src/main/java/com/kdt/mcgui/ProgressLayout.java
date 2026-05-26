package com.kdt.mcgui;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.feature.log.Logging;

import com.craftstudio.launcher.progresskeeper.ProgressKeeper;
import com.craftstudio.launcher.progresskeeper.ProgressListener;
import com.craftstudio.launcher.progresskeeper.TaskCountListener;

import java.util.HashMap;
import java.util.Map;


/** Class staring at specific values and automatically show something if the progress is present
 * Since progress is posted in a specific way, The packing/unpacking is handheld by the class
 * This class relies on ExtraCore for its behavior.
 */
public class ProgressLayout extends ConstraintLayout implements View.OnClickListener, TaskCountListener{
    public static final String UNPACK_RUNTIME = "unpack_runtime";
    public static final String DOWNLOAD_MINECRAFT = "download_minecraft";
    public static final String DOWNLOAD_VERSION_LIST = "download_verlist";
    public static final String LOGIN_ACCOUNT = "login_account";
    public static final String INSTALL_RESOURCE = "install_resource";
    public static final String CHECKING_MODS = "checking_mods";

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

    private final Map<String, LayoutProgressListener> mMap = new HashMap<>();
    private LinearLayout mLinearLayout;
    private TextView mTaskNumberDisplayer;
    private ImageView mFlipArrow;

    public void observe(String progressKey) {
        mMap.put(progressKey, new LayoutProgressListener(progressKey));
    }

    public void unObserve(String progressKey) {
        mMap.remove(progressKey);
    }

    public void cleanUpObservers() {
        mMap.forEach(ProgressKeeper::removeListener);
    }

    public boolean hasProcesses(){
        return ProgressKeeper.getTaskCount() > 0;
    }


    private void init(){
        inflate(getContext(), R.layout.view_progress, this);
        mLinearLayout = findViewById(R.id.progress_linear_layout);
        mTaskNumberDisplayer = findViewById(R.id.progress_textview);
        mFlipArrow = findViewById(R.id.progress_flip_arrow);
        setOnClickListener(this);
    }

    /** Update the text and progress content */
    public static void setProgress(String progressKey, int progress, @StringRes int resource, Object... message){
        ProgressKeeper.submitProgress(progressKey, progress, resource, message);
    }

    /** Update the text and progress content */
    public static void clearProgress(String progressKey) {
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
                Logging.i("ProgressLayout", "onProgressStarted");
                textView.setProgress(0);
                textView.setText("");
                mLinearLayout.addView(textView, params);
            });
        }

        @Override
        public void onProgressUpdated(int progress, int resid, Object... va) {
            post(() -> {
                textView.setProgress(progress);
                try {
                    if (resid != -1) textView.setText(getContext().getString(resid, va));
                    else if (va.length > 0 && va[0] != null) textView.setText((String) va[0]);
                    else textView.setText("");
                } catch (Throwable ignored) {
                }
            });
        }

        @Override
        public void onProgressEnded() {
            post(()-> mLinearLayout.removeView(textView));
        }
    }
}
