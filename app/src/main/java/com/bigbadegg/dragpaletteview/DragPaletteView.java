package com.bigbadegg.dragpaletteview;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class DragPaletteView extends ImageView {
    private Paint mPaint;
    private Paint mDebugPaint;
    private Path mPath;
    private float mLastX;
    private float mLastY;
    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;
    private Context mContext;
    private static final int MAX_CACHE_STEP = 200;

    private List<DrawingInfo> mDrawingList = new ArrayList<>(MAX_CACHE_STEP);
    private PorterDuffXfermode mClearMode, mDrawMode;
    private float mDrawSize;
    private float mEraserSize;

    private boolean mCanEraser;


    public enum Mode {
        DRAW,
        ERASER
    }

    private Mode mMode = Mode.DRAW;

    public DragPaletteView(Context context) {
        this(context, null);
    }

    public DragPaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setDrawingCacheEnabled(true);
        init();
        mScaleDetector = new ScaleGestureDetector(context, new SimpleScaleListenerImpl());
        mGestureDetector = new GestureDetector(context, new SimpleGestureListenerImpl());
    }

    public float getmScaleFactor() {
        return mScaleFactor;
    }

    private void init() {
        mClearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mDrawMode = new PorterDuffXfermode(PorterDuff.Mode.XOR);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFilterBitmap(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawSize = 20;
        mEraserSize = 20;
        mPaint.setStrokeWidth(mDrawSize);
        mPaint.setXfermode(mDrawMode);
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(150);
        mPaint.setMaskFilter(new BlurMaskFilter(mDrawSize / 16, BlurMaskFilter.Blur.NORMAL));


        mDebugPaint = new Paint();
        mDebugPaint.setStyle(Paint.Style.STROKE);
    }

    private void initBuffer() {
        mBufferBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
        mRect.left = (int) mPosX;
        mRect.right = (int) (mPosX + bmpWidth);
        mRect.top = (int) mPosY;
        mRect.bottom = (int) (mPosY + bmpHeight);
    }

    public abstract class DrawingInfo {
        Paint paint;
        Mode mode;

        abstract void draw(Canvas canvas);
    }

    public class PathDrawingInfo extends DrawingInfo {

        Path path;

        public Path getPath() {
            return path;
        }

        @Override
        void draw(Canvas canvas) {
            if (mode == Mode.DRAW) {

//                Paint.Style style = paint.getStyle();
//                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(path, paint);
//                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(path, paint);
//                paint.setStyle(style);

            } else {
                canvas.drawPath(path, paint);
            }
        }
    }

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            if (mMode == Mode.DRAW) {
                mPaint.setXfermode(mDrawMode);
                mPaint.setStrokeWidth(mDrawSize);
                mPaint.setMaskFilter(new BlurMaskFilter(mDrawSize / 16, BlurMaskFilter.Blur.NORMAL));
            } else {
                mPaint.setXfermode(mClearMode);
                mPaint.setStrokeWidth(mEraserSize);
                mPaint.setMaskFilter(null);
            }
        }
    }

    public void setEraserSize(float size) {
        mEraserSize = size / (mScaleFactor);
        mPaint.setStrokeWidth(mEraserSize);
    }

    public void setPenRawSize(float size) {
        mDrawSize = size / (mScaleFactor);
        mPaint.setStrokeWidth(mDrawSize);
        mPaint.setMaskFilter(new BlurMaskFilter(mDrawSize / 16, BlurMaskFilter.Blur.NORMAL));
    }

    public float getmDrawSize() {
        return mDrawSize;
    }

    public float getmEraserSize() {
        return mEraserSize;
    }

    public void setPenColor(int color) {
        mPaint.setColor(color);
    }

    public void setPenAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    private void reDraw() {
        if (mDrawingList != null && mBufferBitmap != null) {
            mBufferBitmap.eraseColor(Color.TRANSPARENT);
            for (int i = 0; i < 3; i++) {
                for (DrawingInfo drawingInfo : mDrawingList) {
                    drawingInfo.draw(mBufferCanvas);
                }
            }
            invalidate();
        }
    }

    public boolean canUndo() {
        return mDrawingList != null && mDrawingList.size() > 0;
    }


    public void undo() {
        int size = mDrawingList == null ? 0 : mDrawingList.size();
        if (size > 0) {
            DrawingInfo info = mDrawingList.remove(size - 1);
            if (size == 1) {
                mCanEraser = false;
            }
            reDraw();
        }
    }

    private void saveDrawingPath() {
        if (mDrawingList == null) {
            mDrawingList = new ArrayList<>(MAX_CACHE_STEP);
        } else if (mDrawingList.size() == MAX_CACHE_STEP) {
            mDrawingList.remove(0);
        }
        Path cachePath = new Path(mPath);
        Paint cachePaint = new Paint(mPaint);
        PathDrawingInfo info = new PathDrawingInfo();
        info.path = cachePath;
        info.paint = cachePaint;
        info.mode = mMode;
        mDrawingList.add(info);
        mCanEraser = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bmp == null) {
            return;
        }

        if (!hasGetViewSize) {
            initViewSize();
        }

        canvas.save();
        checkBounds();
        //以图片的中心为基点进行缩放
        Matrix matrix = new Matrix();
        canvas.scale(mScaleFactor, mScaleFactor, mPosX + bmpWidth / 2, mPosY + bmpHeight / 2);
        canvas.drawBitmap(bmp, mPosX, mPosY, null);
        matrix.reset();
        canvas.restore();

        if (mBufferBitmap == null) {
            return;
        }

        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor, mPosX + bmpWidth / 2, mPosY + bmpHeight / 2);
        canvas.drawBitmap(mBufferBitmap, mPosX, mPosY, null);
        mRect.left = (int) (mPosX + bmpWidth / 2 - bmpWidth * mScaleFactor / 2);
        mRect.right = (int) (mPosX + bmpWidth / 2 + bmpWidth * mScaleFactor / 2);
        mRect.top = (int) (mPosY + bmpHeight / 2 - bmpHeight * mScaleFactor / 2);
        mRect.bottom = (int) (mPosY + bmpHeight / 2 + bmpHeight * mScaleFactor / 2);
        matrix.reset();
        canvas.restore();

