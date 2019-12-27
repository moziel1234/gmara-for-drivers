package com.example.gmara;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import java.io.File;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public static long downloadID;
    private PendingIntent pendingIntent;
    private File lastModifiedFile;
    private MediaPlayer mediaPlayer;
    private boolean doPlay = false;

    private int forwardTime = 30 * 1000;
    private int backwardTime = 30 * 1000;
    private double startTime = 0;
    private double finalTime = 0;

    public static int oneTimeOnly = 0;
    private Handler myHandler = new Handler();
    private SeekBar seekbar;
    private TextView leftTime, rightTime;
    private SharedPreferences mPrefs;
    private SharedPreferences appPrefs;
    private String playedFile;
    private String playingFile;

    private Spinner spinnerFile;

    public BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                populateFileSpinner();
                Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("leftTime", leftTime.getText().toString());
        outState.putString("rightTime", rightTime.getText().toString());
        outState.putString("duration", mediaPlayer.getDuration()+"");
        outState.putString("doPlay", Boolean.toString(doPlay));
        super.onSaveInstanceState(outState);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init
        LessonDownloadManager.ReadDefYomiSite(this, "populateMagids");
        mPrefs = getSharedPreferences("Gmara", 0);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        appPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.setTimeInMillis(System.currentTimeMillis());
        if (mediaPlayer==null) {
            mediaPlayer = new MediaPlayer();
        }
        final ImageButton btnPlayPause = (ImageButton)findViewById(R.id.btnPlayPause);
        final ImageButton btnForward = (ImageButton)findViewById(R.id.btnForward);
        final ImageButton btnBack = (ImageButton)findViewById(R.id.btnBack);

        leftTime = (TextView)findViewById(R.id.textViewLest);
        rightTime = (TextView)findViewById(R.id.textViewRight);
        seekbar = (SeekBar)findViewById(R.id.seekBar);



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
        if (lastModifiedFile != null) {
            // TextView playLabel = (TextView) findViewById(R.id.spinnerPlayFile);
            String[] arr = lastModifiedFile.toString().split("/");
            playedFile = arr[arr.length - 1];
            // playLabel.setText(playedFile);
        }

        String lastPlayedFile = mPrefs.getString("playedFile", "NoSaved");



        try {
            if (!shouldRunTfilatHaderech()) {
                playingFile = lastModifiedFile.toString();
                mediaPlayer.setDataSource(playingFile);
            } else {
                Uri mediaPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tfilat_haderech);
                playingFile = mediaPath.toString();
                mediaPlayer.setDataSource(getApplicationContext(), mediaPath);
            }

            mediaPlayer.prepare();
        } catch (Exception e)  {
            Log.e("Gmara", "media Player preperation failed");
        }

        if (lastPlayedFile.equals(playedFile)) {
            String lastPlayedTime = mPrefs.getString("currentPlayedTime", "0");
            mediaPlayer.seekTo(Integer.parseInt(lastPlayedTime));
        }

        if (savedInstanceState != null) { ;
            doPlay = Boolean.parseBoolean(savedInstanceState.getString("doPlay"));
            oneTimeOnly = 0;
            if (doPlay){
                PlayAudio(btnPlayPause);
            } else {
                leftTime.setText(savedInstanceState.getString("leftTime"));
                rightTime.setText(savedInstanceState.getString("rightTime"));
            }

        }


        seekbar.setClickable(false);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                if (playingFile != null && playingFile.contains("resource")) { //tfilat haderech
                    lastModifiedFile = lastFileModified();
                    playingFile = lastModifiedFile.toString();
                    try {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(playingFile);
                        mediaPlayer.prepare();
                        finalTime = mediaPlayer.getDuration();
                        seekbar.setMax((int) finalTime);
                    } catch (Exception ex) {
                        Log.e("Gmara", ex.toString());
                    }

                    PlayAudio(null);
                }
            }

        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp = (int)startTime;

                if((temp+forwardTime)<=finalTime){
                    startTime = startTime + forwardTime;
                    mediaPlayer.seekTo((int) startTime);
                    // Toast.makeText(getApplicationContext(),"You have Jumped forward 30 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    // Toast.makeText(getApplicationContext(),"Cannot jump forward 30 seconds",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp = (int)startTime;

                if((temp-backwardTime)>0){
                    startTime = startTime - backwardTime;
                    mediaPlayer.seekTo((int) startTime);
                    // Toast.makeText(getApplicationContext(),"You have Jumped backward 30 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    // Toast.makeText(getApplicationContext(),"Cannot jump backward 30seconds",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!doPlay) {
                    PlayAudio(btnPlayPause);
                }
                else {
                    btnPlayPause.setImageResource(R.drawable.play);
                    Toast.makeText(getApplicationContext(), "Pause sound", Toast.LENGTH_SHORT).show();
                    mediaPlayer.pause();
                    doPlay = false;
                    CommitToPrefs(false);
                }
            }
        });

        populateFileSpinner();
    }

    private void populateFileSpinner() {
        spinnerFile = (Spinner) findViewById(R.id.spinnerPlayFile);
        List<String> list = new ArrayList<String>();
        File[] filesList = this.getExternalFilesDir("").listFiles();
        for (File file : filesList) {
            list.add(file.getName());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFile.setAdapter(dataAdapter);
    }

    private boolean shouldRunTfilatHaderech() {
        if ( appPrefs.getBoolean("tfilat_haderech", false)) {
            if (!mPrefs.contains("lastTfila"))
                return true;
            else {
                if (!DateUtils.isToday(Long.parseLong(mPrefs.getString("lastTfila", "0"))))
                    return true;
                else
                    return false;

            }

        }
        else {
            return false;
        }
    }

    private void PlayAudio(ImageButton btnPlayPause) {
        if (playingFile.contains("resource"))
            mPrefs.edit().putString("lastTfila", Calendar.getInstance().getTimeInMillis() +"").commit();
        if (btnPlayPause != null)
            btnPlayPause.setImageResource(R.drawable.pause);
        Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
        mediaPlayer.start();
        doPlay = true;
        finalTime = mediaPlayer.getDuration();
        startTime = mediaPlayer.getCurrentPosition();

        if (oneTimeOnly == 0) {
            seekbar.setMax((int) finalTime);
            oneTimeOnly = 1;
        }

        seekbar.setProgress((int)startTime);
        myHandler.postDelayed(UpdateSongTime,100);

        leftTime.setText(String.format("%s:%s",
                minTwoDigits(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)),
                minTwoDigits(TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                finalTime))))
        );

        rightTime.setText(String.format("%s:%s",
                minTwoDigits(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)),
                minTwoDigits(TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                finalTime))))
        );
    }



    private String minTwoDigits(long n) {
        if (n<10) {
            return "0"+n;
        } else {
            return ""+n;
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            if (mediaPlayer != null ) {
                startTime = mediaPlayer.getCurrentPosition();

                leftTime.setText(String.format("%s:%s",
                        minTwoDigits(TimeUnit.MILLISECONDS.toMinutes((long) startTime)),
                        minTwoDigits(TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                        toMinutes((long) startTime))))
                );


                seekbar.setProgress((int) startTime);
                myHandler.postDelayed(this, 100);
            }
        }
    };

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

        CommitToPrefs(true);


        unregisterReceiver(onDownloadComplete);
        mediaPlayer.release();
        mediaPlayer = null;

        if (isFinishing()) {

        }

    }

    private void CommitToPrefs(Boolean onDestroy) {
        long currentPlayedTime = TimeUnit.MILLISECONDS.toMillis((long) startTime);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        if (onDestroy) {
            if (playingFile != null && playingFile.contains("resource")) { //tfilat haderech
                mEditor.putString("currentPlayedTime", "" + 0).commit();
                if (currentPlayedTime > 0) {
                    mEditor.putString("playedFile", playedFile).commit();
                }
            }
        } else {
            if (currentPlayedTime > 0) {
                mEditor.putString("currentPlayedTime", "" + currentPlayedTime).commit();
                mEditor.putString("playedFile", playedFile).commit();
            }
        }
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
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Start when click on the button
    public void DebugdeleteTfila(View view) {
        if (mPrefs.contains("lastTfila")) {
            mPrefs.edit().remove("lastTfila").commit();
        }
    }


    // Start when click on the button
    public void DownloadLastLesson(View view) {
        LessonDownloadManager.ReadDefYomiSite(this, "DownloadLastLesson");
    }




}
