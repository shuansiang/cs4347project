package com.tetrastudio;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.content.Context;

import java.lang.UnsupportedOperationException;

public class ShakeListener extends ControllerBase {
    private String TAG = ShakeListener.class.getSimpleName();
    private static final int FORCE_THRESHOLD_SOFT = 520;
    private static final int FORCE_THRESHOLD_HARD = 1000;
    private static final int TIME_THRESHOLD = 50;
    private static final int SHAKE_TIMEOUT = 800;
    private static final int SHAKE_DURATION = 100;
    private static final int SHAKE_COUNT = 3;

    private SensorManager mSensorMgr;
    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context mContext;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;

    // Views
    private View mShakerGlowView;

    private SoundPool mSoundPool = null;

    private int[] mShakerSoundIds = {
            R.raw.shaker,
            R.raw.shaker,
    };

    private int[] mLoadedShakerIds;

    public interface OnShakeListener {
        public void onShake(float speed);
    }

    public ShakeListener(Context context, Activity parentActivity) {

//        Log.d(TAG, "ShakeListener invoked---->");
        mContext = context;

        mShakerGlowView = parentActivity.findViewById(R.id.shaker_button_glow);

        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mLoadedShakerIds = new int[mShakerSoundIds.length];
        for (int i = 0; i < mShakerSoundIds.length; i++) {
            mLoadedShakerIds[i] = mSoundPool.load(mContext, mShakerSoundIds[i], 1);
        }

//        resume();

        setOnShakeListener(new ShakeListener.OnShakeListener() {
            public void onShake(float speed) {
                if (mIsEnabled) {
                    playSound(speed);
                }
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
        if (mIsEnabled) {
            mShakerGlowView.setVisibility(View.VISIBLE);
        } else {
            mShakerGlowView.setVisibility(View.INVISIBLE);
        }
    }

    public void setOnShakeListener(OnShakeListener listener) {
//        Log.d(TAG, "ShakeListener setOnShakeListener invoked---->");
        mShakeListener = listener;
    }

    public void resume() {
        mSensorMgr = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = false;
        try {
            supported = mSensorMgr.registerListener(this,
                    mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME);
        } catch (Exception e) {
            Toast.makeText(mContext, "Shaking not supported", Toast.LENGTH_LONG)
                    .show();
        }

        if ((!supported) && (mSensorMgr != null))
            mSensorMgr.unregisterListener(this);
    }

    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this);
            mSensorMgr = null;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        long now = System.currentTimeMillis();

        if ((now - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }

        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(event.values[0]
                    //+ event.values[1]
                    //+ event.values[2] - mLastX - mLastY
                    //- mLastZ)
                    - mLastX)
                    / diff * 10000;
            if (speed >= FORCE_THRESHOLD_HARD) {
                checkSpeed(speed, now);
            } else if (speed >= FORCE_THRESHOLD_SOFT) {
                checkSpeed(speed, now);
            }
            mLastTime = now;
            mLastX = event.values[0];
            mLastY = event.values[1];
            mLastZ = event.values[2];
//            Log.d(TAG, "X,Y,Z Values: " + mLastX + ", " + mLastY + ", " + mLastZ);
//            Log.d(TAG, "Shake speed: " + speed);
        }
    }

    private void checkSpeed(float speed, long now) {
        if ((++mShakeCount >= SHAKE_COUNT)
                && (now - mLastShake > SHAKE_DURATION)) {
            mLastShake = now;
            mShakeCount = 0;
//            Log.d(TAG, "ShakeListener mShakeListener---->" + mShakeListener);
            if (mShakeListener != null) {
                mShakeListener.onShake(speed);
            }
        }
        mLastForce = now;
    }

    protected void playSound(float speed) {
//        if (id > 0) {
        Log.d("SP", "ActivatedShake speed: " + speed);
        float maxSpeed = 1800.0f;
        float volume = Math.min(1, 0.1f + (float) Math.pow((Math.min(speed, maxSpeed) / maxSpeed), 1.5f));
        mSoundPool.play(mLoadedShakerIds[0], volume, volume, 1, 0, 1);
//        } else {
//            mSoundPool.play(mLoadedShakerIds[1], 1, 1, 1, 0, 0 + 1);
//            mediaPlayer = MediaPlayer.create(this, R.raw.soft);
//        }

//        mediaPlayer.start();

    }
}
