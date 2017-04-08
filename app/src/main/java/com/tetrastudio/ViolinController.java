package com.tetrastudio;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.tetrastudio.MathUtils.lowPass;

public class ViolinController extends ControllerBase implements View.OnTouchListener, RadialView.OnSliceClickListener {

    private static final int SAMPLE_RATE = 44100;
    private byte[] mViolinCBuf;
    private int mLastViolinBufIndex = 0; // Index of the last
    private AudioTrack mStreamingTrack = null;
    private AudioWriteRunnable mAudioWriteRunnable;
    private AudioStopRunnable mAudioStopRunnable;

    private float[] mLinearAccel = new float[3];
    private float[] mGravity = new float[3];

    private ArrayList<ImageButton> mViolinButtons;
    private ArrayList<View> mViolinGlows;
    private RadialView mRadialView;

    private ArrayList<Float> mAccelHistory = new ArrayList<>();
    private ArrayList<byte[]> mSoundList = new ArrayList<>();
    private Thread mAudioWriteThread, mAudioStopThread;


    private boolean mIsPlayingViolin;

    protected final float LINEAR_ACCEL_THRESHOLD = 0.15f;

    private int[] mViolinButtonIds = {
            R.id.violin_c,
            R.id.violin_d,
            R.id.violin_e,
            R.id.violin_f,
            R.id.violin_g,
            R.id.violin_a,
            R.id.violin_b
    };

    private int[] mViolinGlowIds = {
            R.id.onepie_c_glow,
            R.id.onepie_d_glow,
            R.id.onepie_e_glow,
            R.id.onepie_f_glow,
            R.id.onepie_g_glow,
            R.id.onepie_a_glow,
            R.id.onepie_b_glow
    };

    private int[] mViolinSoundIds = {
            R.raw.c_violin_loop,
            R.raw.d_violin_loop,
            R.raw.e_violin_loop,
            R.raw.f_violin_loop,
            R.raw.g_violin_loop,
            R.raw.a_violin_loop,
            R.raw.b_violin_loop
    };

    private int mActiveViolinNote = 0;

    public ViolinController(Context context, Activity parentActivity) {
        mViolinButtons = new ArrayList<>();
        mViolinGlows = new ArrayList<>();

        for (int i = 0; i < mViolinButtonIds.length; i++) {
            mViolinButtons.add((ImageButton) parentActivity.findViewById(mViolinButtonIds[i]));
        }

        for (int i = 0; i < mViolinGlowIds.length; i++) {
            mViolinGlows.add(parentActivity.findViewById(mViolinGlowIds[i]));
        }

        mRadialView = (RadialView) parentActivity.findViewById(R.id.radial_view);
        mRadialView.setOnSliceClickListener(this);
		for (int j = 0; j < mViolinSoundIds.length; j++) {
            InputStream is = context.getResources().openRawResource(mViolinSoundIds[j]);
            try {
                int fileLength = is.available();
                DataInputStream dis = new DataInputStream(context.getResources().openRawResource(mViolinSoundIds[j]));
                byte[] tempViolinCBuf = new byte[fileLength];
                dis.readFully(tempViolinCBuf);
                byte[] finalNoteBuffer = new byte[fileLength - 44];
                finalNoteBuffer = Arrays.copyOfRange(tempViolinCBuf, 44, fileLength);

                mSoundList.add(finalNoteBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mStreamingTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize, AudioTrack.MODE_STREAM);

            mAudioWriteRunnable = new AudioWriteRunnable(mSoundList, mStreamingTrack);
            mAudioWriteThread = new Thread(mAudioWriteRunnable);

            mAudioStopRunnable = new AudioStopRunnable(mStreamingTrack);
            mAudioStopThread = new Thread(mAudioStopRunnable);

            mStreamingTrack.play();
            mAudioWriteThread.start();
            mAudioStopThread.start();
        }

    }

    private float getAverageLinearY() {
        float sum = 0;
        for (int i = 0; i < mAccelHistory.size(); i++) {
            sum += (Math.max(0.1, (float) i / (mAccelHistory.size() - 1))) * Math.abs(mAccelHistory.get(i));
        }
        return sum / mAccelHistory.size();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;

            mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];

            mLinearAccel[0] = event.values[0] - mGravity[0];
            mLinearAccel[1] = event.values[1] - mGravity[1];
            mLinearAccel[2] = event.values[2] - mGravity[2];
        }

