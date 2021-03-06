package com.example.gmara;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Toast.makeText(context.getApplicationContext(), "Triggered just now", Toast.LENGTH_LONG).show();
        Log.i("Gmara", "Start MyBroadCastReceiver.onReceive routine.");
        LessonDownloadManager.ReadDefYomiSite(context, "DownloadLastLesson");
        LessonDownloadManager.DeleteOldFile(context);
    }
}

