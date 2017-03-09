//package com.example.cs4347project.cs4347project;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.media.AudioFormat;
//import android.media.AudioManager;
//import android.media.AudioTrack;
//import android.media.SoundPool;
//import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.Button;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.JavaCameraView;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.core.Scalar;
//
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//
//// Some code adapted from http://stackoverflow.com/questions/13679568/using-android-gyroscope-instead-of-accelerometer-i-find-lots-of-bits-and-pieces
//public class MainTestingActivity extends AppCompatActivity implements SensorEventListener {
//
//    private static final int CAMERA_REQUEST_CODE = 123;
//
//    public enum FadeType {
//        NONE,
//        FADE_IN,
//        FADE_OUT
//    }
//
//    public enum Axis {
//        X, Y, Z
//    }
//
//    private static final float FLICK_INTERVAL = 180, UPDATE_INTERVAL = 50;
//    private SensorManager mSensorManager;
//    private Sensor mGyroSensor, mAccelSensor, mMagneticSensor, mGravitySensor, mLinearAccelSensor;
//
//    private TextView mSensorDebugLabel, mSoundToPlayLabel;
//    private Button mAddBufferButton, mEnableViolinButton;
//
//    private static final float NS2S = 1.0f / 1000000000.0f;
//    private final float[] deltaRotationVector = new float[4];
//    private float[] initialRotationMatrix = new float[9];
//    private float[] currentRotationMatrix = new float[9];
//    private float[] rotationMatrix2 = new float[9];
//    private float[] inclinationMatrix = new float[9];
//    private final double EPSILON = 0.000000001;
//    private static final int SAMPLE_RATE = 44100;
//
//    private long mPreviousUpdateTimestamp, mLastFlickTimestamp;
//
//    private boolean mHasInitialOrientation, mHasGravity = false, mHasMag = false, mHasAccel = false;
//    private float[] mGravity = new float[4];
//    private float[] mGeomagnetic = new float[4];
//    private float[] orientation = new float[3]; // Yaw pitch roll in radians
//    private float[] mLinearAccel = new float[3]; // X Y Z linear acceleration
//    private float[] mLinearAccelOffset = new float[3]; // X Y Z offset
//
//    private SoundPool mSoundPool = null;
//    private AudioTrack mStreamingTrack = null;
//    private Thread mAudioWriteThread, mAudioStopThread;
//    private AudioWriteRunnable mAudioWriteRunnable;
//    private AudioStopRunnable mAudioStopRunnable;
//
//    private boolean mUseAudioTrack = true, mIsPlayingViolin = false, mViolinJustStopped = false,
//            mViolinJustStarted = false, mViolinEnabled = false;
//
//    private CameraBridgeViewBase mOpenCvCameraView;
//    private LinearLayout mAverageColourLayout;
//
//    private int[] pianoSoundIds = {
//            R.raw.c_piano,
//            R.raw.d_piano,
//            R.raw.e_piano,
//            R.raw.f_piano,
//            R.raw.g_piano,
//            R.raw.a_piano,
//            R.raw.b_piano
//    };
//
//    private int[] mLoadedPianoIds;
//
//    private byte[] mViolinCBuf;
//    private int mLastViolinBufIndex = 0; // Index of the last
//
//    private int mCurrentSoundId = 0, mPreviousSoundId = -1;
//    private int mOctaveOffset = 0;
//
//    protected final float ALPHA = 0.25f;
//    protected final float LINEAR_ACCEL_THRESHOLD = 0.25f;
//
//    private ArrayList<Float> mAccelHistory = new ArrayList<>();
//
//    private ArrayList<Integer> mLastSoundIds = new ArrayList<>();
//
//    public static int mode(Integer a[]) {
//        int maxValue=0, maxCount=0;
//
//        for (int i = 0; i < a.length; ++i) {
//            int count = 0;
//            for (int j = 0; j < a.length; ++j) {
//                if (a[j] == a[i]) ++count;
//            }
//            if (count > maxCount) {
//                maxCount = count;
//                maxValue = a[i];
//            }
//        }
//
//        return maxValue;
//    }
//
//    class AudioWriteRunnable implements Runnable {
//        public volatile float mFrequency = 440.0f;
//
//        private long mLastCallTime = -1;
//
//        public AudioWriteRunnable() {
//
//        }
//
//        public void run() {
//            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
//            while (mUseAudioTrack) {
//                long currentTime = System.currentTimeMillis();
//                if (mIsPlayingViolin && mViolinEnabled) {
//                    if (mLastCallTime < 0) {
//                        mLastCallTime = System.currentTimeMillis();
//                    }
////                    float duration = (currentTime - mLastCallTime) * 0.001f;
//                    float period = 1.0f / mFrequency;
//                    float duration = (float) (period * Math.ceil(0.05f / period));
////                    byte[] buffer = getViolinBuffer(261.6f, -1, FadeType.NONE, 0);
//                    Log.d("SP", "D: " + duration + " / MLV: " + mLastViolinBufIndex);
////                    if (duration * SAMPLE_RATE <= 0) {
////                        duration = 1.0f / SAMPLE_RATE;
////                    }
//                    byte[] buffer = getSoundBuffer(mFrequency, duration, FadeType.NONE);
////                    byte[] buffer = getViolinBuffer(mFrequency, duration, mLastViolinBufIndex, FadeType.NONE);
//                    Log.d("SP", "Freq: " + mFrequency);
//                    mStreamingTrack.write(buffer, 0, buffer.length);
//                } else {
//                    // Stopping is handled in the AudioStopRunnable since write will block this thread
//                }
//                mLastCallTime = currentTime;
//            }
//        }
//    }
//
//    class AudioStopRunnable implements Runnable {
//
//        public AudioStopRunnable() {
//
//        }
//
//        public void run() {
//            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
//            while (mUseAudioTrack) {
//                if (!mIsPlayingViolin) {
//                    if (mViolinJustStopped == true) {
//                        int playPos = mStreamingTrack.getPlaybackHeadPosition();
//                        Log.d("SP", "MVJS: Just stopped " + playPos);
//                        mStreamingTrack.pause();
//                        mStreamingTrack.flush();
//                        try {
//                            Thread.sleep(1);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        mStreamingTrack.play();
////                        float period = 1.0f / mFrequency;
////                        float duration = (float) (period * Math.ceil(0.25f / period));
////                        byte[] buffer = getViolinBuffer(0, 0.5f, FadeType.FADE_OUT, playPos);
////                        byte[] buffer = getSoundBuffer(0, 0.5f, FadeType.FADE_OUT);
////                        mStreamingTrack.write(buffer, 0, buffer.length);
//                        mViolinJustStopped = false;
//                    }
//                }
//            }
//        }
//    }
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        mSensorDebugLabel = (TextView) findViewById(R.id.sensorLabel);
//        mSoundToPlayLabel = (TextView) findViewById(R.id.currentSoundLabel);
//
//        mAddBufferButton = (Button) findViewById(R.id.addToBufferButton);
//        mAddBufferButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
//                        motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
//                    mIsPlayingViolin = false;
//                    mViolinJustStopped = true;
//                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    mIsPlayingViolin = true;
//                    mViolinJustStarted = true;
//                }
//                return true;
//            }
//        });
//
//        mEnableViolinButton = (Button) findViewById(R.id.enableViolinButton);
//        mEnableViolinButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
//                        motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
//                    mViolinEnabled = false;
//                    if (mIsPlayingViolin) {
//                        mViolinJustStopped = true;
//                    }
//                    mIsPlayingViolin = false;
//                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    mViolinEnabled = true;
//                }
//                return true;
//            }
//        });
//        mAverageColourLayout = (LinearLayout) findViewById(R.id.averageColourLayout);
//        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
//        mLinearAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//
//        // Load audio file
//        InputStream is = getResources().openRawResource(R.raw.c_violin_ensemble);
//        try {
//            int fileLength = is.available();
//            DataInputStream dis = new DataInputStream(getResources().openRawResource(R.raw.c_violin_ensemble));
//            mViolinCBuf = new byte[fileLength];
//            Log.d("SP", "VCB: " + fileLength);
//            dis.readFully(mViolinCBuf);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
//        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//            @Override
//            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
//                Log.d("SP", "Sound " + sampleId + " loaded");
//            }
//        });
//        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
//        mStreamingTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
//                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
//                minBufferSize, AudioTrack.MODE_STREAM);
//        Log.d("SP", "MIN BUF SIZE: " + minBufferSize);
//
//        mAudioWriteRunnable = new AudioWriteRunnable();
//        mAudioWriteThread = new Thread(mAudioWriteRunnable);
//
//        mAudioStopRunnable = new AudioStopRunnable();
//        mAudioStopThread = new Thread(mAudioStopRunnable);
//
////        getSoundBuffer(440);
//        mStreamingTrack.play();
//        mAudioWriteThread.start();
//        mAudioStopThread.start();
//
//        loadPianoSounds();
//
//        // Camera test
//
//        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
//
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setAlpha(0);
//
//        mOpenCvCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
//            @Override
//            public void onCameraViewStarted(int width, int height) {
//
//            }
//
//            @Override
//            public void onCameraViewStopped() {
//
//            }
//
//            @Override
//            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//                Scalar mean = Core.mean(inputFrame.rgba());
//                Log.d("SP", "Mean: "+mean.val[0]+", "+mean.val[1]+ ", "+mean.val[2]);
//                int averageColor = Color.rgb((int) mean.val[0], (int) mean.val[1], (int) mean.val[2]);
//                runOnUiThread(new UpdateBackground(averageColor));
//                return inputFrame.rgba();
//            }
//        });
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.CAMERA},
//                CAMERA_REQUEST_CODE);
//
//    }
//
//    private class UpdateBackground implements Runnable {
//
//        private int mColor;
//
//        public UpdateBackground(int color) {
//            mColor = color;
//        }
//
//        @Override
//        public void run() {
//            Log.d("SP", "Color: "+mColor);
//            mAverageColourLayout.setBackgroundColor(mColor);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case CAMERA_REQUEST_CODE: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // permission was granted, yay!.
//                    enableCamera();
//
//                } else {
//                    // permission denied, boo! Disable the
//                    // functionality that depends on this permission.
//                }
//                return;
//            }
//        }
//    }
//
//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.i("OpenCV", "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };
//
//    private void enableCamera() {
//        if (!OpenCVLoader.initDebug()) {
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
//        } else {
//            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
//    }
//
//    private float getAverageLinearY() {
//        float sum = 0;
//        for (int i = 0; i < mAccelHistory.size(); i++) {
//            sum += (Math.max(0.1, (float) i / (mAccelHistory.size() - 1))) * Math.abs(mAccelHistory.get(i));
//        }
//        return sum / mAccelHistory.size();
//    }
//
//    public static Axis getFastestAxis(float[] readings) {
//        float x = Math.abs(readings[0]);
//        float y = Math.abs(readings[1]);
//        float z = Math.abs(readings[2]);
//        if (x > y && x > z) {
//            return Axis.X;
//        } else if (y > x && y > z) {
//            return Axis.Y;
//        } else {
//            return Axis.Z;
//        }
//    }
//
//    /**
//     * @param frequency frequency in hertz
//     * @param duration  time in seconds
//     * @return amplitude buffer
//     */
//    protected byte[] getSoundBuffer(float frequency, float duration, FadeType fadeType) {
//        byte[] buffer = new byte[(int) (duration * SAMPLE_RATE)];
//        float[] fadeBuffer = new float[(int) (duration * SAMPLE_RATE)];
//        if (fadeType != FadeType.NONE) {
//            for (int i = 0; i < fadeBuffer.length; i++) {
//                float delta = ((float) i) / (fadeBuffer.length - 1);
//                fadeBuffer[i] = (fadeType == FadeType.FADE_IN ? delta : 1 - delta);
//            }
//        }
//
//        for (int i = 0; i < buffer.length; i++) {
//            double amplitude = Math.sin((2.0 * Math.PI * frequency / SAMPLE_RATE * (double) i));
//            buffer[i] = (byte) (amplitude * Byte.MAX_VALUE);
//            if (fadeType != FadeType.NONE) {
//                buffer[i] *= fadeBuffer[i];
//            }
//        }
//        return buffer;
//
//    }
//
//    protected byte[] getViolinBuffer(float frequency, float duration, FadeType fadeType, int startPos) {
//        if (duration < 0) {
//            return mViolinCBuf;
//        }
//        int endPos = startPos + (int) (duration * SAMPLE_RATE);
//        if (endPos > mViolinCBuf.length) {
//            endPos = mViolinCBuf.length - 1;
//        }
//        if (startPos > endPos) {
//            startPos = 0;
//            endPos = Math.min((int) duration * SAMPLE_RATE, mViolinCBuf.length - 1);
//        }
//        byte[] buffer = new byte[endPos - startPos + 1];
//        float[] fadeBuffer = new float[endPos - startPos + 1];
//        if (fadeType != FadeType.NONE) {
//            for (int i = 0; i < fadeBuffer.length; i++) {
//                float delta = ((float) i) / (fadeBuffer.length - 1);
//                fadeBuffer[i] = (fadeType == FadeType.FADE_IN ? delta : 1 - delta);
//            }
//        }
//        for (int i = 0; i < buffer.length; i++) {
//            int violinPos = startPos + i;
//            buffer[i] = mViolinCBuf[violinPos];
//            if (fadeType != FadeType.NONE) {
//                buffer[i] *= fadeBuffer[i];
//            }
//        }
//        return buffer;
//    }
//
//    public void onAddSoundClicked(View v) {
////        mIsPlayingViolin = true;
//    }
//
//    protected void loadPianoSounds() {
//        mLoadedPianoIds = new int[pianoSoundIds.length];
//        for (int i = 0; i < pianoSoundIds.length; i++) {
//            mLoadedPianoIds[i] = mSoundPool.load(this, pianoSoundIds[i], 1);
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mLinearAccelSensor, SensorManager.SENSOR_DELAY_GAME);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mSensorManager.unregisterListener(this);
//    }
//
//    @Override
//    public void onSensorChanged(SensorEvent sensorEvent) {
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            mGeomagnetic = lowPass(sensorEvent.values.clone(), mGeomagnetic);
//            mHasMag = true;
//        }
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
//            mGravity = lowPass(sensorEvent.values.clone(), mGravity);
//            mHasGravity = true;
//        }
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !mHasGravity) {
//            mGravity = lowPass(sensorEvent.values.clone(), mGravity);
//            mHasAccel = true;
//        }
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//            mLinearAccel = lowPass(sensorEvent.values.clone(), mLinearAccel);
//        }
//        long timestamp = System.currentTimeMillis();
//        long deltaTime = timestamp - mPreviousUpdateTimestamp;
//        if (deltaTime < UPDATE_INTERVAL) {
//            return;
//        }
//        float[] previousOrientation = new float[3];
//        copyFloat(orientation, previousOrientation);
//
//        if ((mHasAccel || mHasGravity) && mHasMag) {
//            calculateOrientation();
//        }
//
//        updateSoundToPlay();
//
//
//        mPreviousUpdateTimestamp = timestamp;
//
//        update(deltaTime, previousOrientation, orientation);
//
//    }
//
////    private void getGyroscope(SensorEvent event) {
////        float[] values = event.values;
////        float x = values[0];
////        float y = values[1];
////        float z = values[2];
////
////        if (timestamp != 0) {
////            final float dT = (event.timestamp - timestamp) * NS2S;
////            // Axis of the rotation sample, not normalized yet.
////            float axisX = event.values[0];
////            float axisY = event.values[1];
////            float axisZ = event.values[2];
////
////            // Calculate the angular speed of the sample
////            double omegaMagnitude = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
////
////            // Normalize the rotation vector if it's big enough to get the axis
////            if (omegaMagnitude > EPSILON) {
////                axisX /= omegaMagnitude;
////                axisY /= omegaMagnitude;
////                axisZ /= omegaMagnitude;
////            }
////
////            // Integrate around this axis with the angular speed by the timestep
////            // in order to get a delta rotation from this sample over the timestep
////            // We will convert this axis-angle representation of the delta rotation
////            // into a quaternion before turning it into the rotation matrix.
////            double thetaOverTwo = omegaMagnitude * dT / 2.0f;
////            double sinThetaOverTwo = Math.sin(thetaOverTwo);
////            double cosThetaOverTwo = Math.cos(thetaOverTwo);
////            deltaRotationVector[0] = (float) (sinThetaOverTwo * axisX);
////            deltaRotationVector[1] = (float) (sinThetaOverTwo * axisY);
////            deltaRotationVector[2] = (float) (sinThetaOverTwo * axisZ);
////            deltaRotationVector[3] = (float) (cosThetaOverTwo);
////        }
////        timestamp = event.timestamp;
////        float[] deltaRotationMatrix = new float[9];
////        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
////        // User code should concatenate the delta rotation we computed with the current rotation
////        // in order to get the updated rotation.
////        currentRotationMatrix = matrixMultiplication(currentRotationMatrix, deltaRotationMatrix);
////
////        float[] orientationValues = new float[3];
////        SensorManager.getOrientation(currentRotationMatrix, orientationValues);
////        mSensorDebugLabel.setText(String.format("%f, %f, %f", orientationValues[0], orientationValues[1], orientationValues[2]));
////        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
////    }
//
//    private void update(long deltaTimeMillis, float[] previousOrientation, float[] currentOrientation) {
//        checkFlick(deltaTimeMillis, previousOrientation, currentOrientation);
//        updateLinearAccelSound(deltaTimeMillis, previousOrientation, currentOrientation);
//    }
//
//    private void updateLinearAccelSound(long deltaTimeMillis, float[] previousOrientation, float[] currentOrientation) {
//        float pitchR = (float) Math.toDegrees(currentOrientation[1]); // Rotational pitch (not the frequency pitch)
//        float gain = 1 - ((pitchR + 50) / 140.0f) * 1;
////        mStreamingTrack.setPlaybackRate((int)(gain * 44100));
//        mStreamingTrack.setVolume(gain);
//        mSensorDebugLabel.setText(String.format("PitchR:%f\nGain:%f\nMaxVol:%f" +
//                        "\nMh: %f\nPitch:%f\nRoll:%f", pitchR, gain, mStreamingTrack.getMaxVolume(),
//                Math.toDegrees(currentOrientation[0]), Math.toDegrees(currentOrientation[1]),
//                Math.toDegrees(currentOrientation[2])));
//        if (mViolinEnabled) {
//            mAccelHistory.add(mLinearAccel[1]);
//        } else if (mAccelHistory.size() > 0) {
//            mAccelHistory.clear();
//        }
//
//        if (mAccelHistory.size() < 8) {
//            return;
//        }
//        if (getAverageLinearY() >= LINEAR_ACCEL_THRESHOLD) {
//            mIsPlayingViolin = true;
//            mViolinJustStarted = true;
//        } else {
//            if (mIsPlayingViolin) {
//                mViolinJustStopped = true;
//            }
//            mIsPlayingViolin = false;
//        }
//        mAccelHistory = new ArrayList<Float>(mAccelHistory.subList(mAccelHistory.size() - 3, mAccelHistory.size()));
//    }
//
//
//    private void checkFlick(long deltaTimeMillis, float[] previousOrientation, float[] currentOrientation) {
//        double deltaRoll = Math.toDegrees(currentOrientation[2]) - Math.toDegrees(previousOrientation[2]);
//        double rollSpeed = deltaRoll / (deltaTimeMillis / 1000.0);
//
//        double shakeIntensity = Math.sqrt(0//Math.pow(currentOrientation[0] - previousOrientation[0], 2.0)
//                + Math.pow(currentOrientation[1] - previousOrientation[1], 2)
//                + Math.pow(currentOrientation[2] - previousOrientation[2], 2)) / deltaTimeMillis * 1000;
////        Log.d("SP", "Shake: " + shakeIntensity);
//
//        long currentTimestamp = System.currentTimeMillis();
//
////        if (rollSpeed != 0) Log.d("SP", "Delta rollspeed: " + rollSpeed);
//        if (shakeIntensity >= 25.2) {
//            if (currentTimestamp - mLastFlickTimestamp <= FLICK_INTERVAL) {
////                Log.d("SP", "Too soon");
//                return;
//            }
////            Log.d("SP", "Delta rollspeed: " + rollSpeed);
//            mLastFlickTimestamp = currentTimestamp;
//            playSoundPress(null);
//        }
//    }
//
//    private void copyFloat(float[] source, float[] dest) {
//        if (source.length != dest.length) {
//            return;
//        }
//        for (int i = 0; i < source.length; i++) {
//            dest[i] = source[i];
//        }
//    }
//
//    protected float[] lowPass(float[] input, float[] output) {
//        if (output == null) return input;
//        for (int i = 0; i < input.length; i++) {
//            output[i] = output[i] + ALPHA * (input[i] - output[i]);
//        }
//        return output;
//    }
//
//    private void calculateOrientation() {
//        mHasInitialOrientation = SensorManager.getRotationMatrix(
//                initialRotationMatrix, null, mGravity, mGeomagnetic);
//        SensorManager.remapCoordinateSystem(initialRotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotationMatrix2);
//        SensorManager.getOrientation(rotationMatrix2, orientation);
//        orientation = lowPass(orientation.clone(), orientation);
////        float incl = SensorManager.getInclination(inclinationMatrix);
////        mSensorDebugLabel.setText(String.format("Mh: %f\nPitch:%f\nRoll:%f\nYaw:%f\nIncl:%f", Math.toDegrees(orientation[0]),
////                Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2]), Math.toDegrees(orientation[0]), Math.toDegrees(incl)));
////        mSensorDebugLabel.setText(String.format("LA X: %f\nLA Y:%f\nLA Z:%f\nALY: %f", mLinearAccel[0],
////                mLinearAccel[1], mLinearAccel[2], getAverageLinearY()));
//
//    }
//
//    private void updateSoundToPlay() {
//        int yaw = (int) Math.toDegrees(orientation[0]);
//        yaw += 180;
//        mCurrentSoundId = (int) Math.floor(yaw / 720.0f * pianoSoundIds.length) % pianoSoundIds.length;
//        mOctaveOffset = (int) Math.floor(yaw / 720.0f * pianoSoundIds.length) / pianoSoundIds.length;
//        if (mLastSoundIds.size() >= 5) {
//            mLastSoundIds.remove(0);
//        }
//        mLastSoundIds.add(mCurrentSoundId);
//        mCurrentSoundId = mode(mLastSoundIds.toArray(new Integer[mLastSoundIds.size()]));
////        Log.d("SP", "USTP: "+mCurrentSoundId);
//        mSoundToPlayLabel.setText(String.format("Sound: %d, Octave offset: %d, Yaw:%d", mCurrentSoundId, mOctaveOffset, yaw));
//
////        if (yaw > 180) {
////            mAudioWriteRunnable.mFrequency = 523.5f;
////        } else {
////            mAudioWriteRunnable.mFrequency = 440;
////        }
//        mAudioWriteRunnable.mFrequency = (int) ((180.0f-Math.abs(yaw-180))/180.0f * 200 + 440);
//
//    }
//
//    private float[] matrixMultiplication(float[] a, float[] b) {
//        float[] result = new float[9];
//
//        result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
//        result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
//        result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];
//
//        result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
//        result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
//        result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];
//
//        result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
//        result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
//        result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];
//
//        return result;
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int i) {
//
//    }
//
//
//    public void playSoundPress(View v) {
//        Log.d("SP","PLAYING SOUND: "+mCurrentSoundId);
//        mSoundPool.play(mLoadedPianoIds[mCurrentSoundId], 1, 1, 1, 0, mOctaveOffset + 1);
//    }
//}
