package com.kdt.mcgui;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache;

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
    private KineticProgressView mKineticProgress;
    private TextView mStatusText;
    private TextView mDetailText;
    private TextView mSpeedText;
    private String mLastProgressingKey;
    private int mLastProgress = 0;
    private String mLastDetailText = "";

    private ProgressBar mProgressBar;
    private TextView mPercentageText;
    private TextView mEtaText;
    private View mDownloadCard;
    private ImageView mProgressIcon;
    private boolean mIsFinishing = false;
    private final Runnable mFadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            ProgressLayout.this.animate()
                .alpha(0f)
                .translationY(ProgressLayout.this.getHeight())
                .setDuration(500)
                .withEndAction(() -> {
                    ProgressLayout.this.setVisibility(GONE);
                    ProgressLayout.this.setAlpha(1f);
                    ProgressLayout.this.setTranslationY(0f);
                    mIsFinishing = false;
                })
                .start();
        }
    };


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


    private void init(){
        inflate(getContext(), R.layout.view_progress, this);
        mLinearLayout = findViewById(R.id.progress_linear_layout);
        mTaskNumberDisplayer = findViewById(R.id.progress_textview);
        mFlipArrow = findViewById(R.id.progress_flip_arrow);
        mKineticProgress = findViewById(R.id.kinetic_progress);
        mStatusText = findViewById(R.id.progress_status_text);
        mDetailText = findViewById(R.id.progress_detail_text);
        mSpeedText = findViewById(R.id.progress_speed_text);

        mProgressBar = findViewById(R.id.progress_horizontal_bar);
        mPercentageText = findViewById(R.id.progress_percentage_text);
        mEtaText = findViewById(R.id.progress_eta_text);
        mDownloadCard = findViewById(R.id.download_card);
        mProgressIcon = findViewById(R.id.progress_icon);

        if (mKineticProgress != null) {
            mKineticProgress.setHideText(true);
        }

        if (mDownloadCard != null) {
            mDownloadCard.setOnClickListener(v -> {
                if (mIsFinishing || ProgressKeeper.getTaskCount() == 0) {
                    removeCallbacks(mFadeOutRunnable);
                    ProgressLayout.this.animate().cancel();
                    ProgressLayout.this.setAlpha(0f);
                    ProgressLayout.this.setVisibility(GONE);
                    ProgressLayout.this.setAlpha(1f);
                    ProgressLayout.this.setTranslationY(0f);
                    mIsFinishing = false;
                } else {
                    ProgressLayout.this.onClick(ProgressLayout.this);
                }
            });
        }

        setBackgroundColor(Color.TRANSPARENT);
        setOnClickListener(this);
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
        if (mIsFinishing || ProgressKeeper.getTaskCount() == 0) {
            removeCallbacks(mFadeOutRunnable);
            this.animate().cancel();
            this.setAlpha(0f);
            this.setVisibility(GONE);
            this.setAlpha(1f);
            this.setTranslationY(0f);
            mIsFinishing = false;
        } else {
            mLinearLayout.setVisibility(mLinearLayout.getVisibility() == GONE ? VISIBLE : GONE);
            mFlipArrow.setRotation(mLinearLayout.getVisibility() == GONE? 0 : 180);
        }
    }

    @Override
    public void onUpdateTaskCount(int tc) {
        post(()->{
            if(tc > 0) {
                removeCallbacks(mFadeOutRunnable);
                mIsFinishing = false;
                setAlpha(1f);
                setTranslationY(0f);
                if (mProgressBar != null) {
                    mProgressBar.setProgressTintList(null);
                }
                if (mStatusText != null) {
                    mStatusText.setTextColor(0xFFFFFFFF);
                }
                if (mPercentageText != null) {
                    mPercentageText.setTextColor(0xFF39FF14);
                }
                mTaskNumberDisplayer.setText(getContext().getString(R.string.progresslayout_tasks_in_progress, tc));
                setVisibility(VISIBLE);
            } else {
                if (getVisibility() == VISIBLE && !mIsFinishing) {
                    mIsFinishing = true;
                    if (mStatusText != null) {
                        mStatusText.setText("✓ Download Complete");
                        mStatusText.setTextColor(0xFF39FF14); // neon green
                    }
                    if (mPercentageText != null) {
                        mPercentageText.setText("100%");
                        mPercentageText.setTextColor(0xFF39FF14); // neon green
                    }
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(100);
                        mProgressBar.setProgressTintList(ColorStateList.valueOf(0xFF39FF14));
                    }
                    if (mDetailText != null) {
                        mDetailText.setVisibility(GONE);
                    }
                    if (mSpeedText != null) {
                        mSpeedText.setVisibility(GONE);
                    }
                    if (mEtaText != null) {
                        mEtaText.setVisibility(GONE);
                    }
                    removeCallbacks(mFadeOutRunnable);
                    postDelayed(mFadeOutRunnable, 2500);
                } else if (getVisibility() != VISIBLE) {
                    setVisibility(GONE);
                }
            }
        });
    }

    private static String formatRemainingTime(double seconds) {
        if (seconds < 0) return "";
        int totalSecs = (int) seconds;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        if (mins > 0) {
            return mins + "m " + secs + "s remaining";
        } else {
            return secs + "s remaining";
        }
    }

    class LayoutProgressListener implements ProgressListener {
        final String progressKey;
        final TextProgressBar textView;
        final LinearLayout.LayoutParams params;
        private ObjectAnimator mProgressAnimator;
        private ValueAnimator mPercentageAnimator;
        private int mTargetProgress = -1;

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
                // Update individual task bar in the expandable list
                int current = textView.getProgress();
                if (progress != current && progress >= 0) {
                    ObjectAnimator anim = ObjectAnimator.ofInt(textView, "progress", current, progress);
                    anim.setDuration(200);
                    anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
                    anim.start();
                } else {
                    textView.setProgress(progress);
                }
                if(resid != -1) textView.setText(getContext().getString(resid, va));
                else if(va != null && va.length > 0 && va[0] != null)textView.setText((String)va[0]);
                else textView.setText("");

                // Update the kinetic progress circle and detail texts
                if (progress >= 0) {
                    mKineticProgress.setProgress(progress);
                    mLastProgress = progress;
                }
                mLastProgressingKey = this.progressKey;

                // --- NEW COMPACT VIEW UPDATES ---
                if (mProgressBar != null && progress >= 0) {
                    int currProgress = mProgressBar.getProgress();
                    if (progress != mTargetProgress) {
                        mTargetProgress = progress;

                        if (mProgressAnimator != null && mProgressAnimator.isRunning()) {
                            mProgressAnimator.cancel();
                        }
                        mProgressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", currProgress, progress);
                        mProgressAnimator.setDuration(250);
                        mProgressAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
                        mProgressAnimator.start();

                        if (mPercentageAnimator != null && mPercentageAnimator.isRunning()) {
                            mPercentageAnimator.cancel();
                        }
                        mPercentageAnimator = ValueAnimator.ofInt(currProgress, progress);
                        mPercentageAnimator.setDuration(250);
                        mPercentageAnimator.addUpdateListener(animation -> {
                            if (mPercentageText != null) {
                                mPercentageText.setText(animation.getAnimatedValue() + "%");
                            }
                        });
                        mPercentageAnimator.start();
                    }

                    // Gently pulse the card on update to feel alive
                    if (mDownloadCard != null) {
                        mDownloadCard.animate().scaleX(1.015f).scaleY(1.015f).setDuration(120).withEndAction(() -> {
                            if (mDownloadCard != null) {
                                mDownloadCard.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                            }
                        }).start();
                    }
                }

                // Determine formatted values
                double speed = -1;
                String detailStr = "";
                String etaStr = "";
                String statusTitle = "";

                String modName = null;
                String modVersion = null;
                String modIconUrl = null;
                String contentType = null;

                if (va != null && va.length >= 9) {
                    try {
                        modName = (String) va[5];
                        modVersion = (String) va[6];
                        modIconUrl = (String) va[7];
                        contentType = (String) va[8];

                        double currentMB = ((Number) va[1]).doubleValue();
                        double totalMB = ((Number) va[2]).doubleValue();
                        speed = ((Number) va[3]).doubleValue();
                        double remainingSec = ((Number) va[4]).doubleValue();

                        if (totalMB > 0) {
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                        }
                        if (remainingSec >= 0) {
                            etaStr = formatRemainingTime(remainingSec);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mProgressIcon != null) {
                    if (modIconUrl != null && !modIconUrl.isEmpty()) {
                        String cacheTag = modName != null ? modName : modIconUrl.substring(modIconUrl.lastIndexOf('/') + 1);
                        ModIconCache.getInstance().getImage(bitmap -> {
                            post(() -> {
                                if (mProgressIcon != null && bitmap != null) {
                                    mProgressIcon.setImageBitmap(bitmap);
                                }
                            });
                        }, cacheTag, modIconUrl);
                    } else {
                        mProgressIcon.setImageResource(R.drawable.ic_download);
                    }
                }

                if (this.progressKey != null) {
                    if (resid == R.string.fabric_dl_progress) {
                        statusTitle = "Downloading Fabric" + (va != null && va.length > 0 && va[0] != null ? " " + va[0] : "");
                    } else if (resid == R.string.forge_dl_progress) {
                        String loaderName = "Forge";
                        String verSuffix = "";
                        if (va != null && va.length > 0 && va[0] != null) {
                            String verStr = String.valueOf(va[0]);
                            if (verStr.toLowerCase().contains("neoforge")) {
                                loaderName = ""; // The version string itself already contains NeoForge, e.g. "neoforge-20.4.80" or "NeoForge 20.4.80"
                            }
                            verSuffix = " " + verStr;
                        }
                        statusTitle = "Downloading " + loaderName + verSuffix;
                    } else if (resid == R.string.of_dl_progress) {
                        statusTitle = "Downloading OptiFine" + (va != null && va.length > 0 && va[0] != null ? " " + va[0] : "");
                    } else if (resid == R.string.neoforge_dl_searching) {
                        statusTitle = "Searching NeoForge...";
                    } else if (resid == R.string.forge_dl_searching) {
                        statusTitle = "Searching Forge...";
                    } else {
                        switch (this.progressKey) {
                            case DOWNLOAD_MINECRAFT:
                                statusTitle = "Downloading Minecraft";
                                break;
                            case UNPACK_RUNTIME:
                                statusTitle = "Unpacking Runtime";
                                break;
                            case INSTALL_MODPACK:
                                if (contentType != null) {
                                    String typeStr = contentType.substring(0, 1).toUpperCase() + contentType.substring(1);
                                    if ("resourcepack".equals(contentType)) typeStr = "Resource Pack";
                                    statusTitle = "Downloading " + typeStr + ": " + modName;
                                } else {
                                    statusTitle = "Installing Modpack";
                                }
                                break;
                            case EXTRACT_COMPONENTS:
                                statusTitle = "Extracting Components";
                                break;
                            case EXTRACT_SINGLE_FILES:
                                statusTitle = "Extracting Files";
                                break;
                            default:
                                if (resid != -1) {
                                    statusTitle = getContext().getString(resid);
                                } else if (va != null && va.length > 0 && va[0] instanceof String) {
                                    statusTitle = (String) va[0];
                                } else {
                                    statusTitle = "Downloading...";
                                }
                                break;
                        }
                    }
                }

                // If not using new format, perform fallback legacy parsing
                if (detailStr.isEmpty() && va != null) {
                    if (resid == R.string.newdl_downloading_game_files_size && va.length >= 3) {
                        try {
                            double currentMB = ((Number) va[0]).doubleValue();
                            double totalMB = ((Number) va[1]).doubleValue();
                            speed = ((Number) va[2]).doubleValue();
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                            if (speed > 0) {
                                double remainingMB = totalMB - currentMB;
                                double etaSeconds = remainingMB / speed;
                                etaStr = formatRemainingTime(etaSeconds);
                            } else {
                                etaStr = "";
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (resid == R.string.newdl_downloading_game_files && va.length >= 3) {
                        try {
                            long currentFiles = ((Number) va[0]).longValue();
                            long totalFiles = ((Number) va[1]).longValue();
                            speed = ((Number) va[2]).doubleValue();
                            detailStr = currentFiles + " / " + totalFiles + " files";
                            etaStr = "";
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (va.length >= 2 && va[0] instanceof Number && va[1] instanceof Number) {
                        try {
                            double currentMB = ((Number) va[0]).doubleValue();
                            double totalMB = ((Number) va[1]).doubleValue();
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (va.length >= 3 && va[1] instanceof Number && va[2] instanceof Number) {
                        try {
                            double currentMB = ((Number) va[1]).doubleValue();
                            double totalMB = ((Number) va[2]).doubleValue();
                            detailStr = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (va.length > 1 && va[1] != null) {
                            detailStr = String.valueOf(va[1]);
                        } else if (va.length > 0 && va[0] != null) {
                            detailStr = String.valueOf(va[0]);
                        }
                    }
                }

                if (mStatusText != null) {
                    mStatusText.setTextColor(0xFFFFFFFF); // Keep status white during progress
                    mStatusText.setText(statusTitle);
                }
                if (mDetailText != null) {
                    if (!detailStr.isEmpty()) {
                        mDetailText.setText(detailStr);
                        mDetailText.setVisibility(VISIBLE);
                    } else {
                        mDetailText.setVisibility(GONE);
                    }
                }
                if (mSpeedText != null) {
                    if (speed >= 0) {
                        mSpeedText.setText(String.format(java.util.Locale.US, "%.1f MB/s", speed));
                        mSpeedText.setVisibility(VISIBLE);
                    } else {
                        mSpeedText.setVisibility(GONE);
                    }
                }
                if (mEtaText != null) {
                    if (!etaStr.isEmpty()) {
                        mEtaText.setText(etaStr);
                        mEtaText.setVisibility(VISIBLE);
                    } else {
                        mEtaText.setVisibility(GONE);
                    }
                }
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
