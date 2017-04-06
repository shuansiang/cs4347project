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

/**
 * Created by Eyx on 6/4/2017.
 */

public class RadialView extends View {

    //the number of slice
    private int mSlices = 7;

    //the angle of each slice
    private float degreeStep = 360.0f / mSlices;

    private int quarterDegreeMinus = -90;

    private float mOuterRadius;
    private float mInnerRadius;

    //using radius square to prevent square root calculation
    private float outerRadiusSquare;
    private float innerRadiusSquare;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mSliceOval = new RectF();

    private static final double quarterCircle = Math.PI / 2;

    private float innerRadiusRatio = 0.0F;

    //color for your slice
    private int[] colors = new int[]{Color.GREEN, Color.GRAY, Color.BLUE, Color.CYAN, Color.DKGRAY, Color.RED};

    private int mCenterX;
    private int mCenterY;

    private OnSliceClickListener mOnSliceClickListener;
    private int mTouchSlop;

    private boolean mPressed;
    private float mLatestDownX;
    private float mLatestDownY;

    private int mCurrentSliceIndex = -1;

    public interface OnSliceClickListener {
        void onSlickClickDown(int slicePosition);

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

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();

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
        mInnerRadius = mOuterRadius * innerRadiusRatio;

        outerRadiusSquare = mOuterRadius * mOuterRadius;
        innerRadiusSquare = mInnerRadius * mInnerRadius;

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
        if (distanceSquare > innerRadiusSquare && distanceSquare < outerRadiusSquare) {

            //get the angle to detect which slice is currently being click
            double angle = Math.atan2(dy, dx);

            if (angle >= -quarterCircle && angle < 0) {
                angle += quarterCircle;
            } else if (angle >= -Math.PI && angle < -quarterCircle) {
                angle += Math.PI + Math.PI + quarterCircle;
            } else if (angle >= 0 && angle < Math.PI) {
                angle += quarterCircle;
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
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                mOnSliceClickListener.onSlickClickUp((int) sliceIndex);
            }
        }

//        switch (event.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN:
//                mLatestDownX = currX;
//                mLatestDownY = currY;
//
//
//                mPressed = true;
//                int dx = (int) currX - mCenterX;
//                int dy = (int) currY - mCenterY;
//                int distanceSquare = dx * dx + dy * dy;
//
//                //if the distance between touchpoint and centerpoint is smaller than outerRadius and longer than innerRadius, then we're in the clickable area
//                if (distanceSquare > innerRadiusSquare && distanceSquare < outerRadiusSquare) {
//
//                    //get the angle to detect which slice is currently being click
//                    double angle = Math.atan2(dy, dx);
//
//                    if (angle >= -quarterCircle && angle < 0) {
//                        angle += quarterCircle;
//                    } else if (angle >= -Math.PI && angle < -quarterCircle) {
//                        angle += Math.PI + Math.PI + quarterCircle;
//                    } else if (angle >= 0 && angle < Math.PI) {
//                        angle += quarterCircle;
//                    }
//
//                    double rawSliceIndex = angle / (Math.PI * 2) * mSlices;
//
//                    if (mOnSliceClickListener != null) {
//                        mOnSliceClickListener.onSlickClickDown((int) rawSliceIndex);
//                    }
//
//                }
//                break;
//            case MotionEvent.ACTION_MOVE:
//                if (Math.abs(currX - mLatestDownX) > mTouchSlop || Math.abs(currY - mLatestDownY) > mTouchSlop)
//                    mPressed = false;
//                break;
//            case MotionEvent.ACTION_UP:
//                if (mPressed) {
//                    mOnSliceClickListener.onSlickClickUp((int) rawSliceIndex);
//                    mPressed = false;
//                }
//                break;
//        }

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int startAngle = quarterDegreeMinus;

        //draw slice
        for (int i = 0; i < mSlices; i++) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(colors[i % colors.length]);
            canvas.drawArc(mSliceOval, startAngle, degreeStep, true, mPaint);

//            mPaint.setStyle(Paint.Style.STROKE);
//            mPaint.setColor(Color.WHITE);
//            canvas.drawArc(mSliceOval, startAngle, degreeStep, true, mPaint);

            startAngle += degreeStep;
        }

//        //draw center circle
//        mPaint.setStyle(Paint.Style.FILL);
//        mPaint.setColor(Color.BLACK);
//        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mPaint);
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setColor(Color.WHITE);
//        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mPaint);
    }
}