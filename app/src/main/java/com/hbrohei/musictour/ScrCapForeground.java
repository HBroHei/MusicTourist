package com.hbrohei.musictour;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.SyncStateContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.PixelCopy;
import android.view.Surface;
import android.widget.Toast;

import androidx.appcompat.widget.ThemedSpinnerAdapter;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ScrCapForeground extends Service {

    private ScreenCap scrCap;

    private Handler checkTitleLoop;
    private Runnable titleLoopRunnable;

    private String lapCount = "";
    private String[] musicFilesList;
    private String currentCourse;
    private String currentMusicName;
    private String prevMusicName;

    private String playerName = "hammerbrohei";

    private MediaPlayer mPlayer;
    private int startTime;
    private int loopTime;
    private boolean battleMusicTrigger = false;

    public ScrCapForeground() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        /*
        scrCap = new ScreenCap(
                this,
                ScrCapForeground.class,
                Integer.parseInt(getSharedPreference("MTour_device_res", "density")),
                Integer.parseInt(getSharedPreference("MTour_device_res","width")),
                Integer.parseInt(getSharedPreference("MTour_device_res","height"))
        );

        scrCap.prepareWithoutService();

        //startActivityForResult(scrCap.getmProjectionManager(),1);
/**/
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
             Log.d("CMD_FLAG", String.valueOf(intent.getIntExtra("flag", -1)));
        }
        catch(NullPointerException npe){
            Log.d("FNOTI","catched");
            ScreenCap.createForegroundNoti(this,228);
            return super.onStartCommand(intent, flags, startId);
        }
        if (intent.getIntExtra("flag", -1) == -1){
            ScreenCap.createForegroundNoti(this,228);
        }
        else if (intent.getIntExtra("flag", -1) == 1) {
            //Stop Service
            Log.d("PROCESS_APP","Stopping");
            stopForeground(true);
            checkTitleLoop.removeCallbacksAndMessages(null);
            stopMusic();
            stopSelf();
        }
        //Run the OCR process
        else if (intent.getIntExtra("flag", -1) == 2) {
            Log.d("PROCESS_APP","Looping");
            final int width = intent.getIntExtra("scrCap_width",-1);
            final int height = intent.getIntExtra("scrCap_height",-1);
            final Surface surface = intent.getParcelableExtra("scrCap_surface");
            //Get a list of courses to be replaced by custom music
            //try {
                musicFilesList = new File(getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"));
            //}
            /* TODO Find the correct Exception
            catch(FileNotFoundException fnfe){
                Toast.makeText(this, "No custom music found!", Toast.LENGTH_SHORT).show();
                //TODO Create directory
            }*/

            mPlayer = new MediaPlayer();

            try {
                for (int i = 0; i < Objects.requireNonNull(musicFilesList).length; i++) {
                    musicFilesList[i] = musicFilesList[i].replaceAll(".mp3", "");
                    if (musicFilesList[i].contains("("))
                        musicFilesList[i] = musicFilesList[i].substring(0, musicFilesList[i].indexOf("("));
                }
            }
            catch(NullPointerException npe){
                npe.printStackTrace();
            }
            //Start OCR
            checkTitleLoop = new Handler();
            titleLoopRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d("PROCESS_APP","Looping");
                    if(surface == null){
                        checkTitleLoop.postDelayed(this, 500);
                        Log.w("Surface Empty","Surface is empty!");
                        return;
                    }
                    else if (surface.isValid()) {
                        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        PixelCopy.request(surface, bmp, i -> {
                            recognizeText(bmp);
                        }, new Handler(Looper.getMainLooper()));
                    }

                    //Custom Loop
                    if(mPlayer.isPlaying()){
                        if(mPlayer.getCurrentPosition() >= loopTime){
                            mPlayer.seekTo(startTime);
                            Log.d("MUSIC_FILE","Music exceeded " + loopTime + ", looping music...");
                        }
                    }
                    checkTitleLoop.postDelayed(this, 500);
                }
            };
            checkTitleLoop.postDelayed(titleLoopRunnable,500);
        }

        return super.onStartCommand(intent, flags, startId);
    }


    private void recognizeText(Bitmap bmp){
        Log.d("SCANNED_TEXT","D: Analyzing...");
        TextRecognizer tRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage ocrImg = InputImage.fromBitmap(bmp, 0);
        Task<Text> ocrTask = tRecognizer.process(ocrImg)
                .addOnSuccessListener(visionText -> {
                    //Log.d("SCANNED_TEXT","SUCCESS, text=" + visionText.getText());
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        String blockText = block.getText();
                        Log.d("SCANNED_TEXT",blockText);

                        if(blockText.toLowerCase(Locale.ROOT).contains("1/2")){
                            lapCount = "1_2";
                            setMusic(currentCourse+"("+lapCount+")");
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("2/2")){
                            lapCount = "2_2";
                            setMusic(currentCourse+"("+lapCount+")");
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("3/3")){
                            lapCount = "3_3";
                            setMusic(currentCourse+"("+lapCount+")");
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("5/5")){
                            lapCount = "5_5";
                            setMusic(currentCourse+"("+lapCount+")");
                        }/*
                        else if(isThatFinish(blockText.toLowerCase(Locale.ROOT))){
                            //Log.d("SCANNED_TEXT","FOUND: " + blockText);
                            stopMusic();
                        }*/
                        else if(blockText.toLowerCase(Locale.ROOT).contains(playerName)){
                            Log.d("PlayerName","Player's name detected: " + blockText);
                            lapCount = "";
                            stopMusic();
                            battleMusicTrigger = false;
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("pop your opponents")) {
                            Log.d("MUSIC_FILE","Stage 1");
                            battleMusicTrigger = true;
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("steer") && !mPlayer.isPlaying()){
                            File musicFileCheck = new File(getExternalFilesDir(null) + "/Custom Music/" + currentCourse + ".mp3");
                            if(musicFileCheck.exists() && !musicFileCheck.isDirectory() && battleMusicTrigger){
                                setMusic(currentCourse);
                            }
                            else{
                                setMusic(currentCourse+"("+lapCount+")");
                            }
                        }
                        else{
                            int courseListLoc = isCourseInString(blockText.toLowerCase(Locale.ROOT));
                            //Log.d("SCANNED_TEXT","In list=" + courseListLoc);
                            if(courseListLoc!=-1){
                                currentCourse = musicFilesList[courseListLoc];
                                Log.d("SCANNED_TEXT","New course set: " + currentCourse);
                                battleMusicTrigger = false;
                            }
                        }
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("SCANNED_TEXT", String.valueOf(e));
                });
    }/**/

    private void setMusic(String s) {
        if(!mPlayer.isPlaying() || !prevMusicName.equals(s)){
            Log.d("MUSIC_FILE",getExternalFilesDir(null) + "/Custom Music/" + s + ".mp3");
            stopMusic();
            try {
                mPlayer.setDataSource(getExternalFilesDir(null) + "/Custom Music/" + s + ".mp3");
                mPlayer.setVolume(0.25f,0.25f);
                mPlayer.prepare();
                mPlayer.start();
                prevMusicName = s;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("MUSIC_FILE","Cannot play file: " + e);
            }
            catch (IllegalStateException ise){
                ise.printStackTrace();
                Log.e("MUSIC_FILE","Media player is occupied: " + ise);
            }
        }
        //Get loops time
        SharedPreferences timeSP = getSharedPreferences("MTour_duration", Context.MODE_PRIVATE);
        String[] cropedTime = timeSP.getString(prevMusicName + ".mp3","0,10000").split(",");
        Log.d("MUSIC_FILE", Arrays.toString(cropedTime));
        startTime = Integer.parseInt(cropedTime[0]);
        loopTime = Integer.parseInt(cropedTime[1]);
    }

    private void stopMusic(){
        if(mPlayer.isPlaying()){
            try {
                mPlayer.stop();
                mPlayer.reset();
            }
            catch(IllegalStateException ise){
                ise.printStackTrace();
                Log.e("MUSIC_FILE","Cannot reset media player: " + ise);
            }
        }
    }

    public int isCourseInString(String str){
        for(int i=0;i<musicFilesList.length;i++){
            //Log.d("SCANNED_TEXT",str + "||" + musicFilesList[i]);
            if(str.contains(musicFilesList[i].toLowerCase(Locale.ROOT))){
                return i;
            }
        }
        return -1;
    }

    private boolean isThatFinish(String s){
        if(s.length()>9 || s.length()<4) return false;
        int similarityIdx = 0;
        int finishStrIdx = 0;
        final String finish = "finish";
        for(int i=0;i<s.length();i++){
            //similarityIdx += s.charAt(i)==finish.charAt(finishStrIdx)?1:0;
            if(finish.contains(String.valueOf(s.charAt(i))) && finishStrIdx==0){
                finishStrIdx = finish.indexOf(s.charAt(i));
            }
            else{
                if(s.charAt(i)==finish.charAt(finishStrIdx)){
                    similarityIdx++;
                    finishStrIdx++;
                }
            }
        }
        return similarityIdx>=4;
    }

    private String getSharedPreference(String fileName,String key){
        SharedPreferences spGet = getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return spGet.getString(key,"");
    }

}