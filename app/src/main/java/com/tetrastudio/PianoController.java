package com.tetrastudio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by elementoss on 23/3/17.
 */

public class PianoController extends ControllerBase {

	private Context mContext;
	private Activity mParentActivity;

	private SoundPool mSoundPool = null;

	private int[] mPianoButtonIds = {
		R.id.c,
		R.id.d,
		R.id.e,
		R.id.f,
		R.id.g,
		R.id.a,
		R.id.b,
		R.id.c_sharp,
		R.id.d_sharp,
		R.id.f_sharp,
		R.id.g_sharp,
		R.id.a_sharp
	};
	private ArrayList<Button> mPianoButtons;

	private int[] mPianoSoundIds = {
		R.raw.c_piano,
		R.raw.d_piano,
		R.raw.e_piano,
		R.raw.f_piano,
		R.raw.g_piano,
		R.raw.a_piano,
		R.raw.b_piano,
		R.raw.c_sharp_piano,
		R.raw.d_sharp_piano,
		R.raw.f_sharp_piano,
		R.raw.g_sharp_piano,
		R.raw.a_sharp_piano
	};
	private int[] mLoadedPianoSoundIds;

	private float mOctaveOffset = 0;

	private float[] mAccelerometerVal = new float[4];

	// Test views
	private TextView mDebugTextView;

	// Public constructor
	public PianoController(Context context, Activity parentActivity) {
		mContext = context;
		mParentActivity = parentActivity;
		mDebugTextView = ((PianoActivity) mParentActivity).getDebugGrav();

		loadPianoSounds();
		loadPianoButtons();
		setPianoButtons();
	}

	// Load all piano sounds
	protected void loadPianoSounds() {
		mSoundPool = new SoundPool(12, AudioManager.STREAM_MUSIC, 0);
		mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				Log.d("PianoController", "Sound " + sampleId + " loaded");
			}
		});

		mLoadedPianoSoundIds = new int[mPianoSoundIds.length];
		for (int i=0; i<mPianoSoundIds.length; i++) {
			mLoadedPianoSoundIds[i] = mSoundPool.load(mContext, mPianoSoundIds[i], 1);
		}
	}

	// Find all piano buttons from parent activity
	protected void loadPianoButtons() {
		mPianoButtons = new ArrayList<>();
		for (int i=0; i<mPianoButtonIds.length; i++) {
			mPianoButtons.add((Button) mParentActivity.findViewById(mPianoButtonIds[i]));
		}
	}

	// Set onclick listener to all piano buttons
	protected void setPianoButtons() {
		for (int i=0; i<mPianoButtons.size(); i++) {
			final int idx = i;
			mPianoButtons.get(i).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mSoundPool.play(mLoadedPianoSoundIds[idx], 1, 1, 1, 0, mOctaveOffset);
				}
			});
		}
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mAccelerometerVal = MathUtils.lowPass(sensorEvent.values.clone(), mAccelerometerVal);
			mDebugTextView.setText(String.format("X:%f\nY:%f\nZ:%f", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
		}

		update(mAccelerometerVal);
	}

	// Update piano properties
	public void update(float[] accelerometer) {
		if (accelerometer[2] >= 9.0) {
			mOctaveOffset = 3;
			updateKeyboardColours(R.color.pink2);
		} else if (accelerometer[2] > 6.0 && accelerometer[2] < 9.0) {
			mOctaveOffset = 2;
			updateKeyboardColours(R.color.pink1);
		} else if (accelerometer[2] < -1.0 && accelerometer[2] > -4.5) {
			mOctaveOffset = 0.5f;
			updateKeyboardColours(R.color.pink1);
		} else if (accelerometer[2] <= -4.5) {
			mOctaveOffset = 0.25f;
			updateKeyboardColours(R.color.pink2);
		} else {
			mOctaveOffset = 1;
			updateKeyboardColours(R.color.white);
		}
	}

	// Change colour of keyboard according to octaves
	public void updateKeyboardColours(int color) {
		for (int i=0; i<mPianoButtons.size()-5; i++) {
			mPianoButtons.get(i).setBackgroundResource(color);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}
}
