package com.example.onpointdemoconsumer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;

public class LocationReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationReceiver";
    private SharedPreferences sp;
    private SharedPreferences.Editor edit;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Receive an intent"+intent.getDataString());
        if(intent!=null){
            LocationResult result = LocationResult.extractResult(intent);

            if(result!=null){
                Log.d(TAG, "onReceive: "+result.getLastLocation().getLatitude()+","+result.getLastLocation().getLongitude());
                sp = PreferenceManager.getDefaultSharedPreferences(context);
                edit = sp.edit();
                Location location = result.getLastLocation();
                String locString = "" + location.getLatitude() +
                        "," +
                        location.getLongitude();
                edit.putString("locString",locString);
                edit.apply();
            }
        }

    }
}
