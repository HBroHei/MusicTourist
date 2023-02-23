package com.hbrohei.musictour;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class EndServiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent stopService = new Intent(getApplicationContext(),ScrCapForeground.class);
        stopService.putExtra("flag",1);
        startService(stopService);
        finishAndRemoveTask();
        Log.d("PROCESS_APP","I should not appear.");/**/
    }
}