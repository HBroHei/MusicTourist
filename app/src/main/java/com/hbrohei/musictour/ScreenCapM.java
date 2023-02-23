package com.hbrohei.musictour;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Toast;

public class ScreenCapM {
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


    /**
     * Create a screen capture object, with display settings generated from Display Metrics
     * @param ctx The application context
     * @param foregroundService the service to run in foreground (Note: Use "createForegroundNoti" in the Service for it to work)
     * @param displayMetrics the display metrics
     */
    public ScreenCapM(Context ctx, Class<?> foregroundService, DisplayMetrics displayMetrics){
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
    public ScreenCapM(Context ctx, Class<?> foregroundService, int scrDensity, int width, int height){
        appContext = ctx;
        service = foregroundService;
        intensity = scrDensity;
        dwidth = width;
        dheight = height;

    }

    /**
     * Prepares the setup for media projection.
     */
    public void prepare(){
        /*Intent serviceIntent = new Intent(appContext,service);
        appContext.startService(serviceIntent);*/

        mProjectionManager = (MediaProjectionManager) appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        };
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
        //Intent notiClickAction = new Intent(this, MainActivity.class);
        Intent notiClickAction = new Intent();
        notiClickAction.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        nBuilder.setContentIntent(PendingIntent.getActivity(ctx, 0, notiClickAction, PendingIntent.FLAG_IMMUTABLE))
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("return to app")
                .setWhen(System.currentTimeMillis());

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
            start(1,RESULT_OK,projectionIntent);
            createVirtualDisplay();
        }
    }

    public void createVirtualDisplay(){
        vDisplay = createVD();
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

    public boolean start(int requestCode, int resultCode, Intent data){
        if (requestCode != 1) {
            Toast.makeText(appContext, "-", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (resultCode == RESULT_OK) {
            projectionIntent = data;

            mProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mProjection.registerCallback(mProjectionCallback, null);
            vDisplay = createVD();

            return true;
        }
        return false;
    }

    public void stop(){
        mProjection.stop();
        mProjection = null;
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

