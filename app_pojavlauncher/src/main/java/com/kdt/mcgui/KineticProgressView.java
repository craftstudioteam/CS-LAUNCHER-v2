package com.kdt.mcgui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A kinetic circular progress indicator with animated glow and particle effects.
 * Renders a dynamic data-stream gauge that fills smoothly as progress increases.
 */
public class KineticProgressView extends View {

    private static final int NEON_GREEN = 0xFF00FF41;
    private static final int NEON_GREEN_DIM = 0x6600FF41;
    private static final int DARK_BG = 0xFF1A1A1A;

    private float mProgress = 0f; // 0..1
    private float mAnimProgress = 0f;

    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mArcPath = new Path();
    private final RectF mArcBounds = new RectF();

    private float mAngleOffset = -90f;
    private ValueAnimator mAnimator;
    private long mCompleteTime = -1;
    private boolean mHideText = false;

    public void setHideText(boolean hide) {
        mHideText = hide;
        invalidate();
    }

    // Particle positions (simulated)
    private float mParticlePhase = 0f;

    public KineticProgressView(@NonNull Context context) {
        super(context);
        init();
    }

    public KineticProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KineticProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(6f);
        mTrackPaint.setColor(0xFF2A2A2A);
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(6f);
        mArcPaint.setColor(NEON_GREEN);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setStrokeJoin(Paint.Join.ROUND);

        mGlowPaint.setStyle(Paint.Style.STROKE);
        mGlowPaint.setStrokeWidth(14f);
        mGlowPaint.setColor(NEON_GREEN_DIM);
        mGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        mGlowPaint.setMaskFilter(null); // Disabled for compatibility

        mParticlePaint.setStyle(Paint.Style.FILL);
        mParticlePaint.setColor(NEON_GREEN);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setAntiAlias(true);
    }

    /**
     * Set the progress value (0..100). Triggers smooth animation.
     */
    public void setProgress(int percent) {
        float target = Math.max(0, Math.min(100, percent)) / 100f;
        if (mAnimator != null) mAnimator.cancel();

        mAnimator = ValueAnimator.ofFloat(mAnimProgress, target);
        mAnimator.setDuration(200);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(anim -> {
            mAnimProgress = (float) anim.getAnimatedValue();
            mParticlePhase += 0.08f;
            invalidate();
        });
        mAnimator.start();
        mProgress = target;
    }

    public int getProgress() {
        return Math.round(mAnimProgress * 100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 20f;
        float sweepAngle = mAnimProgress * 360f;

        // Draw track circle
        mArcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(mArcBounds, 0, 360, false, mTrackPaint);

        int percent = Math.round(mAnimProgress * 100);
        if (percent == 100) {
            if (mCompleteTime == -1) {
                mCompleteTime = android.os.SystemClock.elapsedRealtime();
            }
            float elapsed = (android.os.SystemClock.elapsedRealtime() - mCompleteTime) / 1000f;

            // Animate glow width dynamically using elapsed time
            float glowWidth = 14f + 6f * (0.5f + 0.5f * (float) Math.sin(elapsed * 6f));
            mGlowPaint.setStrokeWidth(glowWidth);

            // Draw full circular glow and progress circle
            canvas.drawArc(mArcBounds, 0, 360, false, mGlowPaint);
            canvas.drawArc(mArcBounds, 0, 360, false, mArcPaint);

            // Checkmark animation progress
            float t = Math.min(1.0f, elapsed / 0.4f); // 400ms duration
            float x1 = cx - radius * 0.3f;
            float y1 = cy - radius * 0.05f;
            float x2 = cx - radius * 0.05f;
            float y2 = cy + radius * 0.2f;
            float x3 = cx + radius * 0.35f;
            float y3 = cy - radius * 0.25f;

            mArcPath.reset();
            if (t < 0.4f) {
                float t1 = t / 0.4f;
                float currentX = x1 + (x2 - x1) * t1;
                float currentY = y1 + (y2 - y1) * t1;
                mArcPath.moveTo(x1, y1);
                mArcPath.lineTo(currentX, currentY);
            } else {
                float t2 = (t - 0.4f) / 0.6f;
                float currentX = x2 + (x3 - x2) * t2;
                float currentY = y2 + (y3 - y2) * t2;
                mArcPath.moveTo(x1, y1);
                mArcPath.lineTo(x2, y2);
                mArcPath.lineTo(currentX, currentY);
            }

            float prevStrokeWidth = mArcPaint.getStrokeWidth();
            mArcPaint.setStrokeWidth(8f);
            canvas.drawPath(mArcPath, mArcPaint);
            mArcPaint.setStrokeWidth(prevStrokeWidth);

            postInvalidateOnAnimation();
        } else {
            mCompleteTime = -1;
            // Restore normal glow width
            mGlowPaint.setStrokeWidth(14f);

            // Draw glow layer (thicker, semi-transparent)
            if (mAnimProgress > 0) {
                canvas.drawArc(mArcBounds, mAngleOffset, sweepAngle, false, mGlowPaint);
            }

            // Draw progress arc
            if (mAnimProgress > 0) {
                canvas.drawArc(mArcBounds, mAngleOffset, sweepAngle, false, mArcPaint);
            }

            // Draw particles along the leading edge of the arc
            if (mAnimProgress > 0 && mAnimProgress < 1f) {
                float leadAngle = mAngleOffset + sweepAngle;
                float radians = (float) Math.toRadians(leadAngle);
                float px = cx + radius * (float) Math.cos(radians);
                float py = cy + radius * (float) Math.sin(radians);

                // Pulsing particle at the tip
                float pulseSize = 4f + 3f * (float) Math.sin(mParticlePhase);
                mParticlePaint.setAlpha(200);
                canvas.drawCircle(px, py, pulseSize, mParticlePaint);

                // Secondary trailing particles
                for (int i = 1; i <= 3; i++) {
                    float spreadAngle = sweepAngle - i * 6f;
                    if (spreadAngle < 0) break;
                    float r = (float) Math.toRadians(mAngleOffset + spreadAngle);
                    float sx = cx + radius * (float) Math.cos(r);
                    float sy = cy + radius * (float) Math.sin(r);
                    mParticlePaint.setAlpha(100 - i * 25);
                    canvas.drawCircle(sx, sy, 2.5f - i * 0.5f, mParticlePaint);
                }
            }

            // Draw percentage text
            if (!mHideText) {
                float textSize = radius * 0.6f;
                mTextPaint.setTextSize(textSize);
                canvas.drawText(percent + "%", cx, cy + textSize * 0.35f, mTextPaint);
            }
        }
    }
}
