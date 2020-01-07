package com.example.onpointdemoconsumer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

public class LocationService extends BroadcastReceiver {
    public static final String ACTION_PROCCESS_UPDATE = "com.example.onpointdemoconsumer.UPDATE_LOACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Intent mIntent = new Intent(context, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(mIntent);
            }
            Log.i("Autostart", "started");
        }
    }
}

