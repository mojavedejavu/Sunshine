package com.example.xfang.sunshine;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

/**
 * Created by xfang on 12/24/15.
 */
public class Utilities {

    private static String getReadableDate(long time){
        SimpleDateFormat dayFormat = new SimpleDateFormat("MMM dd, EEE");
        return dayFormat.format(time);
    }

    private static String formatTemp(String input, boolean toImperial){
        double output = new Double(input);
        if (toImperial) {
            output = output * 1.8 + 32;
        }
        return String.format("%.1f", output);

    }

    public static String[] getWeatherDataFromJson(String jsonString, int numDays, boolean toImperial) {
        String LOG_TAG = "getWeatherDataFromJson";
        String[] result = new String[numDays];
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray array = jsonObject.getJSONArray("list");

            Time time = new Time();
            time.setToNow();
            int firstDayJulian = time.getJulianDay(time.toMillis(true), time.gmtoff);

            for(int i = 0; i < array.length(); i++) {
                JSONObject daily = (JSONObject) array.get(i);
                JSONObject tempObject = daily.getJSONObject("temp");

                String max = formatTemp(tempObject.getString("max"), toImperial);
                String min = formatTemp(tempObject.getString("min"), toImperial);

                JSONObject weatherObject = (JSONObject) daily.getJSONArray("weather").get(0);
                String description = weatherObject.getString("main");

                String day = getReadableDate(new Time().setJulianDay(firstDayJulian + i));

                String dailyString = day + "   " + min + " / " + max + ", " + description;
                result[i] = dailyString;
                Log.d(LOG_TAG, dailyString);
            }

        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSON parsing error: " + e);
        }

        return result;
    }
}
