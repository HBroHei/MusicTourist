package com.hbrohei.musictour;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.NOTIFICATION_SERVICE;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenCap {
    private Context appContext;
    private Class<?> service;
    private DisplayMetrics matris;
    private int intensity;
    private int dwidth;
    private int dheight;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection.Callback mProjectionCallback;
    private MediaProjection mProjection;
    private VirtualDisplay vDisplay;
    private Surface surface;
    private Intent projectionIntent;
    private MediaRecorder mRecorder;
    private AudioRecord aRec;
    private Thread recThread;

    private boolean willRecord = false;


    /**
     * Create a screen capture object, with display settings generated from Display Metrics
     * @param ctx The application context
     * @param foregroundService the service to run in foreground (Note: Use "createForegroundNoti" in the Service for it to work)
     * @param displayMetrics the display metrics
     */
    public ScreenCap(Context ctx, Class<?> foregroundService, DisplayMetrics displayMetrics) {
        appContext = ctx;
        service = foregroundService;
        matris = displayMetrics;
        intensity = matris.densityDpi;
        dwidth = matris.widthPixels;
        dheight = matris.heightPixels;
    }

    /**
     * Create a screen capture object, with display settings manually set
     * @param ctx
     * @param foregroundService the service to run in foreground (Note: Use "createForegroundNoti" in the Service for it to work)
     * @param scrDensity
     * @param width
     * @param height
     */
    public ScreenCap(Context ctx, Class<?> foregroundService, int scrDensity, int width, int height) {
        appContext = ctx;
        service = foregroundService;
        intensity = scrDensity;
        dwidth = width;
        dheight = height;

    }

    /**
     * Prepares the setup for media projection.
     */
    public void prepare() {
        Intent serviceIntent = new Intent(appContext, service);
        appContext.startService(serviceIntent);

        mProjectionManager = (MediaProjectionManager) appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        };


    }

    /**
     * Prepares the setup for media projection, without starting the foreground service
     */
    public void prepareWithoutService() {
        mProjectionManager = (MediaProjectionManager) appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        };
    }

    public void recordPrepare(Context ctx) {
        //https://stackoverflow.com/questions/14336338/screen-video-record-of-current-activity-android?answertab=trending#tab-top
        mRecorder = new MediaRecorder();
        mRecorder.reset();
        //mRecorder.release();
        //mRecorder.reset();

    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void recordInternalSound(Context ctx){
        /*OLD CODES
        AudioPlaybackCaptureConfiguration config =
                new AudioPlaybackCaptureConfiguration.Builder(mProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        AudioRecord record = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_MP3)
                        .setSampleRate(8000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setAudioPlaybackCaptureConfig(config)
                .build();*/

        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build();

        AudioFormat af = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setSampleRate(44100)
                .build();
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        aRec = new AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(af)
                .setBufferSizeInBytes(AudioRecord.getMinBufferSize(af.getSampleRate(), AudioFormat.CHANNEL_IN_MONO, af.getEncoding()))
                .build();
        aRec.startRecording();


        recThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    FileOutputStream outFile = new FileOutputStream(appContext.getExternalFilesDir(null) + "/temp2.wav");
                    byte[] rawData = new byte[8];

                    // read from the AudioRecord
                    while(!recThread.isInterrupted()) {
                        aRec.read(rawData, 0, 8);
                        outFile.write(rawData,0, rawData.length);
                    }

                    outFile.flush();
                    outFile.close();
                    addWavHeader(appContext.getExternalFilesDir(null) + "/temp2.wav");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        recThread.start();
        //DISABLED due to not supported by native MediaRecorder
        //mRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
        //mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

    }

    //Long code incoming
    /**
     * Add the wave header to a file
     * @param filePath the wave file path to be outputted
     * @throws IOException Error occured
     */
    private void addWavHeader(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        byte[] audioData = new byte[fis.available()];
        fis.read(audioData);
        fis.close();

        long audioDataSize = 0; //audioData.length;
        //long totalFileSize = audioDataSize + 36;
        long totalFileSize = 0;

        // Write the header information to a byte array
        // https://stackoverflow.com/questions/5245497/how-to-record-wav-format-file-in-android
        byte[] header = new byte[44];
        // RIFF header
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        // Total file size - 8 bytes
        header[4] = (byte) (totalFileSize & 0xff);
        header[5] = (byte) ((totalFileSize >> 8) & 0xff);
        header[6] = (byte) ((totalFileSize >> 16) & 0xff);
        header[7] = (byte) ((totalFileSize >> 24) & 0xff);
        header[8] = 'W'; // Wave file format
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // "fmt " chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // Subchunk1 size (16 bytes)
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // Audio format (PCM)
        header[21] = 0;
        header[22] = (byte) 1; // Number of channels
        header[23] = 0;
        int sampleRate = 44100;
        int numChannels = 1;
        int bitsPerSample = 16;
        header[24] = (byte) (44100 & 0xff); // Sample rate
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        long byteRate = ((sampleRate * numChannels * 8) / 8);
        header[28] = (byte) byteRate; // Byte rate
        header[29] = (byte) (byteRate >>> 8 & 0xFF);
        header[30] = (byte) (byteRate >>> 16 & 0xFF);
        header[31] = (byte) (byteRate >>> 24 & 0xFF);
        header[32] = (byte) (numChannels * bitsPerSample / 8); // Block align
        header[33] = 0;

        header[34] = (byte) bitsPerSample; // Bits per sample
        header[35] = 0;
        header[36] = 'd'; // "data" chunk
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (audioDataSize & 0xff); // Data size
        header[41] = (byte) ((audioDataSize >> 8) & 0xff);
        header[42] = (byte) ((audioDataSize >> 16) & 0xff);
        header[43] = (byte) ((audioDataSize >> 24) & 0xff);

        // Write the header to the beginning of the file
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(header);
        fos.write(audioData);
        fos.close();

}

    /**
     * Create a notification for foreground service. Use this in "onStartCommand"
     * @param ctx
     * @param notiId
     */
    public static void createForegroundNoti(Context ctx,int notiId){
        //Build Notification Channel
        NotificationManager nManager = (NotificationManager)ctx.getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("scrCapTest", "Screen Capturing", NotificationManager.IMPORTANCE_LOW);
        nManager.createNotificationChannel(channel);

        //Build the notification
        Notification.Builder nBuilder = new Notification.Builder(ctx,"scrCapTest");

        Intent notiClickAction = new Intent("stopRec");

        nBuilder.setContentIntent(PendingIntent.getBroadcast(ctx,0,notiClickAction,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE))
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Click here to stop")
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        Notification n = nBuilder.build();
        //Start foreground service with notification
        Service s = (Service) ctx;
        s.startForeground(notiId,n);
    }

    /**
     * Request permission from user to capture the screen
     * *This function will call onActivityResult(int,int,Intent). Pass the data to start(int,int,Intent) afterwards*
     * @param act The current activity
     */
    public void requestPermission(android.app.Activity act){
        if(mProjection==null){
            act.startActivityForResult(mProjectionManager.createScreenCaptureIntent(),1);
        }
        else{
            Toast.makeText(appContext, "Permission already aquired", Toast.LENGTH_SHORT).show();
            start(act,1,RESULT_OK,projectionIntent);
            createVirtualDisplay();
        }
    }

    /**
     * Request permission from user to capture the screen
     * *This uses the ActivityResultLauncher. Create the Launcher in onCreate() first to avoid errors. Pass the data to start(int,int,Intent) afterwards*
     * @param arl The ActivityResultLauncher
     */
    public void requestPermission(Context ctx, ActivityResultLauncher<Intent> arl){
        if(mProjection==null){
            arl.launch(mProjectionManager.createScreenCaptureIntent());
        }
        else{
            Toast.makeText(appContext, "Permission already aquired", Toast.LENGTH_SHORT).show();
            start(ctx,1,RESULT_OK,projectionIntent);
            createVirtualDisplay();
        }
    }


    public void createVirtualDisplay(){
        vDisplay = createVD();
    }
    public void createVirtualDisplayWithRecorder(){
        vDisplay = mProjection.createVirtualDisplay(
                "Display",
                dwidth, dheight,
                intensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mRecorder.getSurface(),
                null,
                null
        );
    }

    private VirtualDisplay createVD(){
        return mProjection.createVirtualDisplay(
                "Display",
                dwidth, dheight,
                intensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
        );
    }

    public MediaProjectionManager getmProjectionManager() {
        return mProjectionManager;
    }

    public void setmProjectionManager(MediaProjectionManager mProjectionManager) {
        this.mProjectionManager = mProjectionManager;
    }

    public MediaProjection.Callback getmProjectionCallback() {
        return mProjectionCallback;
    }

    public void setmProjectionCallback(MediaProjection.Callback mProjectionCallback) {
        this.mProjectionCallback = mProjectionCallback;
    }

    public MediaProjection getmProjection() {
        return mProjection;
    }

    public void setmProjection(MediaProjection mProjection) {
        this.mProjection = mProjection;
    }

    /**
     * Start the recording
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    public boolean start(Context ctx, int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK) {
            mRecorder.reset();

            mProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mProjection.registerCallback(mProjectionCallback, null);

            //Set the required attributes of mRecorder
            /*
                PaLM: The following is the correct order in which the methods should be called:
                    setVideoSource()
                    setAudioSource()
                    setOutputFormat()
                    setAudioEncoder()
                    setVideoEncoder()
                    setVideoEncodingBitRate()
                    setVideoFrameRate()
             */
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                recordInternalSound(ctx);
                mRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
            }
            else{
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            // If statement is here if I intended to switch the Encoder in the future for every methods
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            }
            else{
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            }
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mRecorder.setVideoEncodingBitRate(10*1000000);
            mRecorder.setVideoFrameRate(30);
            //Temporary fix for H264 as H264 require width and height divisible by 16
            //https://stackoverflow.com/a/57943426
            if(dwidth%16!=0){dwidth -= dwidth%16;}
            if(dheight%16!=0){dheight -= dheight%16;}
            mRecorder.setVideoSize(dwidth,dheight);
            Log.d("SAVE_PATH",appContext.getExternalFilesDir(null) + "/temp.mp4");
            mRecorder.setOutputFile(appContext.getExternalFilesDir(null) + "/temp.mp4");

            willRecord = true;

            //Prepare / check if recorder is ready
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("START","Preparation failed: " + e);
            }

            projectionIntent = data;

            if (willRecord) {
                surface = mRecorder.getSurface();
            }
            vDisplay = createVD();

            Log.d("LOGGING", String.valueOf(surface.isValid()));

            try {
                mRecorder.start();
            }catch(Exception e){
                Log.e("ERROR",e.toString());
            }

            return true;
        }
        else{
            Log.w("RUNNING","Permission failed to aquire");
        }
        return false;
    }

    public void stop(){
        Log.d("LOGGING", String.valueOf(mRecorder));
        try{
            mRecorder.stop();
            mRecorder.release();
            //mProjection = null;
            mProjection.stop();

            //Stop AudioRecord
            recThread.interrupt();
            recThread.join();

            aRec.stop();
            aRec.release();
            aRec = null;
        }
        catch(RuntimeException | InterruptedException e) {
            Log.e("START", String.valueOf(e));
        }


    }

    public Surface getSurface() {
        return surface;
    }

    public void setSurfaceView(SurfaceView sv){
        surface = sv.getHolder().getSurface();
    }

    public int getIntensity() {
        return intensity;
    }

    public int getWidth() {
        return dwidth;
    }

    public int getHeight() {
        return dheight;
    }

    /**
     * Returns the Minutes, Seconds and Milliseconds of a time given (in Milliseconds)
     * @param time The time in milliseconds
     * @return the Minutes, Seconds and Milliseconds stored respectively in a int array
     */
    public static int[] returnMSM(int time){
        int[] returnTime = new int[3];
        returnTime[0] = time/1000/60;
        returnTime[1] = time/1000 - returnTime[0]*60;
        returnTime[2] = time - returnTime[0]*60*1000 - returnTime[1]*1000;

        return returnTime;
    }

}

