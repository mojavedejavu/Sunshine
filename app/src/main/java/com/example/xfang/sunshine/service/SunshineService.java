package com.example.xfang.sunshine.service;


import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.xfang.sunshine.R;
import com.example.xfang.sunshine.Utilities;
import com.example.xfang.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SunshineService extends IntentService{

    String LOG_TAG = SunshineService.class.getSimpleName();

    public SunshineService(){
        super(SunshineService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {


    }

    public static class FetchWeatherAlarmReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent sunshineServiceIntent = new Intent(context, SunshineService.class);
            context.startService(sunshineServiceIntent);
        }
    }

}
