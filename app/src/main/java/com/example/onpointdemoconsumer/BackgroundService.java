package com.example.onpointdemoconsumer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.sql.Time;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

public class BackgroundService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final long NORMAL_DURATION = 60*1000;
    private static final String TAG = "BackgroundLocService";
    private LocationCallback locationCallback;
    private RequestQueue queue;
    private String locString, locString2;
    private Timer myTimer;
    private IncomingHandler handler;
    private SharedPreferences sp;
    private DataSender DS;
    private String userId, DatabaseURL, input;
    private static GoogleApiClient mApiClient;

    public BackgroundService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        if (mApiClient == null) {
            mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mApiClient.connect();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        Log.d(TAG, "onStartCommand: Service started");
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        input = sp.getString("inputExtra","");
        userId = sp.getString("userId","");
        DatabaseURL = sp.getString("DatabaseURL","https://onpoint-backend.herokuapp.com/api");
        handler = new IncomingHandler(BackgroundService.this);
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OnPointDemoConsumer")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_location_on_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_custom_launcher_72))
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        queue = Volley.newRequestQueue(this);

        //do heavy work on a background thread
        //stopSelf();
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if(locationResult!=null)
                {
                    Log.d(TAG, "onLocationResult: "+locationResult.getLastLocation().getLatitude());
                    Location location = locationResult.getLastLocation();
                    locString = new StringBuilder(""+location.getLatitude())
                            .append(",")
                            .append(location.getLongitude())
                            .toString();
                }
            }
        };
        //startThread();
        myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new mainTask(), 2000, NORMAL_DURATION);
        return START_REDELIVER_INTENT;
    }

    private void startThread() {
        Log.d(TAG, "getLocation: initiating request of updates");
        ((Runnable) () -> {
            try {
                FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(BackgroundService.this);
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(15000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setSmallestDisplacement(0);
                Intent locationIntent = new Intent(getApplicationContext(), BackgroundService.class);
                PendingIntent locationPendingIntent = PendingIntent.getService(
                        getApplicationContext(),
                        0,
                        locationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationPendingIntent);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }).run();

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

    public void handleMessage(Message msg){
        try{
            locString = sp.getString("locString",null);
            String latlong = (locString==null) ? locString2 : locString;
            Log.d(TAG, "handleMessage: "+MainActivity.getInstance()+ latlong);
            if(MainActivity.getInstance() != null)
            MainActivity.getInstance().updateTextView(latlong);
            if(DS == null){
                DS = new DataSender(this,DatabaseURL,userId);
            }
            DS.uploadLocation(latlong.split(",")[0],latlong.split(",")[1]);
        }
        catch (Exception e){
            e.printStackTrace();
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(BackgroundService.this);
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setSmallestDisplacement(10f);
            // Create LocationSettingsRequest object using location request
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(locationRequest);
            LocationSettingsRequest locationSettingsRequest = builder.build();

            // Check whether location settings are satisfied
            // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
            SettingsClient settingsClient = LocationServices.getSettingsClient(this);
            settingsClient.checkLocationSettings(locationSettingsRequest);

            Intent locationIntent = new Intent(getApplicationContext(), LocationReceiver.class);
            PendingIntent locationPendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    0,
                    locationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationPendingIntent);
            Log.d(TAG, "onConnected: started requesting locations");
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        
    }

    class mainTask extends TimerTask{

        @Override
        public void run() {
            handler.sendEmptyMessage(0);
            if(locString==null)
            {
                FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(BackgroundService.this);
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                            if(location!=null)
                                locString2 = new StringBuilder(""+location.getLatitude())
                                        .append(",")
                                        .append(location.getLongitude())
                                        .toString();
                        }

                );
            }
        }
    }

    static class IncomingHandler extends Handler
    {
        private final WeakReference<BackgroundService> mService;

        IncomingHandler(BackgroundService service) {
            mService = new WeakReference<BackgroundService>(service);
        }
        @Override
        public void handleMessage(Message msg)
        {
            BackgroundService service = mService.get();
            if(service!=null)
                service.handleMessage(msg);

        }
    }
}
