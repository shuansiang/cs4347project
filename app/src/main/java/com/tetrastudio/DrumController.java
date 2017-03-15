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
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Data
    private LinkedList<float[]> mAccelHistory = new LinkedList<>();
    private float[] mAccelerometerVal = new float[4];

    // Sound
    private SoundPool mSoundPool = null;

    private int[] pianoSoundIds = {
            R.raw.c_piano,
            R.raw.d_piano,
            R.raw.e_piano,
            R.raw.f_piano,
            R.raw.g_piano,
            R.raw.a_piano,
            R.raw.b_piano
    };
    private int[] mLoadedPianoIds;


    public DrumController(Context context, Activity parentActivity, CameraBridgeViewBase cameraView) {
        mContext = context;
        mOpenCvCameraView = cameraView;
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setAlpha(0);
        mOpenCvCameraView.setCvCameraViewListener(new DrumCameraListener());

        mParentActivity = parentActivity;
        mDebugTextView = ((VSDActivity) mParentActivity).getDebugGrav();

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
        mLoadedPianoIds = new int[pianoSoundIds.length];
        for (int i = 0; i < pianoSoundIds.length; i++) {
            mLoadedPianoIds[i] = mSoundPool.load(mContext, pianoSoundIds[i], 1);
        }
    }

    public void playSoundPress(View v) {
        Log.d("SP","PLAYING SOUND: "+0);
        mSoundPool.play(mLoadedPianoIds[0], 1, 1, 1, 0, 0 + 1);
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
            mDebugTextView.setText(String.format("X:%f\nY:%f\nZ:%f", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
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
        Log.d("TTS", "Sampling: " + currentAccelerometer[1]);

        StringBuilder str = new StringBuilder();
        for (float[] val : mAccelHistory) {
            str.append(String.format("%.3f", val[1]));
            str.append(',');
        }
        Log.d("TTSL", str.toString());

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
        if (mAccelHistory.get(mAccelHistory.size() - 1)[1] < 2 && maxSwing > 2) {
            // End of hit
            Log.d("TTS", "HIT!");
            playSoundPress(null);
            mAccelHistory = new LinkedList<>();
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
//            Log.d("SP", "Mean: " + mean.val[0] + ", " + mean.val[1] + ", " + mean.val[2]);
            mCurrentAverageColor = mean;
            return inputFrame.rgba();
        }
    }

}
