package com.example.xfang.sunshine;

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
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, MMM dd");
        return dayFormat.format(time);
    }

    public static String[] getWeatherDataFromJson(String jsonString, int numDays) {
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
                String max = tempObject.getString("max");
                String min = tempObject.getString("min");

                JSONObject weatherObject = (JSONObject) daily.getJSONArray("weather").get(0);
                String description = weatherObject.getString("main");

                String day = getReadableDate(new Time().setJulianDay(firstDayJulian + i));

                String dailyString = day + " " + min + " / " + max + ", " + description;
                result[i] = dailyString;
                Log.d(LOG_TAG, dailyString);
            }

        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSON parsing error: " + e);
        }

        return result;
    }
}
