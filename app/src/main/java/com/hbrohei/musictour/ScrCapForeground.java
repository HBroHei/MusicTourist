package com.hbrohei.musictour;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
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
import java.util.regex.Pattern;

public class ScrCapForeground extends Service {

    private ScreenCap scrCap;

    private Thread checkTitleLoop;
    private Runnable titleLoopRunnable;

    private String lapCount = "1_2";
    private boolean isFinalLap = false;
    private boolean finished = false;
    private String[] musicFilesList;
    private String currentCourse;
    private String currentMusicName;
    private String prevMusicName;

    private String playerName = "hammerbrohei";
    private boolean isPlayerNameShown = false;

    private MediaPlayer mPlayer;
    private int startTime;
    private int loopTime;
    private boolean battleMusicTrigger = false;
    private byte frameCounter = 0;
    private BroadcastReceiver stopRec_BC;


    private short threadSleepTime = 1500;
    public ScrCapForeground() {}

    @Override
    public IBinder onBind(Intent intent) {throw new UnsupportedOperationException("Unsupported Binding");}

    @Override
    public void onCreate() {super.onCreate();}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Gets why this service is being called
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
        // Stop the entire app
        else if (intent.getIntExtra("flag", -1) == 1) {
            //Stop Service
            Log.d("PROCESS_APP","Stopping");
            stopForeground(true);
            //checkTitleLoop.removeCallbacksAndMessages(null);
            stopMusic();
            stopSelf();
        }
        //Run the OCR process
        else if (intent.getIntExtra("flag", -1) == 2) {
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

            //Replace all .mp3 with nothing for the list to use
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
            checkTitleLoop = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d("PROCESS_APP", "Looping");
                        if (surface == null) {
                            try {
                                Thread.sleep(threadSleepTime);
                                Log.w("Surface Empty", "Surface is empty!");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            continue;
                        } else if (surface.isValid()) {
                            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            PixelCopy.request(surface, bmp, i -> {
                                recognizeText(bmp);
                            }, new Handler(Looper.getMainLooper()));
                        }

                        // Custom Loop for the music
                        if (mPlayer.isPlaying()) {
                            if (mPlayer.getCurrentPosition() >= loopTime) {
                                mPlayer.seekTo(startTime);
                                //Log.d("MUSIC_FILE", "Music exceeded " + loopTime + ", looping music...");
                            }
                        }
                        try {
                            Thread.sleep(threadSleepTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });
            checkTitleLoop.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }


    private void recognizeText(Bitmap bmp){
        //Log.d("SCANNED_TEXT","D: Analyzing...");
        TextRecognizer tRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage ocrImg = InputImage.fromBitmap(bmp, 0);
        Task<Text> ocrTask = tRecognizer.process(ocrImg)
                .addOnSuccessListener(visionText -> {
                    //Log.d("SCANNED_TEXT","SUCCESS, text=" + visionText.getText());
                    // Resets isPlayerNameShown on every other frame
                    if(++frameCounter==2){
                        frameCounter = 0;
                        isPlayerNameShown = false;
                    }
                    // iterate through every text block (group)
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        String blockText = block.getText();
                        //Log.d("SCANNED_TEXT",blockText);

                        final String[] lapCounts = {"1/2","2/2","1/3","2/3","3/3","1/5","2/5","3/5","4/5","5/5"};

                        final int sim_finish = similarityIndex("finish!",blockText.toLowerCase(Locale.ROOT));
                        final int sim_start = similarityIndex("go!",blockText.toLowerCase(Locale.ROOT));
                        /*
                        // Check for text "START". Currently not checkable
                        if(sim_start<2){
                            Log.d("SCANNED_TEXT","FOUND: " + blockText + " IDX:" + sim_start);
                            lapCount = "1_2";
                            setMusic(currentCourse+"("+lapCount+")");
                        }
                        else */
                        // If: lap count check
                        if(blockText.toLowerCase(Locale.ROOT).contains("1/2")){
                            lapCount = "1_2";
                            setMusic(currentCourse+"("+lapCount+")");
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("2/2") && !finished){
                            lapCount = "2_2";
                            setMusic(currentCourse+"("+lapCount+")");
                            isFinalLap = true;
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("3/3") && !finished){
                            lapCount = "3_3";
                            setMusic(currentCourse+"("+lapCount+")");
                            isFinalLap = true;
                        }
                        else if(blockText.toLowerCase(Locale.ROOT).contains("5/5") && !finished){
                            lapCount = "5_5";
                            setMusic(currentCourse+"("+lapCount+")");
                            isFinalLap = true;
                        }
                        // Check "FINISH!" word. Currently not checkable
                        /*
                        else if(sim_finish<4){
                            Log.d("SCANNED_TEXT","FOUND END: " + blockText + " IDX:" + sim_finish);
                            //stopMusic();
                        }/**/
                        // Check for race ended with
                        // 1. isFinalLap = true, and
                        // 2a. 1st/2nd/3rd/_th place! text shows up, or
                        // 2b. Player name is displayed (at the result screen)
                        else if(isFinalLap && (
                                    Pattern.compile(".?+(st|nd|rd|th) place!").matcher(blockText.toLowerCase(Locale.ROOT)).find()
                                    || blockText.toLowerCase(Locale.ROOT).contains(playerName)
                        )){
                            //Log.d("PlayerName","Player's name detected: " + blockText);
                            lapCount = "";
                            stopMusic();
                            battleMusicTrigger = false;
                            isFinalLap = false;
                            finished = true;
                        }
                        // if: check for battle mode starting
                        else if(blockText.toLowerCase(Locale.ROOT).contains("pop your opponents")) {
                            Log.d("MUSIC_FILE","Stage 1");
                            battleMusicTrigger = true;
                            isPlayerNameShown = false;
                        }
                        // If: check if race started, and the music is not playing
                        else if(blockText.toLowerCase(Locale.ROOT).contains("steer") && !mPlayer.isPlaying()){
                            File musicFileCheck = new File(getExternalFilesDir(null) + "/Custom Music/" + currentCourse + ".mp3");
                            finished = false;
                            if(musicFileCheck.exists() && !musicFileCheck.isDirectory() && battleMusicTrigger){
                                setMusic(currentCourse);
                            }
                            else{
                                setMusic(currentCourse+"("+lapCount+")");
                            }
                        }
                        // Check for course name is on screen, then set the current course to the one on-screen
                        else{
                            if(isPlayerNameShown){
                                int courseListLoc = isCourseInString(blockText.toLowerCase(Locale.ROOT));
                                //Log.d("SCANNED_TEXT",blockText.toLowerCase(Locale.ROOT) + " In list=" + courseListLoc);
                                if (courseListLoc != -1) {
                                    currentCourse = musicFilesList[courseListLoc];
                                    //Log.d("SCANNED_TEXT","New course set: " + currentCourse);
                                    battleMusicTrigger = false;
                                    // Increase the frequency for checking
                                    threadSleepTime = 750;
                                }

                            }
                            else if((!isFinalLap) && blockText.toLowerCase(Locale.ROOT).contains(playerName)) {
                                isPlayerNameShown = true;
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

    public int similarityIndex(String s1, String s2) { // Levenshtein Distance
        int m = s1.length();
        int n = s2.length();

        // Create a matrix to store the distances between all substrings
        int[][] dp = new int[m + 1][n + 1];

        // Initialize the first row and column of the matrix
        for(int i = 0; i <= m; i++){
            dp[i][0] = i;
        }
        for(int j = 0; j <= n; j++){
            dp[0][j] = j;
        }

        // Fill the remaining cells of the matrix
        for(int i = 1; i <= m; i++){
            for(int j = 1; j <= n; j++){
                if(s1.charAt(i - 1) == s2.charAt(j - 1)){
                    dp[i][j] = dp[i - 1][j - 1];
                }
                else{
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }

        // Return the distance between the two strings
        return dp[m][n];
    }

    private String getSharedPreference(String fileName,String key){
        SharedPreferences spGet = getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return spGet.getString(key,"");
    }

    private void addBroadcastreceiver(){
        stopRec_BC = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                checkTitleLoop.interrupt();
                try {
                    checkTitleLoop.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        registerReceiver(stopRec_BC,new IntentFilter("stopRec"));
    }

}