//        if (isApkInDebug()) {
//            mDebugPaint.setColor(Color.GREEN);
//            mDebugPaint.setStrokeWidth(10);
//            canvas.drawRect(edgeOfPath.left, edgeOfPath.top, edgeOfPath.right, edgeOfPath.bottom, mDebugPaint);
//        }
    }

    boolean hasTwoPoint = false;//曾经有过两指的情况
    private Rect mRect = new Rect();
    private PathMeasure pathMeasure = new PathMeasure();

    private boolean containPath = false;//bitmap是否包含path
    private boolean initContainPath = true;//每条path只判断一次

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getPointerCount() == 2) {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
            hasTwoPoint = true;
            return true;
        }
        final int action = event.getAction() & MotionEvent.ACTION_MASK;

        float ox = ((bmpWidth * mScaleFactor - bmpWidth) / 2) / mScaleFactor - mPosX / mScaleFactor;
        float oy = ((bmpHeight * mScaleFactor - bmpHeight) / 2) / mScaleFactor - mPosY / mScaleFactor;

        float x = (event.getX() / mScaleFactor) + ox;
        float y = (event.getY() / mScaleFactor) + oy;

        boolean contains = mRect.contains((int) (x + mPosX), (int) (y + mPosY));

        if (initContainPath && contains) {
            containPath = true;
            initContainPath = false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                if (mPath == null) {
                    mPath = new Path();
                }

                mPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                //这里终点设为两点的中心点的目的在于使绘制的曲线更平滑，如果终点直接设置为x,y，效果和lineto是一样的,实际是折线效果
                mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);

                if (mBufferBitmap == null) {
                    initBuffer();
                }
                // 防止 页面clear 橡皮擦拖不得，且无法更新预览
