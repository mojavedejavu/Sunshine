package com.example.xfang.sunshine;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

public class Utilities {

    public static String getReadableDate(long time){
        SimpleDateFormat dayFormat = new SimpleDateFormat("MMM dd, EEE");
        return dayFormat.format(time);
    }

    public static String formatTemp(String input, boolean toImperial){
        double output = new Double(input);
        if (toImperial) {
            output = output * 1.8 + 32;
        }
        return String.format("%.1f", output);

    }
}
