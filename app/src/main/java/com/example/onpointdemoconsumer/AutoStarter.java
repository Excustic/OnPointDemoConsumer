package com.example.onpointdemoconsumer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class AutoStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d("AutoStarter", "onReceive: received");
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            Log.d("Autostarter", "onReceive: boot completed, starting service");
            Intent serviceIntent = new Intent(context, BackgroundService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
