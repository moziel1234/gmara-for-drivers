package com.example.gmara;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.DOWNLOAD_SERVICE;
// import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

public class LessonDownloadManager {



    public static void ReadDefYomiSite(final Context context, final String action) {

        final RequestQueue queue = Volley.newRequestQueue(context);
        final String url ="http://daf-yomi.com/Media.aspx?menu=1";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        final String url2 = GetDataUrlFromHtml(response);
                        StringRequest stringRequest2 = new StringRequest(Request.Method.GET, url2,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        if (action.equals("DownloadLastLesson")) {
                                            DownloadLesson(response, context);
                                        } else if (action.equals("populateMagids")){
                                            PopulateMagids(response);
                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e("Gmara", "Request " + url2 + "returned with error!");
                                Log.e("Gmara", error.toString());
                            }
                        });

                        queue.add(stringRequest2);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Gmara", "Request " + url + "returned with error!");
                Log.e("Gmara", error.toString());
            }
        });

        queue.add(stringRequest);
    }

    private static void PopulateMagids(String response) {
        List<String> listMagidNames = new ArrayList<String>();
        List<String> listMagidVals = new ArrayList<String>();

        try {
            JSONArray arrayJ = new JSONArray(response);

            for (int i = 0; i < arrayJ.length(); i++) {
                JSONObject obj = arrayJ.getJSONObject(i);
                String mpType = obj.getString("e");
                String magidName = obj.getString("ma");
                String vals = obj.getString("k").split("/")[4];
                if (mpType.equals("mp3")) {
                    listMagidNames.add(magidName);
                    listMagidVals.add(vals);
                }
            }

        } catch (Exception t) {
            Log.e("Gmara", "Could not parse malformed JSON: \"" + response + "\"");
        }
        SettingsActivity.entries =  listMagidNames.toArray(new CharSequence[listMagidNames.size()]);
        SettingsActivity.entryValues =  listMagidVals.toArray(new CharSequence[listMagidVals.size()]);
    }

    private static String GetDataUrlFromHtml(String response) {
        String[] resList = response.split("\n");
        List<String> valRes = new ArrayList<String>();
        Pattern pattern = Pattern.compile("(?<=value=\")(\\d+)(?=\")");
        for (int i = 0; i < resList.length; i++) {
            if (resList[i].contains("elected")) {
                Matcher matcher = pattern.matcher(resList[i]);
                while (matcher.find()) {
                    valRes.add(matcher.group(1));
                }
            }
            if (valRes.size() >= 2) {
                break;
            }
        }
        String massechet = valRes.get(0);
        String daf = valRes.get(1);
        return String.format("http://daf-yomi.com/AjaxHandler.ashx?medialist=1&page=1&massechet=%s&medaf=%s&addaf=%s",
                massechet, daf, daf);
    }

    public static void DownloadLesson(String response, Context context) {
        try {
            JSONArray arrayJ = new JSONArray(response);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String magidName = sp.getString("magid_name","navon");
            Boolean foundLesson = false;
            for (int i = 0; i < arrayJ.length(); i++) {
                JSONObject obj = arrayJ.getJSONObject(i);
                String mpType = obj.getString("e");
                String downloadUrl = obj.getString("k");
                if (mpType.equals("mp3") && downloadUrl.contains(magidName)) {
                    handleDownload(context, downloadUrl, magidName);
                    foundLesson = true;
                    break;
                }
            }

            if (!foundLesson) {
                Toast.makeText(context.getApplicationContext(), "Couldn't find a Magid", Toast.LENGTH_LONG).show();
            }

        } catch (Exception t) {
            Log.e("Gmara", "Could not parse malformed JSON: \"" + response + "\"");
        }
    }

    public static void handleDownload(Context context, String url, String magidNane) {
        Log.i("Gmara", "Start handleDownload routine.");
        String[] arr = url.split("/");
        String nameOfFile = (arr[arr.length - 1]).replace(".","-" + magidNane+ ".");
        if (new File(context.getExternalFilesDir("") + "/" + nameOfFile).exists() == false) {
            Log.i("Gmara", "Going to download: " + url);
            File file = new File(context.getExternalFilesDir(null), nameOfFile);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(nameOfFile)// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                    .setDestinationUri(Uri.fromFile(file))// Uri of the destination file
                    .setRequiresCharging(false)// Set if charging is required to begin the download
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);// Set if download is allowed on roaming network
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            MainActivity.downloadID = downloadManager.enqueue(request);// enqueue puts the download request in the queue.
        }
        else {
            Toast.makeText(context.getApplicationContext(), "Lesson already exist", Toast.LENGTH_LONG).show();
        }


    }

    public static void DeleteOldFile(Context context){
        Log.i("Gmara", "Start DeleteOldFile routine.");
        Calendar time = Calendar.getInstance();
        time.add(Calendar.DAY_OF_YEAR,-7); //week
        File[] filesList = context.getExternalFilesDir("").listFiles();
        for (File file : filesList) {
            Date lastModified = new Date(file.lastModified());
            if(lastModified.before(time.getTime())) {
                //file is older than a week
                Log.i("Gmara", "Going to delete: " +file.getName());
                file.delete();
            }
        }
    }

}