//                if (mMode == Mode.ERASER && !mCanEraser) {
//                    break;
//                }
                mBufferCanvas.save();
                mBufferCanvas.drawPath(mPath, mPaint);
                mBufferCanvas.restore();
                invalidate();
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                if (mMode == Mode.DRAW || mCanEraser) {
                    pathMeasure.setPath(mPath, false);
                    if (containPath && pathMeasure.getLength() > 5) {//去掉点
                        saveDrawingPath();
                    }
                }

                for (int i = 0; i < 3; i++) {
                    if (mBufferCanvas != null) {
                        if (mMode == Mode.DRAW) {
//                            Paint.Style style = mPaint.getStyle();
//                            mPaint.setStyle(Paint.Style.FILL);
//                            mPath.close();
                            mBufferCanvas.drawPath(mPath, mPaint);
//                            mPaint.setStyle(Paint.Style.STROKE);
//                            mPath.close();
                            mBufferCanvas.drawPath(mPath, mPaint);
//                            mPaint.setStyle(style);
                        } else {
                            mBufferCanvas.drawPath(mPath, mPaint);
                        }
                    }
                }

                if (hasTwoPoint && pathMeasure.getLength() > 5) {
                    undo();
                    hasTwoPoint = !hasTwoPoint;
                }
                invalidate();
                mPath.reset();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;

        }
        return true;
    }

    //监听图片缩放
    private ScaleGestureDetector mScaleDetector;
    //监听图片移动
    private GestureDetector mGestureDetector;

    //当前的缩放比例
    private float mScaleFactor = 1.0f;
    //图片资源
    private Bitmap bmp;
    //图片的宽高
    private int bmpWidth = 1, bmpHeight = 1;
    //绘制图片的起始位置
    private float mPosX = 0, mPosY = 0;

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        bmp = bm;
        bmpWidth = bm.getWidth();
        bmpHeight = bm.getHeight();

        initViewSize();

        invalidate();
    }

    /**
     * 不能超出边界.
     * 原则是：图片较小时任意一条边都不能出了边界，图片较大任意一条边都不能进入边界。宽度和高度分别独立计算。
     */
    private void checkBounds() {
        if (mScaleFactor > widthScale) {
            //宽度方向已经填满
            mPosX = Math.min(mPosX, (mScaleFactor - 1) * (bmpWidth / 2));
            mPosX = Math.max(mPosX, viewWidth - bmpWidth - (mScaleFactor - 1) * (bmpWidth / 2));
        } else {
            mPosX = Math.max(mPosX, (mScaleFactor - 1) * (bmpWidth / 2));
            mPosX = Math.min(mPosX, viewWidth - bmpWidth - (mScaleFactor - 1) * (bmpWidth / 2));
        }

        if (mScaleFactor > heightScale) {
            //高度方向已经填满
            mPosY = Math.min(mPosY, (mScaleFactor - 1) * (bmpHeight / 2));
            mPosY = Math.max(mPosY, viewHeight - bmpHeight - (mScaleFactor - 1) * (bmpHeight / 2));
        } else {
            mPosY = Math.max(mPosY, (mScaleFactor - 1) * (bmpHeight / 2));
            mPosY = Math.min(mPosY, viewHeight - bmpHeight - (mScaleFactor - 1) * (bmpHeight / 2));
        }
    }

    private int viewWidth, viewHeight;
    //组件尺寸只需要获取一次
    private boolean hasGetViewSize;

    private void initViewSize() {
        viewWidth = getWidth();
        viewHeight = getHeight();

        if (viewWidth > 0 && viewHeight > 0) {
            hasGetViewSize = true;

            widthScale = 1.0f * viewWidth / bmpWidth;
            heightScale = 1.0f * viewHeight / bmpHeight;
            //初始缩放比例（使组件刚好铺满）
            mScaleFactor = Math.min(widthScale, heightScale);

            //初始时图片居中绘制
            mPosX = viewWidth / 2 - bmpWidth / 2;
            mPosY = viewHeight / 2 - bmpHeight / 2;
        }
    }

    /**
     * 宽度和高度放大多少倍时，刚好填满此方向的屏幕
     */
    private float widthScale, heightScale;

    //缩放
    private class SimpleScaleListenerImpl extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            //缩放倍数范围：0.3～3
            mScaleFactor = Math.max(1f, Math.min(mScaleFactor, 3.0f));
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);

        }
    }

    //移动
    private class SimpleGestureListenerImpl extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mPosX -= distanceX;
            mPosY -= distanceY;


            invalidate();
            return true;
        }

    }

    float[] points = new float[2];
    float[] tans = new float[2];
    float currentDistance = 0;// 0 ~ 1;

    private void getPathPoint(Path path) {
        PathMeasure pathMeasure = new PathMeasure(path, false);
        pathMeasure.getPosTan(pathMeasure.getLength() * currentDistance, points, tans);

    }

    /**
     * 判断当前应用是否是debug状态
     */
    public boolean isApkInDebug() {
        try {
            ApplicationInfo info = mContext.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }
}
