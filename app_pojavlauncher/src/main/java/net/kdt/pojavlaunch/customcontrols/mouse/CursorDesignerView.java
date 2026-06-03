package net.kdt.pojavlaunch.customcontrols.mouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CursorDesignerView extends View {
    private Bitmap mDrawingBitmap;
    private Canvas mDrawingCanvas;
    private Paint mPaint;
    private Path mPath;
    public enum Tool { PENCIL, ERASER }
    private Tool mCurrentTool = Tool.PENCIL;
    private int mCanvasSize = 32; // Smaller canvas for pixel art

    public CursorDesignerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mDrawingBitmap = Bitmap.createBitmap(mCanvasSize, mCanvasSize, Bitmap.Config.ARGB_8888);
        mDrawingCanvas = new Canvas(mDrawingBitmap);
        mPaint = new Paint();
        mPaint.setAntiAlias(false); 
        mPaint.setDither(false);
        mPaint.setStyle(Paint.Style.FILL);
        mPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawGrid(canvas);
        
        float scale = (float) getWidth() / mCanvasSize;
        canvas.save();
        canvas.scale(scale, scale);
        canvas.drawBitmap(mDrawingBitmap, 0, 0, null);
        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#33FFFFFF"));
        float cellSize = (float) getWidth() / mCanvasSize;
        for (int i = 0; i <= mCanvasSize; i++) {
            canvas.drawLine(i * cellSize, 0, i * cellSize, getHeight(), gridPaint);
            canvas.drawLine(0, i * cellSize, getWidth(), i * cellSize, gridPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float scale = (float) getWidth() / mCanvasSize;
        int x = (int) (event.getX() / scale);
        int y = (int) (event.getY() / scale);

        if (x >= 0 && x < mCanvasSize && y >= 0 && y < mCanvasSize) {
            if (mCurrentTool == Tool.PENCIL) {
                mDrawingBitmap.setPixel(x, y, mCurrentColor);
            } else {
                mDrawingBitmap.setPixel(x, y, Color.TRANSPARENT);
            }
            invalidate();
        }

        return true;
    }

    public void setTool(Tool tool) {
        mCurrentTool = tool;
    }

    public void setColor(int color) {
        mCurrentColor = color;
        mPaint.setColor(mCurrentColor);
    }

    public Bitmap getCursorBitmap() {
        return mDrawingBitmap;
    }

    public void clear() {
        mDrawingBitmap.eraseColor(Color.TRANSPARENT);
        invalidate();
    }
}
