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
    public void prepareWithoutService(){
        mProjectionManager = (MediaProjectionManager) appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        };
    }

    public void recordPrepare(){
        //https://stackoverflow.com/questions/14336338/screen-video-record-of-current-activity-android?answertab=trending#tab-top
        mRecorder = new MediaRecorder();
        //Set the required attributes of mRecorder
        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //mRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mRecorder.setVideoEncodingBitRate(512*1000);
        mRecorder.setVideoFrameRate(24);
        mRecorder.setVideoSize(dwidth,dheight);
        Log.d("SAVE_PATH",appContext.getExternalCacheDir() + "/temp.mp4");
        mRecorder.setOutputFile(appContext.getExternalCacheDir() + "/temp.mp4");

        //Prepare / check if recorder is ready
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("START","Preparation failed: " + e);
        }

        willRecord = true;
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void recordInternalSound(Context ctx){
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
                    .build();
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
        /*Intent notiClickAction = new Intent(ctx, EndServiceActivity.class);

        final Intent notiClickAction = new Intent(ctx, MainActivity.class);
        notiClickAction.setAction(Intent.ACTION_MAIN);
        notiClickAction.addCategory(Intent.CATEGORY_LAUNCHER);
        notiClickAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notiClickAction.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);/**/

        Intent notiClickAction = new Intent("stopRec");

        nBuilder.setContentIntent(PendingIntent.getBroadcast(ctx,0,notiClickAction,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE))
        //nBuilder.setContentIntent(PendingIntent.getActivity(ctx, 0, notiClickAction, PendingIntent.FLAG_IMMUTABLE))
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //recordInternalSound(ctx);
            }

            projectionIntent = data;

            mProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mProjection.registerCallback(mProjectionCallback, null);
            if (willRecord) {
                surface = mRecorder.getSurface();
            }
            vDisplay = createVD();

            Log.d("LOGGING", String.valueOf(surface.isValid()));

            mRecorder.start();

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
        }
        catch(RuntimeException re){
            Log.e("START", String.valueOf(re));
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


}

