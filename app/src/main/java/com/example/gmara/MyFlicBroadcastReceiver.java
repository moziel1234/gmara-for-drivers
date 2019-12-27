package com.example.gmara;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import io.flic.lib.FlicBroadcastReceiver;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;


public class MyFlicBroadcastReceiver extends FlicBroadcastReceiver {
    @Override
    protected void onRequestAppCredentials(Context context) {
        FlicManager.setAppCredentials(
                "ad7c6ef1-e2fa-4855-8038-cf964938231c",
                "b740e9ae-6d03-49d0-9185-509a9c57c204",
                "GEMARA-FOR-DRIVERS"
        );
    }

    @Override
    public void onButtonUpOrDown(Context context, FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
        if (isUp) {
            Toast.makeText(context.getApplicationContext(), "Botton UP", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context.getApplicationContext(), "Botton Down", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onButtonRemoved(Context context, FlicButton button) {
        // Button was removed
    }
}

