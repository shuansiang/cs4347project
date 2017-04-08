package com.tetrastudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

// Adapted from http://stackoverflow.com/questions/36467849/custom-shape-button
public class RadialView extends View {

    //the number of slice
    private int mSlices = 7;

    //the angle of each slice
    private float mDegreeStep = 360.0f / mSlices;

    private int quarterDegreeMinus = -90;

    private float mOuterRadius;
    private float mInnerRadius;

    //using radius square to prevent square root calculation
    private float mOuterRadiusSquare;
    private float mInnerRadiusSquare;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mSliceOval = new RectF();

    private static final double QUARTER_CIRCLE = Math.PI / 2;

    private float mInnerRadiusRatio = 0.0F;

    private int[] colors = new int[]{Color.GREEN, Color.GRAY, Color.BLUE, Color.CYAN, Color.DKGRAY, Color.RED};

    private int mCenterX;
    private int mCenterY;

    private OnSliceClickListener mOnSliceClickListener;

    private int mCurrentSliceIndex = -1;

    public interface OnSliceClickListener {
        void onSlickClickDown(int slicePosition);

        void onSlickClickMove(int slicePosition);

        void onSlickClickUp(int slicePosition);
    }

    public RadialView(Context context) {
        this(context, null);
    }

    public RadialView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadialView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPaint.setStrokeWidth(10);
    }

    public void setOnSliceClickListener(OnSliceClickListener onSliceClickListener) {
        mOnSliceClickListener = onSliceClickListener;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCenterX = w / 2;
        mCenterY = h / 2;

        mOuterRadius = mCenterX > mCenterY ? mCenterY : mCenterX;
        mInnerRadius = mOuterRadius * mInnerRadiusRatio;

        mOuterRadiusSquare = mOuterRadius * mOuterRadius;
        mInnerRadiusSquare = mInnerRadius * mInnerRadius;

        mSliceOval.left = mCenterX - mOuterRadius;
        mSliceOval.right = mCenterX + mOuterRadius;
        mSliceOval.top = mCenterY - mOuterRadius;
        mSliceOval.bottom = mCenterY + mOuterRadius;
    }

    private int getSliceIndex(float x, float y) {
        int dx = (int) x - mCenterX;
        int dy = (int) y - mCenterY;
        int distanceSquare = dx * dx + dy * dy;

        //if the distance between touchpoint and centerpoint is smaller than outerRadius and longer than innerRadius, then we're in the clickable area
        if (distanceSquare > mInnerRadiusSquare && distanceSquare < mOuterRadiusSquare) {

            //get the angle to detect which slice is currently being click
            double angle = Math.atan2(dy, dx);

            if (angle >= -QUARTER_CIRCLE && angle < 0) {
                angle += QUARTER_CIRCLE;
            } else if (angle >= -Math.PI && angle < -QUARTER_CIRCLE) {
                angle += Math.PI + Math.PI + QUARTER_CIRCLE;
            } else if (angle >= 0 && angle < Math.PI) {
                angle += QUARTER_CIRCLE;
            }

            double rawSliceIndex = angle / (Math.PI * 2) * mSlices;
            return (int) rawSliceIndex;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currX = event.getX();
        float currY = event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_MOVE
                || event.getActionMasked() == MotionEvent.ACTION_UP) {
            int sliceIndex = getSliceIndex(currX, currY);
            mCurrentSliceIndex = sliceIndex;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mOnSliceClickListener.onSlickClickDown((int) sliceIndex);
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                mOnSliceClickListener.onSlickClickMove((int) sliceIndex);
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                mOnSliceClickListener.onSlickClickUp((int) sliceIndex);
            }
        }


        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int startAngle = quarterDegreeMinus;

        //draw slice
        for (int i = 0; i < mSlices; i++) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(colors[i % colors.length]);
            canvas.drawArc(mSliceOval, startAngle, mDegreeStep, true, mPaint);
            startAngle += mDegreeStep;
        }

    }
}