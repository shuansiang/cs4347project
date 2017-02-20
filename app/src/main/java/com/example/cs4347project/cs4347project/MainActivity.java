package com.example.cs4347project.cs4347project;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

// Some code adapted from http://stackoverflow.com/questions/13679568/using-android-gyroscope-instead-of-accelerometer-i-find-lots-of-bits-and-pieces
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mGyroSensor, mAccelSensor, mMagneticSensor, mGravitySensor;

    private TextView mSensorDebugLabel, mSoundToPlayLabel;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float[] initialRotationMatrix = new float[9];
    private float[] currentRotationMatrix = new float[9];
    private float[] rotationMatrix2 = new float[9];
    private float[] inclinationMatrix = new float[9];
    private final double EPSILON = 0.000000001;
    private float timestamp;
    private boolean mHasInitialOrientation, mHasGravity = false, mHasMag = false, mHasAccel = false;
    private float[] mGravity = new float[4];
    private float[] mGeomagnetic = new float[4];
    private float[] orientation = new float[3];

    private MediaPlayer mMediaPlayer = null;
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

    private int[] loadedPianoIds;

    private int mCurrentSoundId = 0, mPreviousSoundId = -1;
    private int mOctaveOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorDebugLabel = (TextView) findViewById(R.id.sensorLabel);
        mSoundToPlayLabel = (TextView) findViewById(R.id.currentSoundLabel);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

//        mMediaPlayer = MediaPlayer.create(this, pianoSoundIds[mCurrentSoundId]);
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.d("SP", "Sound "+sampleId+" loaded");
            }
        });

        loadPianoSounds();
    }

    protected void loadPianoSounds() {
        loadedPianoIds = new int[pianoSoundIds.length];
        for (int i=0; i<pianoSoundIds.length; i++) {
            loadedPianoIds[i] = mSoundPool.load(this, pianoSoundIds[i], 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            getGyroscope(sensorEvent);
//        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = sensorEvent.values;
            mHasMag = true;
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
            mGravity = sensorEvent.values;
            mHasGravity = true;
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !mHasGravity) {
            mGravity = sensorEvent.values;
            mHasAccel = true;
        }

        if ((mHasAccel || mHasGravity) && mHasMag) {
            calculateOrientation();
        }

        updateSoundToPlay();

//        if (mGravity != null && mGeomagnetic != null && !mHasInitialOrientation) {
//            calculateOrientation();
//        }
    }

//    private void getGyroscope(SensorEvent event) {
//        float[] values = event.values;
//        float x = values[0];
//        float y = values[1];
//        float z = values[2];
//
//        if (timestamp != 0) {
//            final float dT = (event.timestamp - timestamp) * NS2S;
//            // Axis of the rotation sample, not normalized yet.
//            float axisX = event.values[0];
//            float axisY = event.values[1];
//            float axisZ = event.values[2];
//
//            // Calculate the angular speed of the sample
//            double omegaMagnitude = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
//
//            // Normalize the rotation vector if it's big enough to get the axis
//            if (omegaMagnitude > EPSILON) {
//                axisX /= omegaMagnitude;
//                axisY /= omegaMagnitude;
//                axisZ /= omegaMagnitude;
//            }
//
//            // Integrate around this axis with the angular speed by the timestep
//            // in order to get a delta rotation from this sample over the timestep
//            // We will convert this axis-angle representation of the delta rotation
//            // into a quaternion before turning it into the rotation matrix.
//            double thetaOverTwo = omegaMagnitude * dT / 2.0f;
//            double sinThetaOverTwo = Math.sin(thetaOverTwo);
//            double cosThetaOverTwo = Math.cos(thetaOverTwo);
//            deltaRotationVector[0] = (float) (sinThetaOverTwo * axisX);
//            deltaRotationVector[1] = (float) (sinThetaOverTwo * axisY);
//            deltaRotationVector[2] = (float) (sinThetaOverTwo * axisZ);
//            deltaRotationVector[3] = (float) (cosThetaOverTwo);
//        }
//        timestamp = event.timestamp;
//        float[] deltaRotationMatrix = new float[9];
//        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
//        // User code should concatenate the delta rotation we computed with the current rotation
//        // in order to get the updated rotation.
//        currentRotationMatrix = matrixMultiplication(currentRotationMatrix, deltaRotationMatrix);
//
//        float[] orientationValues = new float[3];
//        SensorManager.getOrientation(currentRotationMatrix, orientationValues);
//        mSensorDebugLabel.setText(String.format("%f, %f, %f", orientationValues[0], orientationValues[1], orientationValues[2]));
//        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
//    }

    private void calculateOrientation() {
        mHasInitialOrientation = SensorManager.getRotationMatrix(
                initialRotationMatrix, null, mGravity, mGeomagnetic);
        SensorManager.remapCoordinateSystem(initialRotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotationMatrix2);
        SensorManager.getOrientation(rotationMatrix2, orientation);
        float incl = SensorManager.getInclination(inclinationMatrix);
        mSensorDebugLabel.setText(String.format("Mh: %f\nPitch:%f\nRoll:%f\nYaw:%f\nIncl:%f", Math.toDegrees(orientation[0]),
                Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2]), Math.toDegrees(orientation[0]), Math.toDegrees(incl)));

//        System.arraycopy(initialRotationMatrix, 0, currentRotationMatrix, 0, initialRotationMatrix.length);
    }

    private void updateSoundToPlay() {
        int yaw = (int) Math.toDegrees(orientation[0]);
        yaw += 180;

        mCurrentSoundId = (int) Math.floor(yaw / 180.0f * pianoSoundIds.length) % pianoSoundIds.length;
        mOctaveOffset = (int) Math.floor(yaw / 180.0f * pianoSoundIds.length) / pianoSoundIds.length;
        mSoundToPlayLabel.setText(String.format("Sound: %d, Octave offset: %d", mCurrentSoundId, mOctaveOffset));
    }

    private float[] matrixMultiplication(float[] a, float[] b) {
        float[] result = new float[9];

        result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
        result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
        result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

        result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
        result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
        result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

        result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
        result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
        result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public void playSoundPress(View v) {
        Log.d("SP", "Sound pressed");

        mSoundPool.play(loadedPianoIds[mCurrentSoundId], 1, 1, 1, 0, mOctaveOffset+1);
//
//        if (mPreviousSoundId != mCurrentSoundId) {
//            mPreviousSoundId = mCurrentSoundId;
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//            mMediaPlayer = MediaPlayer.create(this, loadedPianoIds[mCurrentSoundId]);
//        }
//        if (mMediaPlayer != null) {
//            if (mMediaPlayer.isPlaying()) {
//                mMediaPlayer.pause();
//            }
//            mMediaPlayer.seekTo(0);
//            mMediaPlayer.setLooping(false);
//            mMediaPlayer.start();
//        }
    }
}
