package com.circleimagecrop.cropoverlay;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.circleimagecrop.R;
import com.circleimagecrop.cropoverlay.edge.Edge;
import com.circleimagecrop.cropoverlay.utils.PaintUtil;
import com.circleimagecrop.photoview.PhotoViewAttacher;


public class CropOverlayView extends View implements PhotoViewAttacher.IGetImageBounds {

    private static final float CORNER_RADIUS = 360;
    //Defaults
    private boolean DEFAULT_GUIDELINES = true;
    private int DEFAULT_MARGINTOP = 100;
    private int DEFAULT_MARGINSIDE = 50;
    private int DEFAULT_MIN_WIDTH = 200;
    private int DEFAULT_MAX_WIDTH = 300;
    // we are croping square image so width and height will always be equal
    private int DEFAULT_CROPWIDTH = 600;
    // The Paint used to darken the surrounding areas outside the crop area.
    private Paint mBackgroundPaint;

    // The Paint used to draw the white rectangle around the crop area.
    private Paint mBorderPaint;

    // The Paint used to draw the guidelines within the crop area.
    private Paint mGuidelinePaint;

    // The bounding box around the Bitmap that we are cropping.
    private Rect mBitmapRect;

    private int cropHeight = DEFAULT_CROPWIDTH;
    private int cropWidth = DEFAULT_CROPWIDTH;


    private boolean mGuidelines;
    private int mMarginTop;
    private int mMarginSide;
    private int mMinWidth;
    private int mMaxWidth;
    private Context mContext;

    public CropOverlayView(Context context) {
        super(context);
        init(context);
        this.mContext = context;
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropOverlayView, 0, 0);
        try {
            mGuidelines = ta.getBoolean(R.styleable.CropOverlayView_guideLines, DEFAULT_GUIDELINES);
            mMarginTop = ta.getDimensionPixelSize(R.styleable.CropOverlayView_marginTop, DEFAULT_MARGINTOP);
            mMarginSide = ta.getDimensionPixelSize(R.styleable.CropOverlayView_marginSide, DEFAULT_MARGINSIDE);
            mMinWidth = ta.getDimensionPixelSize(R.styleable.CropOverlayView_minWidth, DEFAULT_MIN_WIDTH);
            mMaxWidth = ta.getDimensionPixelSize(R.styleable.CropOverlayView_maxWidth, DEFAULT_MAX_WIDTH);
        } finally {
            ta.recycle();
        }

        init(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {
            //BUG FIX : Turn of hardware acceleration. Clip path doesn't work with hardware acceleration
            //BUG FIX : Will have to do it here @ View level. Activity level not working on HTC ONE X
            //http://stackoverflow.com/questions/8895677/work-around-canvas-clippath-that-is-not-supported-in-android-any-more/8895894#8895894
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            final float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CORNER_RADIUS, mContext.getResources().getDisplayMetrics());

            RectF rectF = new RectF(Edge.LEFT.getCoordinate(), Edge.TOP.getCoordinate(), Edge.RIGHT.getCoordinate(), Edge.BOTTOM.getCoordinate());
            Path clipPath = new Path();
            clipPath.addRoundRect(rectF, radius, radius, Path.Direction.CW);
            canvas.clipPath(clipPath, Region.Op.DIFFERENCE);
            canvas.drawARGB(204, 41, 48, 63);
            canvas.restore();
            canvas.drawRoundRect(rectF, radius, radius, mBorderPaint);

            //GT :  Drop shadow not working right now. Commenting the code now
//        //Draw shadow
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setShadowLayer(12, 0, 0, Color.YELLOW);
//        paint.setAlpha(0);
//        drawRuleOfThirdsGuidelines(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Rect getImageBounds() {
        return new Rect((int) Edge.LEFT.getCoordinate(), (int) Edge.TOP.getCoordinate(), (int) Edge.RIGHT.getCoordinate(), (int) Edge.BOTTOM.getCoordinate());
    }


    // Private Methods /////////////////////////////////////////////////////////
    private void init(Context context) {
        int w = context.getResources().getDisplayMetrics().widthPixels;
        cropWidth = w - 2 * mMarginSide;
        cropHeight = cropWidth;
        int edgeT = mMarginTop;
        int edgeB = mMarginTop + cropHeight;
        int edgeL = mMarginSide;
        int edgeR = mMarginSide + cropWidth;
        mBackgroundPaint = PaintUtil.newBackgroundPaint(context);
        mBorderPaint = PaintUtil.newBorderPaint(context);
        mGuidelinePaint = PaintUtil.newGuidelinePaint();
        Edge.TOP.setCoordinate(edgeT);
        Edge.BOTTOM.setCoordinate(edgeB);
        Edge.LEFT.setCoordinate(edgeL);
        Edge.RIGHT.setCoordinate(edgeR);
        new Rect(edgeL, edgeT, edgeR, edgeB);
        mBitmapRect = new Rect(0, 0, w, w);
    }

}
