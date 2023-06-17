package com.hbrohei.musictour;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainListActivity extends AppCompatActivity {

    private ScreenCap scrCap;

    private ToggleButton scrCapToggle;
    private Button btnStart;
    private EditText nicknameET;
    private ImageButton addSongBtn;
    private RecyclerView musicRList;

    // song dialog views
    private LinearLayout base;
    private EditText courseNameET;
    private LinearLayout lapContainer;
    private EditText currentLapET;
    private EditText maxLapET;
    private CheckBox isBattleBox;
    private EditText loopTime_min;
    private EditText loopTime_sec;
    private EditText loopTime_mse;
    private EditText loopTime_min2;
    private EditText loopTime_sec2;
    private EditText loopTime_mse2;
    private ImageButton btn_play_dialog;
    private Button setStartPtBtn;
    private Button setLoopPtBtn;
    private SeekBar dia_seekbar;

    private ActivityResultLauncher<String> openFileAR;
    private ActivityResultLauncher<Intent> capScreenRequest;

    private MediaPlayer previewMP;

    private String[] musicFilesList;

    private final String [] permissions = {Manifest.permission.RECORD_AUDIO};

    private boolean isPlaying;
    private boolean firstStartup;
    private boolean isRunning = false;
    private ActivityResultLauncher<String> recAudioRequest;
    private BroadcastReceiver stopRec_BC;
    private BroadcastReceiver editDia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainlist);



        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        saveSharedPreference("MTour_device_res","density", String.valueOf(dm.densityDpi));
        saveSharedPreference("MTour_device_res","width", String.valueOf(dm.widthPixels));
        saveSharedPreference("MTour_device_res","height", String.valueOf(dm.heightPixels));

        scrCap = new ScreenCap(this,ScrCapForeground.class,dm);

        btnStart = findViewById(R.id.btnStart);
        nicknameET = findViewById(R.id.nicknameET);
        SharedPreferences nnameSP = getSharedPreferences("MTour_nname",Context.MODE_PRIVATE);
        nicknameET.setText(nnameSP.getString("name",""));

        //Intent serviceIntent = ;
        //startService(new Intent(getApplicationContext(),ScrCapForeground.class));

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("RUNNING", String.valueOf(isRunning));
                if(nicknameET.getText().toString().equals("")){
                    Toast.makeText(MainListActivity.this, "Please enter your in-game username.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(musicFilesList.length==0){
                    Toast.makeText(MainListActivity.this, "Please add custom music first", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor nameSPE = nnameSP.edit();
                nameSPE.putString("name",nicknameET.getText().toString());
                nameSPE.apply();
                scrCap.prepare();
                Toast.makeText(MainListActivity.this, "Please accept the permission for it to function", Toast.LENGTH_SHORT).show();
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    //Prepare recording foreground
                    scrCap.recordPrepare();
                    scrCap.requestPermission(getApplicationContext(),capScreenRequest);
                    isRunning = true;


                } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    //Tell the user the permission
                } else {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    recAudioRequest.launch(Manifest.permission.RECORD_AUDIO);
                }
                //requestPermissions( permissions, 200);
                //scrCap.requestPermission(MainActivity.this)
            }
        });/**/
        /*
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("RUNNING", String.valueOf(isRunning));
                if(isRunning){
                    scrCap.stop();
                    isRunning = false;
                }
                else{
                    //Check for invalid inputs
                    if(nicknameET.getText().toString().equals("")){
                        Toast.makeText(MainListActivity.this, "Please enter your in-game username.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(musicFilesList.length==0){
                        Toast.makeText(MainListActivity.this, "Please add custom music first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //Store player name for future use
                    SharedPreferences.Editor nameSPE = nnameSP.edit();
                    nameSPE.putString("name",nicknameET.getText().toString());
                    nameSPE.apply();

                    //start service
                    capScreenRequest.launch();


                }

            }
        });*/

        addSongBtn = findViewById(R.id.addSongBtn);
        addSongBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });

        //Recycler View Setup
        musicRList = findViewById(R.id.musicRList);
        musicFilesList = new File(getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"));
        //Check for first time user
        if(musicFilesList==null){
            firstStartup = true;
            musicFilesList = new String[]{};
            //TODO implement a tutorial
        }
        else{
            SharedPreferences nameSP = getSharedPreferences("MTour_name", Context.MODE_PRIVATE);
            for(int i=0;i<musicFilesList.length;i++){
                musicFilesList[i] += "\n(Original:" + nameSP.getString(musicFilesList[i],"") + ")";
            }
        }

        musicRList.setAdapter(new MusicListView(musicFilesList));
        musicRList.setLayoutManager( new LinearLayoutManager(this));

        //Add Activity Result launcher as they must be created in onCreate()
        setActivityResultLauncher();

        //Add broadcastreceiver,thanks openai
        addBroadcastreceiver();

        //Set dialog view
        createDialogView();

    }

    private void setActivityResultLauncher(){
        openFileAR = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        try {
                            showSongDialog(getUriFileName(uri), uri);
                        }
                        catch (IOException ioe){
                            Toast.makeText(MainListActivity.this, "Error while opening the file!\n" + ioe.getCause() + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        capScreenRequest = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode()!=RESULT_OK) return;

                        scrCap.start(getApplicationContext(),0, result.getResultCode(), result.getData());

                        Intent startOCR = new Intent(getApplicationContext(),ScrCapForeground.class);
                        startOCR.putExtra("flag",2);
                        startOCR.putExtra("scrCap_width",scrCap.getWidth());
                        startOCR.putExtra("scrCap_height",scrCap.getHeight());
                        startOCR.putExtra("scrCap_surface",scrCap.getSurface());
                        startService(startOCR);
                        try {
                            startActivity(getPackageManager().getLaunchIntentForPackage("com.nintendo.zaka"));
                        }
                        catch(ActivityNotFoundException anfe){
                            Log.e("APP_LAUNCH", String.valueOf(anfe));
                        }

                        //finish();
                    }
                });

        recAudioRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        //Prepare recording foreground
                        scrCap.recordPrepare();
                        scrCap.requestPermission(getApplicationContext(),capScreenRequest);
                        isRunning = true;
                        saveRunningState();
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // feature requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        Toast.makeText(this, "Please accept the permission", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addBroadcastreceiver(){
        stopRec_BC = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                scrCap.stop();

                Intent stopService = new Intent(getApplicationContext(),ScrCapForeground.class);
                stopService.putExtra("flag",1);
                startService(stopService);
                finishAndRemoveTask();
            }
        };
        registerReceiver(stopRec_BC,new IntentFilter("stopRec"));

        editDia = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    showSongDialog(
                            intent.getStringExtra("sname"),
                            Uri.fromFile(new File(getExternalFilesDir(null) + "/Custom Music/" + intent.getStringExtra("sname")))
                    );
                } catch (IOException ioe) {
                    Toast.makeText(MainListActivity.this, "Error while opening the file!\n" + ioe.getCause() + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TEXTNAME", Arrays.toString(ioe.getStackTrace()));
                }
            }
        };
        registerReceiver(editDia,new IntentFilter("editDia"));
    }

    private void openFile() {
        Intent openFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        openFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        openFileIntent.setType("application/audio");

        openFileAR.launch("audio/*");
    }

    private void updateRecycleView(){
        musicFilesList = new File(getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"));
        SharedPreferences nameSP = getSharedPreferences("MTour_name", Context.MODE_PRIVATE);
        for(int i=0;i<musicFilesList.length;i++){
            musicFilesList[i] += "\n(Original:" + nameSP.getString(musicFilesList[i],"") + ")";
        }
        musicRList.setAdapter(new MusicListView(musicFilesList));
        musicRList.setLayoutManager( new LinearLayoutManager(this));
    }


    /**
     * Dev's note: collapse this pls
     */
    private void createDialogView(){
        base = new LinearLayout(this);
        base.setOrientation(LinearLayout.VERTICAL);

        TextView _tv1 = new TextView(this);
        _tv1.setText("\nCourse name to replace music:");
        base.addView(_tv1);
        courseNameET = new EditText(this);
        courseNameET.setSingleLine(true);
        courseNameET.setHint("Must be the same as the name in-game.");
        base.addView(courseNameET);

        base.addView(newLabel("Lap to be played in"));
        lapContainer = new LinearLayout(this);
        currentLapET = new EditText(this);
        currentLapET.setFilters(new InputFilter[] { new InputFilter.LengthFilter(1) });
        currentLapET.setText("1");
        lapContainer.addView(currentLapET);
        lapContainer.addView(newLabel("/"));
        maxLapET = new EditText(this);
        maxLapET.setFilters(new InputFilter[] { new InputFilter.LengthFilter(1) });
        maxLapET.setText("2");
        lapContainer.addView(maxLapET);
        isBattleBox = new CheckBox(this);
        isBattleBox.setText("No Lap Count (Battle Stage Music)");
        lapContainer.addView(isBattleBox);
        base.addView(lapContainer);

        isBattleBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                currentLapET.setText("");
                maxLapET.setText("");
                currentLapET.setEnabled(false);
                maxLapET.setEnabled(false);
            }
            else{
                currentLapET.setEnabled(true);
                maxLapET.setEnabled(true);
            }
        });

        base.addView(newLabel("Music start time"));
        LinearLayout timerLayout = new LinearLayout(this);
        loopTime_min = new EditText(this);
        loopTime_min.setHint("MM");
        loopTime_min.setInputType(InputType.TYPE_CLASS_DATETIME);
        loopTime_min.setFilters(new InputFilter[] { new InputFilter.LengthFilter(2) });
        timerLayout.addView(loopTime_min);
        TextView _colon1 = new TextView(this);
        _colon1.setText(":");
        timerLayout.addView(_colon1);
        loopTime_sec = new EditText(this);
        loopTime_sec.setHint("SS");
        timerLayout.addView(loopTime_sec);
        TextView _colon2 = new TextView(this)  ;
        _colon2.setText(":");
        timerLayout.addView(_colon2);
        loopTime_mse = new EditText(this);
        loopTime_mse.setHint("MIS");
        timerLayout.addView(loopTime_mse);

        base.addView(timerLayout);

        TextView _tv3 = new TextView(this);
        _tv3.setText("Music Loop time");
        base.addView(_tv3);

        LinearLayout timerLayout2 = new LinearLayout(this);
        loopTime_min2 = new EditText(this);
        loopTime_min2.setHint("MM");
        loopTime_min2.setInputType(InputType.TYPE_CLASS_DATETIME);
        loopTime_min2.setFilters(new InputFilter[] { new InputFilter.LengthFilter(2) });
        timerLayout2.addView(loopTime_min2);
        TextView _colon12 = new TextView(this);
        _colon12.setText(":");
        timerLayout2.addView(_colon12);
        loopTime_sec2 = new EditText(this);
        loopTime_sec2.setHint("SS");
        timerLayout2.addView(loopTime_sec2);
        TextView _colon22 = new TextView(this)  ;
        _colon22.setText(":");
        timerLayout2.addView(_colon22);
        loopTime_mse2 = new EditText(this);
        loopTime_mse2.setHint("MIS");
        timerLayout2.addView(loopTime_mse2);

        base.addView(timerLayout2);

        LinearLayout mediaControlPanel = new LinearLayout(this);
        btn_play_dialog = new ImageButton(this);
        btn_play_dialog.setImageResource(android.R.drawable.ic_media_play);
        btn_play_dialog.setOnClickListener(v -> {
            if(!isPlaying){
                previewMP.start();
                btn_play_dialog.setImageResource(android.R.drawable.ic_media_pause);
                dia_seekbar.setEnabled(false);
            }
            else{
                previewMP.pause();
                btn_play_dialog.setImageResource(android.R.drawable.ic_media_play);
                dia_seekbar.setEnabled(true);
            }
            isPlaying = !isPlaying;
        });
        mediaControlPanel.addView(btn_play_dialog);
        setStartPtBtn = new Button(this);
        setStartPtBtn.setText("Set Starting Point");
        setStartPtBtn.setOnClickListener(v -> {
            int min = previewMP.getCurrentPosition()/1000/60;
            int sec = (previewMP.getCurrentPosition()/1000)-(min*60);
            int milsec = (previewMP.getCurrentPosition())-(min*60*1000)-(sec*1000);
            loopTime_min.setText(String.valueOf(min));
            loopTime_sec.setText(String.valueOf(sec));
            loopTime_mse.setText(String.valueOf(milsec));
        });
        mediaControlPanel.addView(setStartPtBtn);

        setLoopPtBtn = new Button(this);
        setLoopPtBtn.setText("Set Looping Point");
        setLoopPtBtn.setOnClickListener(v -> {
            int min = previewMP.getCurrentPosition()/1000/60;
            int sec = (previewMP.getCurrentPosition()/1000)-(min*60);
            int milsec = (previewMP.getCurrentPosition())-(min*60*1000)-(sec*1000);
            loopTime_min2.setText(String.valueOf(min));
            loopTime_sec2.setText(String.valueOf(sec));
            loopTime_mse2.setText(String.valueOf(milsec));
        });
        mediaControlPanel.addView(setLoopPtBtn);
        base.addView(mediaControlPanel);

        dia_seekbar = new SeekBar(this);
        dia_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!previewMP.isPlaying() && fromUser){
                    previewMP.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        base.addView(dia_seekbar);

    }

    private void showSongDialog(String songName, Uri songPath) throws IOException {

        previewMP = new MediaPlayer();

        Log.d("ADD_MUSIC",songPath.getPath());


        previewMP.setDataSource(this,songPath);
        previewMP.prepare();
        //previewMP.start();

        Log.d("ADD_MUSIC", String.valueOf(previewMP.getDuration()));

        int min = previewMP.getDuration()/1000/60;
        int sec = (previewMP.getDuration()/1000)-(min*60);
        int milsec = (previewMP.getDuration())-(min*60*1000)-(sec*1000);

        Handler seekbarUpdateDia = new Handler();
        MainListActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(previewMP.isPlaying()){
                    dia_seekbar.setProgress(previewMP.getCurrentPosition());
                }
                seekbarUpdateDia.postDelayed(this, 1000);
            }
        });

        AlertDialog.Builder songDialog = new AlertDialog.Builder(MainListActivity.this);
        Log.d("TEXTNAME", Arrays.toString(musicFilesList) + "," + songName);
        String[] listFileName = new File(getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"));
        if(listFileName==null){}
        //If the dialog is called from the "edit" option in menu
        else if(Arrays.asList(listFileName).contains(songName)){
            songDialog.setTitle("Edit " + songName);

            //Get laps count via regex
            //GBA Riverside Park\(\d_\d\)\.mp3
            //Pattern p = ;
            Matcher m = Pattern.compile("\\(\\d_\\d\\)").matcher(songName);
            if(m.find()){
                //This is kinda hacky but it should works
                currentLapET.setText(String.valueOf(songName.charAt(songName.indexOf("(")+1)));
                maxLapET.setText(String.valueOf(songName.charAt(songName.indexOf(")")-1)));
            }
            else{
                isBattleBox.setChecked(true);
            }

            courseNameET.setText(songName.replaceAll(".mp3",""));

            //Get loops time
            SharedPreferences timeSP = getSharedPreferences("MTour_duration", Context.MODE_PRIVATE);
            String[] croppedTime = timeSP.getString(songName,"0,10000").split(",");
            int[] croppedTimeInt = {Integer.parseInt(croppedTime[0]), Integer.parseInt(croppedTime[1])};
            int[] loopTime_start = ScreenCap.returnMSM(croppedTimeInt[0]);
            int[] loopTime_end = ScreenCap.returnMSM(croppedTimeInt[1]);
            loopTime_min2.setText(String.valueOf(loopTime_end[0]));
            loopTime_sec2.setText(String.valueOf(loopTime_end[1]));
            loopTime_mse2.setText(String.valueOf(loopTime_end[2]));
            loopTime_min.setText(String.valueOf(loopTime_start[0]));
            loopTime_sec.setText(String.valueOf(loopTime_start[1]));
            loopTime_mse.setText(String.valueOf(loopTime_start[2]));
        }
        else{
            songDialog.setTitle("Add custom music");
            loopTime_min2.setText(String.valueOf(min));
            loopTime_sec2.setText(String.valueOf(sec));
            loopTime_mse2.setText(String.valueOf(milsec));
            loopTime_min.setText(String.valueOf(0));
            loopTime_sec.setText(String.valueOf(0));
            loopTime_mse.setText(String.valueOf(0));
        }
        dia_seekbar.setMax(previewMP.getDuration());
        songDialog.setView(base);
        //Cancel Action
        songDialog.setNegativeButton("Cancel", (dialog, which)-> {
            ((ViewGroup) base.getParent()).removeView(base);
        });
        // Add the song
        songDialog.setPositiveButton("OK", (dialog, which) -> {
            seekbarUpdateDia.removeCallbacksAndMessages(null);
            ((ViewGroup)base.getParent()).removeView(base);
            int startTime = timeToMil(
                    Integer.parseInt(loopTime_min.getText().toString()),
                    Integer.parseInt(loopTime_sec.getText().toString()),
                    Integer.parseInt(loopTime_mse.getText().toString())
            );
            int looptime = timeToMil(
                    Integer.parseInt(loopTime_min2.getText().toString()),
                    Integer.parseInt(loopTime_sec2.getText().toString()),
                    Integer.parseInt(loopTime_mse2.getText().toString())
            );
            if(startTime>=looptime
                    || startTime>previewMP.getDuration()
                    || looptime>previewMP.getDuration()
                    || startTime<0
                    || looptime<0
                    || courseNameET.getText().toString().equals("")
                    || (!isBattleBox.isChecked() && (currentLapET.getText().toString().equals("") && maxLapET.getText().toString().equals("")))
            ){
                Toast.makeText(this, "Invalid input. Please make sure your input value is correct.", Toast.LENGTH_LONG).show();
                return;
            }
            String newFileName = courseNameET.getText().toString()
                    + (isBattleBox.isChecked()?"":("(" + currentLapET.getText().toString() + "_" + maxLapET.getText().toString()) + ")") + ".mp3";
            SharedPreferences.Editor loopTimeSP = getSharedPreferences("MTour_duration",Context.MODE_PRIVATE).edit();
            loopTimeSP.putString(newFileName,startTime + "," + looptime);
            loopTimeSP.apply();

            SharedPreferences.Editor songNameSP = getSharedPreferences("MTour_name",Context.MODE_PRIVATE).edit();
            songNameSP.putString(newFileName,songName);
            songNameSP.apply();

            try {
                copyMusicFile(songPath,getExternalFilesDir(null) + "/Custom Music/" + newFileName);
                Log.d("ADD_MUSIC", String.valueOf(new File(getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"))));
            } catch (Exception ioe) {
                Log.e("ADD_MUSIC",ioe.toString());
                ioe.printStackTrace();
                Toast.makeText(this, "Cannot copy the file: " + ioe.getCause() + ioe.getMessage(), Toast.LENGTH_SHORT).show();
            }

            updateRecycleView();
        });
        songDialog.setCancelable(false);
        songDialog.show();
    }

    private int timeToMil(int min,int sec, int mil){
        return min*60*1000 + sec*1000 + mil;
    }

    private TextView newLabel(String txt){
        TextView tv = new TextView(getApplicationContext());
        tv.setText(txt);
        return tv;
    }

    private String getUriFileName(Uri uri){
        Cursor returnCursor =
                getContentResolver().query(uri, null, null, null, null);
        /*
         * https://developer.android.com/training/secure-file-sharing/retrieve-info#java
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        return returnCursor.getString(nameIndex);
    }

    private void copyMusicFile(Uri songPath, String destPath){
        // Create Directory if the directory did not exist
        File cusMfolder = new File(getExternalFilesDir(null) + "/Custom Music");
        if (!cusMfolder.exists()){
            cusMfolder.mkdirs();
        }
        // Open a specific media item using InputStream.
        ContentResolver resolver = getApplicationContext().getContentResolver();
        try (InputStream orgFileStream = resolver.openInputStream(songPath)) {
            byte[] buffer = new byte[1024];
            int readAtPos;
            OutputStream newFileStream = new FileOutputStream(destPath,false);
            while ((readAtPos = orgFileStream.read(buffer)) > 0) {
                newFileStream.write(buffer, 0, readAtPos);
            }
            orgFileStream.close();
            newFileStream.close();
        } catch (IOException e) {
            Log.e("ADD_MUSIC", String.valueOf(e) + " - " + destPath);
            e.printStackTrace();
        }

    }

    private void saveRunningState(){
        SharedPreferences.Editor recordingSP = getSharedPreferences("MTour_recordingInProgress",Context.MODE_PRIVATE).edit();
        recordingSP.putBoolean("val", isRunning);
        recordingSP.apply();
        Log.d("RUNNING", String.valueOf(isRunning));
    }

    private void saveSharedPreference(String fileName,String key,String val){
        SharedPreferences.Editor recordingSP = getSharedPreferences(fileName,Context.MODE_PRIVATE).edit();
        recordingSP.putString(key, val);
        recordingSP.apply();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(stopRec_BC);
        super.onDestroy();
    }
}