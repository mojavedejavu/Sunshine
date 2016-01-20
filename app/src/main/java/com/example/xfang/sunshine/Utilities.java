package com.example.xfang.sunshine;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utilities {

    public static String getReadableDate(long dateInMilliseconds){
        SimpleDateFormat dayFormat = new SimpleDateFormat("MMM dd, EEE");
        return dayFormat.format(dateInMilliseconds);
    }

    public static String formatTemp(String input, boolean toImperial){
        double output = new Double(input);
        if (toImperial) {
            output = output * 1.8 + 32;
        }
        return String.format("%.1f", output);

    }
}
