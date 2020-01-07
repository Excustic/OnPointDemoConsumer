package com.example.onpointdemoconsumer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class BackgroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final long NORMAL_DURATION = 5*1000;
    private static final String TAG = "BackgroundLocService";
    private LocationCallback locationCallback;

    public BackgroundService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OnPointDemoConsumer")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_custom_launcher_vector)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_custom_launcher_72))
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        //do heavy work on a background thread
        //stopSelf();
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(TAG, "onLocationResult: "+locationResult.getLastLocation().getLatitude());
                if(locationResult!=null)
                {
                    Location location = locationResult.getLastLocation();
                    String locString = new StringBuilder(""+location.getLatitude())
                            .append(",")
                            .append(location.getLongitude())
                            .toString();
                    try{
                        MainActivity.getInstance().updateTextView(locString);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        startThread();
        return START_STICKY;
    }

    private void startThread() {
        Log.d(TAG, "getLocation: initiating request of updates");
        new Runnable() {
            @Override
            public void run() {
                try{
                    FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(BackgroundService.this);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(NORMAL_DURATION);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setSmallestDisplacement(10f);
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                }
                catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }.run();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
