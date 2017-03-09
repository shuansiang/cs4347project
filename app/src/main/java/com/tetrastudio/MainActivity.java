package com.tetrastudio;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.cs4347project.cs4347project.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



    }


    public void testt(View v) {
        Log.d("TSTUDIO","HI");
        startActivity(new Intent(MainActivity.this, PianoActivity.class));

    }
}
