package com.tetrastudio;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Eyx on 9/3/2017.
 */

public class DrumController extends ControllerBase {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Context mContext;
    private Activity mParentActivity;

    private Scalar mCurrentAverageColor;

    // Test views
    private TextView mDebugTextView;

    private int[] mGlowDrumViewIds = {
            R.id.ride_cymbal_glow,
            R.id.tom_2_glow,
            R.id.bass_drum_glow,
            R.id.tom_1_glow,
            R.id.crash_symbal_glow,
    };

    private ArrayList<View> mGlowDrumViews;

    // Data
    private LinkedList<float[]> mAccelHistory = new LinkedList<>();
    private float[] mAccelerometerVal = new float[4];
    private boolean mIsSwingingDown = false;
    private int mActiveDrum = 0;

    // Sound
    private SoundPool mSoundPool = null;

    private int[] mDrumSoundIds = {
            R.raw.drum_ride,
            R.raw.drum_tom_2,
            R.raw.drum_bass,
            R.raw.drum_tom_1,
            R.raw.drum_crash_cymbal,
    };

    private Scalar[] mDrumColours = {
            new Scalar(58.393345269097225, 166.12298177083332, 101.20939127604167), // Green
            new Scalar(192.87634548611112, 120.40522243923611, 107.47523546006944), // Orange
            new Scalar(175.71397135416666, 113.93810004340278, 157.77175455729167), // Pink
            new Scalar(148.47259114583332, 144.60708658854168, 144.38650390625), // Beige
            new Scalar(58, 58, 58), // Black
    };

    private int[] mLoadedDrumIds;


    public DrumController(Context context, Activity parentActivity, CameraBridgeViewBase cameraView) {
        mContext = context;
        mOpenCvCameraView = cameraView;
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setAlpha(0);
        mOpenCvCameraView.disableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(new DrumCameraListener());

        mParentActivity = parentActivity;
        mDebugTextView = ((VSDActivity) mParentActivity).getDebugGrav();

        mGlowDrumViews = new ArrayList<>();
        for (int i = 0; i < mGlowDrumViewIds.length; i++) {
            mGlowDrumViews.add(mParentActivity.findViewById(mGlowDrumViewIds[i]));
        }

        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.d("SP", "Sound " + sampleId + " loaded");
            }
        });

        loadDrumSounds();

    }

    private void loadDrumSounds() {
        mLoadedDrumIds = new int[mDrumSoundIds.length];
        for (int i = 0; i < mDrumSoundIds.length; i++) {
            mLoadedDrumIds[i] = mSoundPool.load(mContext, mDrumSoundIds[i], 1);
        }
    }

    private void updateDrumView() {
        for (int i = 0; i < mGlowDrumViews.size(); i++) {
            if (i != mActiveDrum) {
                mGlowDrumViews.get(i).setVisibility(View.INVISIBLE);
            } else {
                if (mIsEnabled) {
                    mGlowDrumViews.get(i).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void playSoundPress(View v, float volume) {
        Log.d("SP", "PLAYING SOUND: " + 0);
        mSoundPool.play(mLoadedDrumIds[mActiveDrum], volume, volume, 1, 0, 0 + 1);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
        for (View view : mGlowDrumViews) {
            if (!mIsEnabled) {
               view.setVisibility(View.INVISIBLE);
            }
        }
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
//        float[] accelerometerValues = new float[0];
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerVal = MathUtils.lowPass(sensorEvent.values.clone(), mAccelerometerVal);
//            mDebugTextView.setText(String.format("X:%f\nY:%f\nZ:%f", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
        }

        long deltaTime = updateDeltaTime();
        if (deltaTime < UPDATE_INTERVAL) {
            return;
        }
        mPreviousUpdateTimestamp = System.currentTimeMillis();

        update(deltaTime, mAccelerometerVal);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void update(long deltaTimeMillis, float[] accelerometer) {
//        checkFlick(deltaTimeMillis, previousOrientation, currentOrientation);
        updateLinearAccelSound(deltaTimeMillis, accelerometer);
        updateDrumView();
    }

    private void updateLinearAccelSound(long deltaTimeMillis, float[] currentAccelerometer) {
//        mSensorDebugLabel.setText(String.format("PitchR:%f\nGain:%f\nMaxVol:%f" +
//                        "\nMh: %f\nPitch:%f\nRoll:%f", pitchR, gain, mStreamingTrack.getMaxVolume(),
//                Math.toDegrees(currentOrientation[0]), Math.toDegrees(currentOrientation[1]),
//                Math.toDegrees(currentOrientation[2])));
        if (!mIsEnabled) {
            return;
        }
//        else if (mAccelHistory.size() > 0) {
//            mAccelHistory.clear();
//        }

//        if (mAccelHistory.size()>0 && currentAccelerometer[1] == mAccelHistory.get(mAccelHistory.size()-1)[1]) {
//            return;
//        }
        mAccelHistory.add(currentAccelerometer.clone());
//        Log.d("TTS", "Sampling: " + currentAccelerometer[1]);

//        StringBuilder str = new StringBuilder();
//        for (float[] val : mAccelHistory) {
//            str.append(String.format("%.3f", val[1]));
//            str.append(',');
//        }
//        Log.d("TTSL", str.toString());

        if (mAccelHistory.size() < 10) {
            return;
        }

        if (mAccelHistory.size() >= 13) {
            for (int i = 0; i < 3; i++) {
                mAccelHistory.removeFirst();
//                mAccelHistory.remove(0);
            }
        }
//        mAccelHistory = new <>(mAccelHistory.subList(mAccelHistory.size() - 3, mAccelHistory.size()));
        float maxSwing = MathUtils.max(mAccelHistory, 1);

//        Log.d("SP", ""+currentAccelerometer[1]);
//        Log.d("SP", String.format("Max of Swing: %.3f | Last: %.3f", maxSwing, mAccelHistory.get(mAccelHistory.size()-1)[1]));
        if (MathUtils.getOverallSlope(mAccelHistory, 1) < 0) {
            mIsSwingingDown = true;
        } else {
            mIsSwingingDown = false;
        }
        if (mAccelHistory.get(mAccelHistory.size() - 1)[1] < 2 && maxSwing > 2) {
            // End of hit
            float volume = Math.max(Math.min(1.0f, (maxSwing - 2) / 7.0f), 0.0f);
            Log.d("TTS", "HIT! " + volume);
            playSoundPress(null, volume);
            mAccelHistory = new LinkedList<>();
        }
    }

    private int getClosestColour(Scalar colour) {
        double minDistance = Double.MAX_VALUE;
        int minIndex = 0, i = 0;
        for (Scalar drumColour : mDrumColours) {
            double distance = Core.norm(new Mat(1, 3, CvType.CV_16SC4, colour), new Mat(1, 3, CvType.CV_16SC4, drumColour));
            if (distance < minDistance) {
                minDistance = distance;
                minIndex = i;
            }
            i++;
        }
        return minIndex;
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
            if (!mIsSwingingDown) {
                Scalar mean = Core.mean(inputFrame.rgba());
                int closestColourIndex = getClosestColour(mean);
//                Log.d("SP", "Mean: " + mean.val[0] + ", " + mean.val[1] + ", " + mean.val[2]);
                mActiveDrum = closestColourIndex;
    //            Log.d("SP", "Closest Colour Index: " + closestColourIndex);
                mCurrentAverageColor = mean;
            }
            return inputFrame.rgba();
        }
    }

}
