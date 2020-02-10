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
import android.widget.AdapterView;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.flic.lib.FlicAppNotInstalledException;
import io.flic.lib.FlicBroadcastReceiverFlags;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicButtonCallback;
import io.flic.lib.FlicButtonCallbackFlags;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

public class MainActivity extends AppCompatActivity {
    private static MainActivity instance;

    public static long downloadID;
    public ImageButton btnPlayPause;
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

    private FlicManager manager;
    private boolean grabedFlicButton;


    public BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                populateFileSpinner();
                btnPlayPause.setEnabled(true);
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
        outState.putString("grabedFlicButton", Boolean.toString(grabedFlicButton));
        super.onSaveInstanceState(outState);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // Init FLIC
        FlicManager.setAppCredentials(
                "ad7c6ef1-e2fa-4855-8038-cf964938231c",
                "b740e9ae-6d03-49d0-9185-509a9c57c204",
                "GEMARA-FOR-DRIVERS"
        );


        // Init
        instance = this;
        LessonDownloadManager.ReadDefYomiSite(this, "populateMagids");
        mPrefs = getSharedPreferences("Gmara", 0);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        appPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if ( appPrefs.getBoolean("use_flic", false)) {
            if (savedInstanceState != null) {
                grabedFlicButton = Boolean.parseBoolean(savedInstanceState.getString("grabedFlicButton"));
            }
            if (!grabedFlicButton) {
                try {
                    FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
                        @Override
                        public void onInitialized(FlicManager manager) {
                            manager.initiateGrabButton(MainActivity.this);
                        }
                    });
                } catch (FlicAppNotInstalledException err) {
                    Toast.makeText(this, "Flic App is not installed", Toast.LENGTH_SHORT).show();
                }
            }
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.setTimeInMillis(System.currentTimeMillis());
        if (mediaPlayer==null) {
            mediaPlayer = new MediaPlayer();
        }
        btnPlayPause = (ImageButton)findViewById(R.id.btnPlayPause);
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

        populateFileSpinner();

        // Disable play button in case no files
        if (spinnerFile.getSelectedItem()==null ) {
            btnPlayPause.setEnabled(false);
        }

        if (savedInstanceState != null) { ;
            doPlay = Boolean.parseBoolean(savedInstanceState.getString("doPlay"));
            oneTimeOnly = 0;
            if (doPlay){
                PrepareAudio();
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
                    PrepareAudio();
                    PlayAudio(null);
                }
            }

        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PressOnForward();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PressOnBackward();
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PressOnPlayPause(btnPlayPause);
            }
        });


    }

    public void PrepareAudio() {
        boolean shouldPlay = false;
        String lastPlayedFile = mPrefs.getString("playedFile", "NoSaved");

        try {
            if (mediaPlayer == null)
                mediaPlayer = new MediaPlayer();

            if (shouldRunTfilatHaderech()) {
                Uri mediaPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.tfilat_haderech);
                playingFile = mediaPath.toString();
                mediaPlayer.setDataSource(getApplicationContext(), mediaPath);
            }
            else {

                // in case of swich lesson during play - reset the lesson.
                if (mediaPlayer.isPlaying()) {
                    shouldPlay = true;

                }
                mediaPlayer.reset();
                mediaPlayer.setDataSource(playingFile);
            }
            mediaPlayer.prepare();
        } catch (Exception e)  {
            Log.e("Gmara", "media Player preperation failed");
        }

        if (lastPlayedFile.contains(playingFile)) {
            String lastPlayedTime = mPrefs.getString("currentPlayedTime", "0");
            mediaPlayer.seekTo(Integer.parseInt(lastPlayedTime));
        }

        finalTime = mediaPlayer.getDuration();
        seekbar.setMax((int) finalTime);

        if (shouldPlay) {
            PlayAudio(null);
        }

    }

    public  void PressOnPlayPause(ImageButton btnPlayPause) {
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

    public void PressOnBackward() {
        int temp = (int)startTime;

        if((temp-backwardTime)>0){
            startTime = startTime - backwardTime;
            mediaPlayer.seekTo((int) startTime);
            // Toast.makeText(getApplicationContext(),"You have Jumped backward 30 seconds",Toast.LENGTH_SHORT).show();
        }else{
            // Toast.makeText(getApplicationContext(),"Cannot jump backward 30seconds",Toast.LENGTH_SHORT).show();
        }
    }

    public void PressOnForward() {
        int temp = (int)startTime;

        if((temp+forwardTime)<=finalTime){
            startTime = startTime + forwardTime;
            mediaPlayer.seekTo((int) startTime);
            // Toast.makeText(getApplicationContext(),"You have Jumped forward 30 seconds",Toast.LENGTH_SHORT).show();
        }else{
            // Toast.makeText(getApplicationContext(),"Cannot jump forward 30 seconds",Toast.LENGTH_SHORT).show();
        }
    }

    private void populateFileSpinner() {
        spinnerFile = (Spinner) findViewById(R.id.spinnerPlayFile);
        List<String> list = new ArrayList<String>();
        final File[] filesList = this.getExternalFilesDir("").listFiles();

        Arrays.sort(filesList, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return -1 * Long.compare(f1.lastModified(), f2.lastModified());
            }
        });

        for (File file : filesList) {
            list.add(file.getName());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFile.setAdapter(dataAdapter);

        // Here assign the audio file
        if (filesList.length > 0)
            playingFile = filesList[0].toString();
        else
            playingFile = "";

        spinnerFile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                playingFile = filesList[i].toString();
                try {
                    PrepareAudio();
                } catch (Exception e)  {
                    Log.e("Gmara", "media Player preperation failed");
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });
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
                return;
            }
        }
        if (currentPlayedTime > 0) {
            mEditor.putString("currentPlayedTime", "" + currentPlayedTime).commit();
            mEditor.putString("playedFile", playingFile).commit();
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

    public void grabButton(View v) {
        if (manager != null) {
            manager.initiateGrabButton(this);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
                if (button != null) {
                    button.registerListenForBroadcast(FlicBroadcastReceiverFlags.CLICK_OR_DOUBLE_CLICK_OR_HOLD | FlicBroadcastReceiverFlags.REMOVED);
                    Toast.makeText(MainActivity.this, "Grabbed a button", Toast.LENGTH_SHORT).show();
                    grabedFlicButton = true;
                } else {
                    Toast.makeText(MainActivity.this, "Did not grab any button", Toast.LENGTH_SHORT).show();
                    grabedFlicButton = false;
                }
            }
        });
    }

    public static MainActivity getInstance() {
        return instance;
    }


}
