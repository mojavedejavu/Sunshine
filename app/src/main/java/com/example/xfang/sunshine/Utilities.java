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

    public static boolean unitsPrefIsImperial(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String unitsPref = sp.getString(
                context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_defaultValue));

        boolean isImperial = unitsPref.equals(context.getString(R.string.pref_units_imperial_key));

        return isImperial;
    }


    public static String formatWind(Context context, float windSpeed, float degrees) {
        int windFormat;

        if (unitsPrefIsImperial(context)) {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        } else {
            windFormat = R.string.format_wind_kmh;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }
}
