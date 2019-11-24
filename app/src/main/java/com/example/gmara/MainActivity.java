package com.example.gmara;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import java.io.File;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.FileFilter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    public static long downloadID;
    private PendingIntent pendingIntent;
    private File lastModifiedFile;
    private MediaPlayer mediaPlayer;
    private boolean doPlay = false;

    public BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.setTimeInMillis(System.currentTimeMillis());
        mediaPlayer = new MediaPlayer();
        final Button btnPlayPause = (Button)findViewById(R.id.btnPlayPause);


        // Set Download time to 2 AM
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 2);
        midnightCalendar.set(Calendar.MINUTE, 0);
        midnightCalendar.set(Calendar.SECOND, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(MainActivity.this, MyBroadCastReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, midnightCalendar.getTimeInMillis(),
                1000 * 60 * 60 * 24, pendingIntent);

        // Init player with last file
        lastModifiedFile = lastFileModified();
        TextView playLabel = (TextView)findViewById(R.id.textViewPlayFile);
        String[] arr = lastModifiedFile.toString().split("/");
        String nameOfFile = arr[arr.length - 1];
        playLabel.setText(nameOfFile);
        try {
            mediaPlayer.setDataSource(lastModifiedFile.toString());
            mediaPlayer.prepare();
        } catch (Exception e)  {
            // TODO: handle error
        }

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!doPlay) {
                    btnPlayPause.setText("||");
                    Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
                    mediaPlayer.start();
                    doPlay = true;
                }
                else {
                    btnPlayPause.setText(">");
                    Toast.makeText(getApplicationContext(), "Pause sound", Toast.LENGTH_SHORT).show();
                    mediaPlayer.pause();
                    doPlay = false;
                }
            }
        });


    }

    public File lastFileModified() {
        File fl = new File(MainActivity.this.getExternalFilesDir("").toString());
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Start when click on the button
    public void DownloadLastLesson(View view) {
        LessonDownloadManager.DownloadLessson(this);
    }




}
