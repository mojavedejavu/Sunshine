package com.example.xfang.sunshine;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.xfang.sunshine.data.WeatherContract;
import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    final int NUM_DAYS = 7;

    Context mContext;
    ArrayAdapter<String> mAdapter;

    private long addLocation(String locationSetting, String cityName, double lat, double lon){
        long rowId;
        Cursor cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI,
                null,
                LocationEntry.COLUMN_LOCATION_SETTING + " =? ",
                new String[]{cityName},
                null,
                null);
        if (cursor.moveToFirst()){
            int id = cursor.getColumnIndex(LocationEntry._ID);
            rowId = cursor.getLong(id);
        }
        else{
            ContentValues locationValues = new ContentValues();
            locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(LocationEntry.COLUMN_COORD_LONG, lon);
            locationValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
            Uri insertedUri = mContext.getContentResolver().insert(
                    LocationEntry.CONTENT_URI,
                    locationValues);
            rowId = ContentUris.parseId(insertedUri);
        }

        cursor.close();
        return rowId;
    }

    private String[] getWeatherDataFromJson(String jsonString, String locationSetting, int numDays, boolean toImperial) {
        String LOG_TAG = "getWeatherDataFromJson";
        String[] result = new String[numDays];
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            JSONArray array = jsonObject.getJSONArray("list");

            JSONObject city = jsonObject.getJSONObject("city");
            String cityName = city.getString("name");
            JSONObject cityCoord = city.getJSONObject("coord");
            double cityLongitude = cityCoord.getDouble("lon");
            double cityLatitude = cityCoord.getDouble("lat");

            Log.d(LOG_TAG, "Parsed city. Name: " + cityName + ", longitude: " + cityLongitude + ", latitude " +
                    cityLatitude);

            Time time = new Time();
            time.setToNow();
            int firstDayJulian = time.getJulianDay(time.toMillis(true), time.gmtoff);

            for(int i = 0; i < array.length(); i++) {
                JSONObject daily = (JSONObject) array.get(i);
                JSONObject tempObject = daily.getJSONObject("temp");

                String max = Utilities.formatTemp(tempObject.getString("max"), toImperial);
                String min = Utilities.formatTemp(tempObject.getString("min"), toImperial);

                JSONObject weatherObject = (JSONObject) daily.getJSONArray("weather").get(0);
                String description = weatherObject.getString("main");

                String day = Utilities.getReadableDate(new Time().setJulianDay(firstDayJulian + i));

                String dailyString = day + "   " + min + " / " + max + ", " + description;
                result[i] = dailyString;
                Log.d(LOG_TAG, dailyString);
            }

        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSON parsing error: " + e);
        }

        return result;
    }

    private URL buildQueryURL(String locationSetting, int numDays){
        String format = "json";
        String unit = "metric"; // always query in metric, convert later if needed

        final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String QUERY_PARAM = "q";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String API_PARAM = "APPID";

        URL returnUrl = null;

        try {
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon().
                    appendQueryParameter(QUERY_PARAM, locationSetting).
                    appendQueryParameter(FORMAT_PARAM, format).
                    appendQueryParameter(UNITS_PARAM, unit).
                    appendQueryParameter(DAYS_PARAM, String.valueOf(numDays)).
                    appendQueryParameter(API_PARAM, mContext.getString(R.string.WeatherAPIKey)).build();

            returnUrl = new URL(builtUri.toString());
        } catch( MalformedURLException e){
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "Built URL: " + returnUrl);
        return returnUrl;
    }

    private boolean unitsPrefIsImperial(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitsPref = sp.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_defaultValue));

        boolean isImperial = unitsPref.equals(mContext.getString(R.string.pref_units_imperial_key));

        return isImperial;
    }

    public FetchWeatherTask(Context context, ArrayAdapter<String> adapter){
        mContext = context;
        mAdapter = adapter;
    }

    @Override
    protected String[] doInBackground(String... params){
        // declare these outside of the try block so they can be closed
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        int numDays = NUM_DAYS;

        String locationSetting = params[0];

        try {
            // build query URL and make urlConnection
            URL url = buildQueryURL(locationSetting, numDays);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // read the query response
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null){
                buffer.append(line + "\n");
            }
            Log.d(LOG_TAG, buffer.toString());

            // parse the response
            String[] parsedForecasts = getWeatherDataFromJson(buffer.toString(), locationSetting, numDays, unitsPrefIsImperial());
            return parsedForecasts;
        }
        catch(IOException e){
            Log.e(LOG_TAG, "Error ", e);
            e.printStackTrace();
            return null;
        }

        finally{
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            if (reader != null){
                try {
                    reader.close();
                }
                catch(IOException e){
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    @Override
    protected void onPostExecute(String[] array){
        if (array != null) {
            mAdapter.clear();
            mAdapter.addAll(array);
        }

    }
}