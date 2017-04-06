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
import android.widget.Button;
import android.widget.ImageButton;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
    private Thread mAudioWriteThread, mAudioStopThread;

    private int[] mViolinButtonIds = {
            R.id.violin_c,
            R.id.violin_d,
            R.id.violin_e,
            R.id.violin_f,
            R.id.violin_g,
            R.id.violin_a,
            R.id.violin_b
    };

    private int[] mViolinSoundIds = {
            R.raw.c_violin_loop
    };

    private int[] mLoadedViolinIds;
    private float mOctaveOffset = 0;

    public ViolinController(Context context, Activity parentActivity) {
        mViolinButtons = new ArrayList<>();

        for (int i = 0; i < mViolinButtonIds.length; i++) {
            mViolinButtons.add((ImageButton) parentActivity.findViewById(mViolinButtonIds[i]));
            mViolinButtons.get(i).setOnTouchListener(this);
        }

        InputStream is = context.getResources().openRawResource(R.raw.c_violin_loop);
        try {
            int fileLength = is.available();
            DataInputStream dis = new DataInputStream(context.getResources().openRawResource(R.raw.c_violin_loop));
            mViolinCBuf = new byte[fileLength];
            Log.d("SP", "VCB: " + fileLength);
            dis.readFully(mViolinCBuf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mStreamingTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM);

        Log.d("SP", "MIN BUF SIZE: " + minBufferSize);

        mAudioWriteRunnable = new AudioWriteRunnable(mViolinCBuf, mStreamingTrack);
        mAudioWriteThread = new Thread(mAudioWriteRunnable);

        mAudioStopRunnable = new AudioStopRunnable(mStreamingTrack);
        mAudioStopThread = new Thread(mAudioStopRunnable);

        mStreamingTrack.play();
        mAudioWriteThread.start();
        mAudioStopThread.start();

//        SoundPool soundPoll = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);

//        //public Context context;
//        HashMap<Integer, Integer> soundPoolMap = new HashMap<Integer, Integer>();
//
//        final ViolinSoundPool violinSoundPool = new ViolinSoundPool(this, soundPoolMap, soundPoll);
//
//        violinSoundPool.SoundPoolLoadtest();
//
//        imageButton_m1.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        violinSoundPool.play(violinSoundPool.soundPoolMap.get(1), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.pause(violinSoundPool.soundPoolMap.get(1));
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//
//        imageButton_m2.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                int i = 0;
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        i = violinSoundPool.play(violinSoundPool.soundPoolMap.get(2), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(i);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//        imageButton_m3.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                int i = 0;
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        i = violinSoundPool.play(violinSoundPool.soundPoolMap.get(3), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(i);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//        imageButton_m4.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        violinSoundPool.play(violinSoundPool.soundPoolMap.get(4), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(violinSoundPool.soundPoolMap.get(4));
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//        imageButton_m5.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        violinSoundPool.play(violinSoundPool.soundPoolMap.get(5), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(violinSoundPool.soundPoolMap.get(5));
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//        imageButton_m6.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        violinSoundPool.play(violinSoundPool.soundPoolMap.get(6), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(violinSoundPool.soundPoolMap.get(6));
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//        imageButton_m7.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        violinSoundPool.play(violinSoundPool.soundPoolMap.get(7), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(violinSoundPool.soundPoolMap.get(7));
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//
//        imageButton_m8.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    try {
//                        violinSoundPool.play(violinSoundPool.soundPoolMap.get(8), 1, 1, 0, 0, 1);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    try {
//                        violinSoundPool.stop(violinSoundPool.soundPoolMap.get(8));
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            mLinearAccel = lowPass(event.values.clone(), mLinearAccel);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean down = false;
        Log.d("SP", v.getId()+" ontouch");
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            down = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            mAudioStopRunnable.checkStop(true);
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