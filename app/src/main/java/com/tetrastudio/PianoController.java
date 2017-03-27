package com.tetrastudio;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
		R.id.c_sharp,
		R.id.d,
		R.id.d_sharp,
		R.id.e,
		R.id.f,
		R.id.f_sharp,
		R.id.g,
		R.id.g_sharp,
		R.id.a,
		R.id.a_sharp,
		R.id.b
	};
	private ArrayList<Button> mPianoButtons;

	private int[] mPianoSoundIds = {
		R.raw.c_piano,
		R.raw.c_sharp_piano,
		R.raw.d_piano,
		R.raw.d_sharp_piano,
		R.raw.e_piano,
		R.raw.f_piano,
		R.raw.f_sharp_piano,
		R.raw.g_piano,
		R.raw.g_sharp_piano,
		R.raw.a_piano,
		R.raw.a_sharp_piano,
		R.raw.b_piano
	};
	private int[] mLoadedPianoSoundIds;

	// Public constructor
	public PianoController(Context context, Activity parentActivity) {
		mContext = context;
		mParentActivity = parentActivity;

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
					mSoundPool.play(mLoadedPianoSoundIds[idx], 1, 1, 1, 0, 1);
				}
			});
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}
}