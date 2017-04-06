package com.tetrastudio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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

public class ViolinController extends ControllerBase implements View.OnTouchListener {

    private static final int SAMPLE_RATE = 44100;
    private byte[] mViolinCBuf;
    private int mLastViolinBufIndex = 0; // Index of the last
    private AudioTrack mStreamingTrack = null;
    private AudioWriteRunnable mAudioWriteRunnable;
    private AudioStopRunnable mAudioStopRunnable;
    private float[] mLinearAccel = new float[3];
    private ArrayList<ImageButton> mViolinButtons;
    private ArrayList<View> mViolinGlows;
    private ArrayList<Float> mAccelHistory = new ArrayList<>();
    private ArrayList<byte[]> mSoundList = new ArrayList<>();
    private Thread mAudioWriteThread, mAudioStopThread;

    private boolean mIsPlayingViolin, mViolinJustStarted, mViolinJustStopped;

    protected final float LINEAR_ACCEL_THRESHOLD = 0.25f;

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

    private int[] getmLoadedViolinIds;
    private int[] mLoadedViolinSoundIds;
    private float mOctaveOffset = 0;
    private int mActiveViolinNote = 0;

    public ViolinController(Context context, Activity parentActivity) {
        mViolinButtons = new ArrayList<>();
        mViolinGlows = new ArrayList<>();

        for (int i = 0; i < mViolinButtonIds.length; i++) {
            mViolinButtons.add((ImageButton) parentActivity.findViewById(mViolinButtonIds[i]));
            mViolinButtons.get(i).setOnTouchListener(this);
        }

        for (int i = 0; i < mViolinGlowIds.length; i++) {
            mViolinGlows.add(parentActivity.findViewById(mViolinGlowIds[i]));
        }

        mLoadedViolinSoundIds = new int[mViolinSoundIds.length];
        for (int j = 0; j < mViolinSoundIds.length; j++) {
            InputStream is = context.getResources().openRawResource(mViolinSoundIds[j]);

            try {
                int fileLength = is.available();
                DataInputStream dis = new DataInputStream(context.getResources().openRawResource(mViolinSoundIds[j]));
                byte[] tempViolinCBuf = new byte[fileLength];
                Log.d("SP", "VCB: " + fileLength);
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

            Log.d("SP", "MIN BUF SIZE: " + minBufferSize);

            mAudioWriteRunnable = new AudioWriteRunnable(mSoundList, mStreamingTrack);
            mAudioWriteThread = new Thread(mAudioWriteRunnable);

            mAudioStopRunnable = new AudioStopRunnable(mStreamingTrack);
            mAudioStopThread = new Thread(mAudioStopRunnable);

            mStreamingTrack.play();
            mAudioWriteThread.start();
            mAudioStopThread.start();
        }

//        InputStream is = context.getResources().openRawResource(R.raw.c_violin_loop);
//        try {
//            int fileLength = is.available();
//            DataInputStream dis = new DataInputStream(context.getResources().openRawResource(R.raw.c_violin_loop));
//            byte[] tempViolinCBuf = new byte[fileLength];
//            Log.d("SP", "VCB: " + fileLength);
//            dis.readFully(tempViolinCBuf);
//            mViolinCBuf = new byte[fileLength-44];
//            mViolinCBuf = Arrays.copyOfRange(tempViolinCBuf, 44, fileLength);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
//        mStreamingTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
//                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
//                minBufferSize, AudioTrack.MODE_STREAM);
//
//        Log.d("SP", "MIN BUF SIZE: " + minBufferSize);
//
//        mAudioWriteRunnable = new AudioWriteRunnable(mViolinCBuf, mStreamingTrack);
//        mAudioWriteThread = new Thread(mAudioWriteRunnable);
//
//        mAudioStopRunnable = new AudioStopRunnable(mStreamingTrack);
//        mAudioStopThread = new Thread(mAudioStopRunnable);
//
//        mStreamingTrack.play();
//        mAudioWriteThread.start();
//        mAudioStopThread.start();
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
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            mLinearAccel = lowPass(event.values.clone(), mLinearAccel);
        }

        long deltaTime = updateDeltaTime();
        if (deltaTime < UPDATE_INTERVAL) {
            return;
        }
        mPreviousUpdateTimestamp = System.currentTimeMillis();

        update(deltaTime, mLinearAccel);
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

    private void update(long deltaTimeMillis, float[] accelerometer) {
//        checkFlick(deltaTimeMillis, previousOrientation, currentOrientation);
        updateLinearAccelSound(deltaTimeMillis, accelerometer);
        updateViolinView();
    }

    private void updateLinearAccelSound(long deltaTimeMillis, float[] currentOrientation) {
//        float pitchR = (float) Math.toDegrees(currentOrientation[1]); // Rotational pitch (not the frequency pitch)
//        float gain = 1 - ((pitchR + 50) / 140.0f) * 1;
//        mStreamingTrack.setPlaybackRate((int)(gain * 44100));
        mStreamingTrack.setVolume(1.0f);

        if (mIsEnabled) {
            mAccelHistory.add(mLinearAccel[1]);
        } else if (mAccelHistory.size() > 0) {
            mAccelHistory.clear();
        }

        if (mAccelHistory.size() < 8) {
            return;
        }
        if (getAverageLinearY() >= LINEAR_ACCEL_THRESHOLD) {
            mIsPlayingViolin = true;
            mViolinJustStarted = true;
        } else {
            if (mIsPlayingViolin) {
                mViolinJustStopped = true;
            }
            mIsPlayingViolin = false;
        }
        mAccelHistory = new ArrayList<Float>(mAccelHistory.subList(mAccelHistory.size() - 3, mAccelHistory.size()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean down = false;
        Log.d("SP", v.getId() + " ontouch");

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            down = true;
            for (int i = 0; i < mViolinButtons.size(); i++) {

            }
//            mAudioWriteRunnable.changedIdx()
        }

        for (int i = 0; i < mViolinButtonIds.length; i++) {
            if (v == mViolinButtons.get(i)) {
                mAudioWriteRunnable.checkEnabled(down);
                mAudioStopRunnable.checkStop(!down);
                mAudioStopRunnable.setJustStop(!down);
                return down;
            }
        }

        return down;
    }
}