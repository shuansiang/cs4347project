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
    private static final int FORCE_THRESHOLD_SOFT = 150;
    private static final int FORCE_THRESHOLD_HARD = 1000;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 5;

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
            R.raw.hard,
            R.raw.soft,
    };

    private int[] mLoadedShakerIds;

    public interface OnShakeListener {
        public void onShake(int id);
    }

    public ShakeListener(Context context, Activity parentActivity) {

        Log.d(TAG, "ShakeListener invoked---->");
        mContext = context;

        mShakerGlowView = parentActivity.findViewById(R.id.shaker_button_glow);

        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mLoadedShakerIds = new int[mShakerSoundIds.length];
        for (int i = 0; i < mShakerSoundIds.length; i++) {
            mLoadedShakerIds[i] = mSoundPool.load(mContext, mShakerSoundIds[i], 1);
        }

//        resume();

        setOnShakeListener(new ShakeListener.OnShakeListener() {
            public void onShake(int id) {
                if (mIsEnabled) {
                    playSound(id);
                }
//                if (mToast != null) {
//                    mToast.cancel();
//                }
                if (id > 0) {
//                    mToast = Toast.makeText(VSDActivity.this, "Hard Shake", Toast.LENGTH_SHORT);
//                    mToast.show();
                } else {
//                    mToast = Toast.makeText(VSDActivity.this, "Soft Shake", Toast.LENGTH_SHORT);
//                    mToast.show();
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
        Log.d(TAG, "ShakeListener setOnShakeListener invoked---->");
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
                    + event.values[1]
                    + event.values[2] - mLastX - mLastY
                    - mLastZ)
                    / diff * 10000;
            if (speed > FORCE_THRESHOLD_HARD) {
                checkSpeed(1, now);
            } else if (speed > FORCE_THRESHOLD_SOFT) {
                checkSpeed(0, now);
            }
            mLastTime = now;
            mLastX = event.values[0];
            mLastY = event.values[1];
            mLastZ = event.values[2];
            Log.d(TAG, "X,Y,Z Values: " + mLastX + ", " + mLastY + ", " + mLastZ);
        }
    }

    private void checkSpeed(int id, long now) {
        if ((++mShakeCount >= SHAKE_COUNT)
                && (now - mLastShake > SHAKE_DURATION)) {
            mLastShake = now;
            mShakeCount = 0;
            Log.d(TAG, "ShakeListener mShakeListener---->" + mShakeListener);
            if (mShakeListener != null) {
                mShakeListener.onShake(id);
            }
        }
        mLastForce = now;
    }

    protected void playSound(int id) {
        MediaPlayer mediaPlayer;
        if (id > 0) {
            mSoundPool.play(mLoadedShakerIds[0], 1, 1, 1, 0, 0 + 1);
        } else {
            mSoundPool.play(mLoadedShakerIds[1], 1, 1, 1, 0, 0 + 1);
//            mediaPlayer = MediaPlayer.create(this, R.raw.soft);
        }

//        mediaPlayer.start();

    }
}
