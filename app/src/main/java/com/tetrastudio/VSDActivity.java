package com.tetrastudio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class VSDActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 123;

    // Views
    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView mDebugGrav;

    // Controllers
    private ArrayList<Pair<ControllerBase, Sensor>> mControllers;

    private DrumController mDrumController;
    private SensorManager mSensorManager;
    private Sensor mAccelSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vsd);

        initViews();
        initSensors();
        initControllers();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }

    private void initViews() {
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mDebugGrav = (TextView) findViewById(R.id.debugGrav);
    }

    private void initSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initControllers() {
        mControllers = new ArrayList<>();
        // Controllers for each type of instrument can be added here
        mDrumController = new DrumController(this, this, mOpenCvCameraView);
        mDrumController.setEnabled(true);
        mControllers.add(new Pair<ControllerBase, Sensor>(mDrumController, mAccelSensor));
    }

    private void resumeControllerSensors() {
        for (Pair<ControllerBase, Sensor> controllerEntry : mControllers) {
            mSensorManager.registerListener(controllerEntry.first, controllerEntry.second, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void pauseControllerSensors() {
        HashSet<ControllerBase> encounteredControllers = new HashSet<>();
        for (Pair<ControllerBase, Sensor> controllerEntry : mControllers) {
            if (encounteredControllers.contains(controllerEntry.first)) {
                continue;
            }
            encounteredControllers.add(controllerEntry.first);
            mSensorManager.unregisterListener(controllerEntry.first);
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!.
                    mDrumController.enableCamera();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    public TextView getDebugGrav() {
        return mDebugGrav;
    }
}
