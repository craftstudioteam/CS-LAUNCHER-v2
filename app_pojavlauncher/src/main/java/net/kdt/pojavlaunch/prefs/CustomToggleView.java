package net.kdt.pojavlaunch.prefs;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

public class CustomToggleView extends View {

    private boolean mChecked = false;
    private float mAnimProgress = 0f;
    private ValueAnimator mAnimator;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTrackRect = new RectF();
    private OnCheckedChangeListener mListener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CustomToggleView view, boolean isChecked);
    }

    public CustomToggleView(Context context) {
        super(context);
        init();
    }

    public CustomToggleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomToggleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        mAnimProgress = mChecked ? 1f : 0f;
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (mChecked == checked) return;
        mChecked = checked;
        
        if (mAnimator != null) {
            mAnimator.cancel();
        }

        if (animate) {
            mAnimator = ValueAnimator.ofFloat(mAnimProgress, checked ? 1f : 0f);
            mAnimator.setDuration(220);
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.addUpdateListener(animation -> {
                mAnimProgress = (float) animation.getAnimatedValue();
                invalidate();
            });
            mAnimator.start();
        } else {
            mAnimProgress = checked ? 1f : 0f;
            invalidate();
        }

        if (mListener != null) {
            mListener.onCheckedChanged(this, mChecked);
        }
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    public void toggle() {
        setChecked(!mChecked, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float density = getResources().getDisplayMetrics().density;
        int defaultWidth = Math.round(52 * density);
        int defaultHeight = Math.round(28 * density);

        int width = (widthMode == MeasureSpec.EXACTLY) ? widthSize : defaultWidth;
        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : defaultHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        // Draw background track
        mTrackRect.set(0, 0, w, h);
        float radius = h / 2f;

        // Transition from dark grey (#222222) to neon green (#39FF14)
        int darkBg = 0xFF222222;
        int activeBg = 0xFF39FF14;
        int trackColor = blendColors(darkBg, activeBg, mAnimProgress);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(trackColor);
        canvas.drawRoundRect(mTrackRect, radius, radius, mPaint);

        // Draw thumb (white circle)
        float padding = 3f * getResources().getDisplayMetrics().density;
        float thumbRadius = radius - padding;
        
        // Thumb position interpolation
        float minX = radius;
        float maxX = w - radius;
        float thumbX = minX + (maxX - minX) * mAnimProgress;
        float thumbY = h / 2f;

        // Draw small shadow/glow if enabled
        if (mAnimProgress > 0.05f) {
            mPaint.setColor(blendColors(0x0039FF14, 0x4439FF14, mAnimProgress));
            canvas.drawCircle(thumbX, thumbY, thumbRadius + padding * 0.5f, mPaint);
        }

        mPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(thumbX, thumbY, thumbRadius, mPaint);
    }

    private int blendColors(int color1, int color2, float ratio) {
        int a = (int) (((color1 >> 24) & 0xff) * (1 - ratio) + ((color2 >> 24) & 0xff) * ratio);
        int r = (int) (((color1 >> 16) & 0xff) * (1 - ratio) + ((color2 >> 16) & 0xff) * ratio);
        int g = (int) (((color1 >> 8) & 0xff) * (1 - ratio) + ((color2 >> 8) & 0xff) * ratio);
        int b = (int) ((color1 & 0xff) * (1 - ratio) + (color2 & 0xff) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
