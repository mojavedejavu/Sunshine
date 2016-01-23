package com.example.xfang.sunshine;

import android.content.Context;
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

    public static String formatMillisecondsToReadableDate(long dateInMilliseconds){
        SimpleDateFormat dayFormat = new SimpleDateFormat("MMM dd, EEE");
        return dayFormat.format(dateInMilliseconds);
    }

    public static String formatTemp(Context context, String input, boolean toImperial){
        double output = new Double(input);
        if (toImperial) {
            output = output * 1.8 + 32;
        }
        return String.format(context.getString(R.string.format_temperature), output);

    }

    public static String getPreferredLocationSetting(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String locationString = sp.getString(
                context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_defaultValue));
        return locationString;
    }
}
