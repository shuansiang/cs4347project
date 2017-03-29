package com.tetrastudio;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.media.MediaPlayer;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ShakeListener mShaker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //ShakerController
        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener () {
            public void onShake()
            {
                playSound();
                Toast.makeText(MainActivity.this, "Shake " , Toast.LENGTH_LONG).show();

            }
        });
    }

    //ShakerController
    @Override
    protected void onResume() {
        super.onResume();
        mShaker.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mShaker.pause();
        //   super.onPause();

    }

    protected void playSound(){
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.hard);
        mediaPlayer.start();
    }



    public void start(View v) {
        Log.d("TSTUDIO","HI");
        startActivity(new Intent(MainActivity.this, PianoActivity.class));

    }
}
