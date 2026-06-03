package net.kdt.pojavlaunch.customcontrols.mouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Stack;

public class CursorDesignerView extends View {
    public enum Tool { PENCIL, ERASER, FILL }
    
    private Bitmap mDrawingBitmap;
    private Canvas mDrawingCanvas;
    private final Paint mPaint = new Paint();
    
    private Tool mCurrentTool = Tool.PENCIL;
    private int mCurrentColor = Color.WHITE;
    private final int mCanvasSize = 32;

    // View Transformation
    private final Matrix mMatrix = new Matrix();
    private final Matrix mInverseMatrix = new Matrix();
    private final float[] mTouchPoint = new float[2];
    
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    
    // Undo/Redo System
    private final Stack<Bitmap> mUndoStack = new Stack<>();
    private final Stack<Bitmap> mRedoStack = new Stack<>();
    private static final int MAX_STACK_SIZE = 20;

    public interface OnCanvasChangedListener {
        void onCanvasChanged(Bitmap bitmap);
    }
    private OnCanvasChangedListener mListener;

    public CursorDesignerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mDrawingBitmap = Bitmap.createBitmap(mCanvasSize, mCanvasSize, Bitmap.Config.ARGB_8888);
        mDrawingCanvas = new Canvas(mDrawingBitmap);
        
        mPaint.setAntiAlias(false);
        mPaint.setDither(false);
        mPaint.setStyle(Paint.Style.FILL);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        
        // Initial center and scale
        post(() -> {
            float scale = Math.min(getWidth(), getHeight()) / (float) mCanvasSize * 0.8f;
            mMatrix.setScale(scale, scale);
            mMatrix.postTranslate((getWidth() - mCanvasSize * scale) / 2f, (getHeight() - mCanvasSize * scale) / 2f);
            invalidate();
        });
        
        saveState();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#1A1A1A")); // Editor background
        
        canvas.save();
        canvas.concat(mMatrix);
        
        // Draw Grid
        drawGrid(canvas);
        
        // Draw Bitmap
        canvas.drawBitmap(mDrawingBitmap, 0, 0, null);
        
        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        Paint gridPaint = new Paint();
        gridPaint.setStrokeWidth(0.05f); // Very thin grid
        gridPaint.setColor(Color.parseColor("#40FFFFFF"));
        
        for (int i = 0; i <= mCanvasSize; i++) {
            canvas.drawLine(i, 0, i, mCanvasSize, gridPaint);
            canvas.drawLine(0, i, mCanvasSize, i, gridPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = mScaleDetector.onTouchEvent(event);
        handled = mGestureDetector.onTouchEvent(event) || handled;

        if (event.getPointerCount() == 1) {
            // Drawing logic
            mMatrix.invert(mInverseMatrix);
            mTouchPoint[0] = event.getX();
            mTouchPoint[1] = event.getY();
            mInverseMatrix.mapPoints(mTouchPoint);

            int x = (int) mTouchPoint[0];
            int y = (int) mTouchPoint[1];

            if (x >= 0 && x < mCanvasSize && y >= 0 && y < mCanvasSize) {
                getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    saveState();
                }
                
                if (mCurrentTool == Tool.FILL) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        floodFill(x, y, mCurrentColor);
                    }
                } else {
                    int color = (mCurrentTool == Tool.PENCIL) ? mCurrentColor : Color.TRANSPARENT;
                    mDrawingBitmap.setPixel(x, y, color);
                }
                invalidate();
                if (mListener != null) mListener.onCanvasChanged(mDrawingBitmap);
                return true;
            }
        }
        
        return handled || super.onTouchEvent(event);
    }

    public void setOnCanvasChangedListener(OnCanvasChangedListener listener) {
        mListener = listener;
    }

    private void saveState() {
        if (mUndoStack.size() >= MAX_STACK_SIZE) mUndoStack.remove(0);
        mUndoStack.push(mDrawingBitmap.copy(mDrawingBitmap.getConfig(), true));
        mRedoStack.clear();
    }

    public void undo() {
        if (mUndoStack.size() > 1) {
            mRedoStack.push(mUndoStack.pop());
            mDrawingBitmap = mUndoStack.peek().copy(mDrawingBitmap.getConfig(), true);
            mDrawingCanvas = new Canvas(mDrawingBitmap);
            invalidate();
            if (mListener != null) mListener.onCanvasChanged(mDrawingBitmap);
        }
    }

    public void redo() {
        if (!mRedoStack.isEmpty()) {
            mUndoStack.push(mRedoStack.pop());
            mDrawingBitmap = mUndoStack.peek().copy(mDrawingBitmap.getConfig(), true);
            mDrawingCanvas = new Canvas(mDrawingBitmap);
            invalidate();
            if (mListener != null) mListener.onCanvasChanged(mDrawingBitmap);
        }
    }

    public void setTool(Tool tool) { mCurrentTool = tool; }
    public void setColor(int color) { mCurrentColor = color; }
    public Bitmap getCursorBitmap() { return mDrawingBitmap; }
    
    public void clear() {
        saveState();
        mDrawingBitmap.eraseColor(Color.TRANSPARENT);
        invalidate();
        if (mListener != null) mListener.onCanvasChanged(mDrawingBitmap);
    }

    private void floodFill(int x, int y, int targetColor) {
        int oldColor = mDrawingBitmap.getPixel(x, y);
        if (oldColor == targetColor) return;
        
        Stack<android.graphics.Point> pixels = new Stack<>();
        pixels.push(new android.graphics.Point(x, y));
        
        while (!pixels.isEmpty()) {
            android.graphics.Point p = pixels.pop();
            if (p.x < 0 || p.x >= mCanvasSize || p.y < 0 || p.y >= mCanvasSize) continue;
            
            if (mDrawingBitmap.getPixel(p.x, p.y) == oldColor) {
                mDrawingBitmap.setPixel(p.x, p.y, targetColor);
                pixels.push(new android.graphics.Point(p.x + 1, p.y));
                pixels.push(new android.graphics.Point(p.x - 1, p.y));
                pixels.push(new android.graphics.Point(p.x, p.y + 1));
                pixels.push(new android.graphics.Point(p.x, p.y - 1));
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e2.getPointerCount() > 1) {
                mMatrix.postTranslate(-distanceX, -distanceY);
                invalidate();
                return true;
            }
            return false;
        }
    }
}
