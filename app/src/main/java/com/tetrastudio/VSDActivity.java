package com.tetrastudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.cs4347project.cs4347project.R;

import org.opencv.android.CameraBridgeViewBase;

public class VSDActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 123;

    // Views
    private CameraBridgeViewBase mOpenCvCameraView;

    // Controllers
    private DrumController mDrumController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vsd);

        initViews();
        initControllers();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }

    private void initViews() {
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(-1);
    }

    private void initControllers() {
        mDrumController = new DrumController(this, this, mOpenCvCameraView);
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
}
