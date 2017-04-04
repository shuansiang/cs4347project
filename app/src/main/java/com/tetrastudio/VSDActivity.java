package com.tetrastudio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import android.media.MediaPlayer;
import android.widget.Toast;

public class VSDActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 123;

    // Views
    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView mDebugGrav;
    private View mDrumLayout;

    // Controllers
    private ArrayList<Pair<ControllerBase, Sensor>> mControllers;

    private DrumController mDrumController;
    private SensorManager mSensorManager;
    private Sensor mAccelSensor;
    //Shaker
    private ShakeListener mShaker;
    private Toast mToast;

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
        mDrumLayout = (View) findViewById(R.id.drumLayout);
        mDrumLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.d("SP", "ACTION UP");
                    mDrumController.setEnabled(false);
                    mShaker.setEnabled(true);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d("SP", "ACTION DOWN");
                    mDrumController.setEnabled(true);
                    mShaker.setEnabled(false);
                }
                return false;
            }
        });
    }

    private void initSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initControllers() {
        mControllers = new ArrayList<>();
        // Controllers for each type of instrument can be added here
        mDrumController = new DrumController(this, this, mOpenCvCameraView);
        mDrumController.setEnabled(false);

        mShaker = new ShakeListener(this, this);
        mShaker.setEnabled(true);
//        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
//            public void onShake(int id) {
//                playSound(id);
//                if (mToast != null) {
//                    mToast.cancel();
//                }
//                if (id > 0) {
//                    mToast = Toast.makeText(VSDActivity.this, "Hard Shake", Toast.LENGTH_SHORT);
//                    mToast.show();
//                } else {
//                    mToast = Toast.makeText(VSDActivity.this, "Soft Shake", Toast.LENGTH_SHORT);
//                    mToast.show();
//                }
//            }
//        });
//        shakerControl();

        mControllers.add(new Pair<ControllerBase, Sensor>(mDrumController, mAccelSensor));
        mControllers.add(new Pair<ControllerBase, Sensor>(mShaker, mAccelSensor));
    }

    private void resumeControllerSensors() {
        for (Pair<ControllerBase, Sensor> controllerEntry : mControllers) {
            if (controllerEntry.second != null) {
                mSensorManager.registerListener(controllerEntry.first, controllerEntry.second, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    private void pauseControllerSensors() {
        HashSet<ControllerBase> encounteredControllers = new HashSet<>();
        for (Pair<ControllerBase, Sensor> controllerEntry : mControllers) {
            if (encounteredControllers.contains(controllerEntry.first)) {
                continue;
            }
            if (controllerEntry.second == null) {
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
//        mShaker.pause();
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

//    private void shakerControl() {
//        mShaker = new ShakeListener(this);
//        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
//            public void onShake(int id) {
//                playSound(id);
//                if (mToast != null) {
//                    mToast.cancel();
//                }
//                if (id > 0) {
//                    mToast = Toast.makeText(VSDActivity.this, "Hard Shake", Toast.LENGTH_SHORT);
//                    mToast.show();
//                } else {
//                    mToast = Toast.makeText(VSDActivity.this, "Soft Shake", Toast.LENGTH_SHORT);
//                    mToast.show();
//                }
//            }
//        });
//    }

    public void toPianoPage(View v) {
        startActivity(new Intent(VSDActivity.this, PianoActivity.class));
    }
}
