package com.tetrastudio;

import android.media.AudioTrack;
import android.util.Log;

import java.util.ArrayList;

class AudioWriteRunnable implements Runnable {
    private static final int SAMPLE_RATE = 44100;
    private ArrayList<byte[]> bufferList = new ArrayList<>();
    private int mCurrentSoundId;
    private boolean mViolinEnabled = false;
    private boolean mIsPlayingViolin = false;
    private long mLastCallTime = -1;
    private AudioTrack mStreamingTrack = null;

    public AudioWriteRunnable(ArrayList<byte[]> bufferList, AudioTrack mStreamingTrack) {
        this.bufferList = bufferList;
        this.mStreamingTrack = mStreamingTrack;
    }

    private byte[] getSoundBuffer(float frequency, float duration, FadeType fadeType) {
        byte[] buffer = new byte[(int) (duration * SAMPLE_RATE)];
        float[] fadeBuffer = new float[(int) (duration * SAMPLE_RATE)];
        if (fadeType != FadeType.NONE) {
            for (int i = 0; i < fadeBuffer.length; i++) {
                float delta = ((float) i) / (fadeBuffer.length - 1);
                fadeBuffer[i] = (fadeType == FadeType.FADE_IN ? delta : 1 - delta);
            }
        }

        for (int i = 0; i < buffer.length; i++) {
            double amplitude = Math.sin((2.0 * Math.PI * frequency / SAMPLE_RATE * (double) i));
            buffer[i] = (byte) (amplitude * Byte.MAX_VALUE);
            if (fadeType != FadeType.NONE) {
                buffer[i] *= fadeBuffer[i];
            }
        }
        return buffer;

    }

    public boolean setEnabled(boolean isEnabled) {
        mViolinEnabled = isEnabled;

        return mViolinEnabled;
    }

    public int setPlayingNote(int idx) {
        mCurrentSoundId = idx;
        return mCurrentSoundId;
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        boolean mUseAudioTrack = true;
        while (mUseAudioTrack) {
            long currentTime = System.currentTimeMillis();

            if (mViolinEnabled && mCurrentSoundId >= 0) {
                if (mLastCallTime < 0) {
                    mLastCallTime = System.currentTimeMillis();
                }
//                    float duration = (currentTime - mLastCallTime) * 0.001f;
                float mFrequency = 440.0f;
                float period = 1.0f / mFrequency;
                float duration = (float) (period * Math.ceil(0.05f / period));
//                    byte[] buffer = getViolinBuffer(261.6f, -1, FadeType.NONE, 0);
                int mLastViolinBufIndex = 0;
                Log.d("SP", "D: " + duration + " / MLV: " + mLastViolinBufIndex);
//                    if (duration * SAMPLE_RATE <= 0) {
//                        duration = 1.0f / SAMPLE_RATE;
//                    }
                byte[] buffer = this.bufferList.get(mCurrentSoundId); //getSoundBuffer(mFrequency, duration, FadeType.NONE);

//                    byte[] buffer = getViolinBuffer(mFrequency, duration, mLastViolinBufIndex, FadeType.NONE);
                Log.d("SP", "Freq: " + mFrequency);
                mStreamingTrack.write(buffer, 0, buffer.length);
            } else {
                // Stopping is handled in the AudioStopRunnable since write will block this thread
            }
            mLastCallTime = currentTime;
        }
    }

    private enum FadeType {
        NONE,
        FADE_IN,
        FADE_OUT
    }
}

class AudioStopRunnable implements Runnable {
    private boolean mViolinJustStopped = false;
    private boolean mIsNotPlayingViolin = false;
    private AudioTrack mStreamingTrack = null;

    public AudioStopRunnable(AudioTrack mStreamingTrack) {
        this.mStreamingTrack = mStreamingTrack;
    }

    public boolean setStop(boolean isStop) {
        mIsNotPlayingViolin = isStop;

        return mIsNotPlayingViolin;
    }

    public boolean setJustStop(boolean isStop) {
        mViolinJustStopped = isStop;

        return mViolinJustStopped;
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        boolean mUseAudioTrack = true;
        while (mUseAudioTrack) {
//            mIsNotPlayingViolin = false;
            if (mIsNotPlayingViolin) {
                int playPos = mStreamingTrack.getPlaybackHeadPosition();
                Log.d("SP", "MVJS: Just stopped " + playPos);
                mStreamingTrack.pause();
                Log.d("SP", "MVJS: Flush " + playPos);
                mStreamingTrack.flush();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mStreamingTrack.play();
//                        float period = 1.0f / mFrequency;
//                        float duration = (float) (period * Math.ceil(0.25f / period));
//                        byte[] buffer = getViolinBuffer(0, 0.5f, FadeType.FADE_OUT, playPos);
//                        byte[] buffer = getSoundBuffer(0, 0.5f, FadeType.FADE_OUT);
//                        mStreamingTrack.write(buffer, 0, buffer.length);
                mViolinJustStopped = false;
                mIsNotPlayingViolin = false;
            }
        }
    }
}