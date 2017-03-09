package com.tetrastudio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

/**
 * Created by Eyx on 9/3/2017.
 */

public class DrumController {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Context mContext;
    private Activity mParentActivity;

    private Scalar mCurrentAverageColor;

    public DrumController(Context context, Activity parentActivity, CameraBridgeViewBase cameraView) {
        mContext = context;
        mOpenCvCameraView = cameraView;
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setAlpha(0);
        mOpenCvCameraView.setCvCameraViewListener(new DrumCameraListener());

        mParentActivity = parentActivity;
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(mContext) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    public void enableCamera() {
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, mContext, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private class DrumCameraListener implements CameraBridgeViewBase.CvCameraViewListener2 {
        @Override
        public void onCameraViewStarted(int width, int height) {

        }

        @Override
        public void onCameraViewStopped() {

        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            Scalar mean = Core.mean(inputFrame.rgba());
            Log.d("SP", "Mean: " + mean.val[0] + ", " + mean.val[1] + ", " + mean.val[2]);
            mCurrentAverageColor = mean;
            return inputFrame.rgba();
        }
    }

}
