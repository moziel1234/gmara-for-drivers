package com.example.gmara;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;

public class MyBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Toast.makeText(context.getApplicationContext(), "Triggered just now", Toast.LENGTH_LONG).show();
        LessonDownloadManager.DownloadLessson(context);
        LessonDownloadManager.DeleteOldFile(context);
    }
}

