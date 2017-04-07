package com.tetrastudio;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class PianoActivity extends AppCompatActivity {

    // Controllers
    private PianoController mPianoController;
    private SensorManager mSensorManager;
    private Sensor mAccelSensor;

	// Views

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piano);

		initViews();
        initSensors();
        initControllers();
    }

	private void initViews() {
	}

    private void initSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initControllers() {
        mPianoController = new PianoController(this, this);
    }

    private void resumeControllerSensors() {
        mSensorManager.registerListener(mPianoController, mAccelSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void pauseControllerSensors() {
        mSensorManager.unregisterListener(mPianoController);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeControllerSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseControllerSensors();
    }

    public void toVSDPage(View v) {
        startActivity(new Intent(PianoActivity.this, VSDActivity.class));
    }

}