        long deltaTime = updateDeltaTime();
        if (deltaTime < UPDATE_INTERVAL) {
            return;
        }
        mPreviousUpdateTimestamp = System.currentTimeMillis();

        update(deltaTime, mLinearAccel, event.values);
    }

    private void updateViolinView() {
        for (int i = 0; i < mViolinButtons.size(); i++) {
            if (i != mActiveViolinNote) {
                mViolinGlows.get(i).setVisibility(View.INVISIBLE);
            } else {
                if (mIsEnabled) {
                    mViolinGlows.get(i).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void update(long deltaTimeMillis, float[] linAccelerometer, float[] accelerometer) {
        updateLinearAccelSound(deltaTimeMillis, linAccelerometer, accelerometer);
        updateViolinView();
    }

    private long mStartTime = 0;
    private static long STOP_TIME_THRESHOLD = 200;
    private void updateLinearAccelSound(long deltaTimeMillis, float[] currentOrientation, float[] currentAccel) {
        float pitchR = (float) (currentAccel[0] + 3)/14.0f;
        float gain = Math.max(0, Math.min(1.2f, 1 - (pitchR)));
        mStreamingTrack.setVolume(gain);

        if (mIsEnabled) {
            mAccelHistory.add(mLinearAccel[1]);
        } else if (mAccelHistory.size() > 0) {
            mAccelHistory.clear();
        }

        if (mAccelHistory.size() < 8) {
            return;
        }
        if (getAverageLinearY() >= LINEAR_ACCEL_THRESHOLD ) {
            // Moving
            if (!mIsPlayingViolin) {
                mStartTime = System.currentTimeMillis();
            }
            mIsPlayingViolin = true;
            mAudioWriteRunnable.setEnabled(mIsPlayingViolin);
            mAudioStopRunnable.setStop(false);
            mAudioStopRunnable.setJustStop(false);
        } else {
            if (mIsPlayingViolin && System.currentTimeMillis()-mStartTime >= STOP_TIME_THRESHOLD) {
                mAudioStopRunnable.setJustStop(true);
                mAudioStopRunnable.setStop(true);
                mIsPlayingViolin = false;
                mAudioWriteRunnable.setEnabled(mIsPlayingViolin);
            } else if (mIsPlayingViolin && System.currentTimeMillis()-mStartTime < STOP_TIME_THRESHOLD) {
            }
        }
        mAccelHistory = new ArrayList<Float>(mAccelHistory.subList(mAccelHistory.size() - 3, mAccelHistory.size()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }

    @Override
    public void onSlickClickDown(int slicePosition) {
        mActiveViolinNote = (slicePosition + 4) % 7;
        Log.d("SP", "Playing note down: "+mActiveViolinNote);
        mAudioWriteRunnable.setPlayingNote(mActiveViolinNote);

    }

    @Override
    public void onSlickClickMove(int slicePosition) {
        int newNote = (slicePosition + 4) % 7;
        if (newNote != mActiveViolinNote) {
            mActiveViolinNote = newNote;
            mAudioStopRunnable.setStop(true);
            mAudioWriteRunnable.setPlayingNote(mActiveViolinNote);
        } else {
            return;
        }
    }

    @Override
    public void onSlickClickUp(int slicePosition) {
        mActiveViolinNote = -1;
        mAudioWriteRunnable.setPlayingNote(mActiveViolinNote);
        mAudioStopRunnable.setStop(true);
        mAudioStopRunnable.setJustStop(true);
    }
}