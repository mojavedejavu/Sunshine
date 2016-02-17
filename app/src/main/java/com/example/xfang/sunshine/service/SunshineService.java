package com.example.xfang.sunshine.service;


import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
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
    final int NUM_DAYS = 7;

    public SunshineService(){
        super(SunshineService.class.getSimpleName());
    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon){
        long rowId;
        Cursor cursor = this.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                null,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " =? ",
                new String[]{locationSetting},
                null,
                null);
        if (cursor.moveToFirst()){
            int id = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            rowId = cursor.getLong(id);
        }
        else{
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            Uri insertedUri = this.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues);
            rowId = ContentUris.parseId(insertedUri);
        }

        cursor.close();
        return rowId;
    }

    private void parseFromJsonAndBulkInsert(String jsonString, String locationSetting, int numDays, boolean toImperial) {
        String LOG_TAG = "parseFromJsonAndBulkInsert";
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

            // add location setting to contentProvider
            long locationRowId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            ContentValues[] forecasts = new ContentValues[array.length()];

            for(int i = 0; i < array.length(); i++) {
                long dateInMillisecondsAndNormalized;
                String max;
                String min;
                String description;
                String weatherId;
                String humidity;
                String pressure;
                String windSpeed;
                String windDirection;

                JSONObject daily = (JSONObject) array.get(i);

                // date, humidity, pressure, windSpeed, windDirection
                long dateInSeconds = daily.getLong("dt");
                dateInMillisecondsAndNormalized = WeatherContract.normalizeDate(dateInSeconds * 1000);
                humidity = daily.getString("humidity");
                pressure = daily.getString("pressure");
                windSpeed = daily.getString("speed");
                windDirection = daily.getString("deg");

                // min and max temp
                JSONObject tempObject = daily.getJSONObject("temp");
                max = Utilities.formatTemp(this,tempObject.getString("max"), toImperial);
                min = Utilities.formatTemp(this,tempObject.getString("min"), toImperial);

                // desc and weatherId
                JSONObject weatherObject = (JSONObject) daily.getJSONArray("weather").get(0);
                description = weatherObject.getString("main");
                weatherId = weatherObject.getString("id");

                ContentValues forecast = new ContentValues();
                forecast.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateInMillisecondsAndNormalized);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, max);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, min);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                forecast.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                forecasts[i] = forecast;
            }

            this.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, forecasts);

        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSON parsing error: " + e);
        }

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
                    appendQueryParameter(API_PARAM, this.getString(R.string.WeatherAPIKey)).build();

            returnUrl = new URL(builtUri.toString());
        } catch( MalformedURLException e){
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "Built URL: " + returnUrl);
        return returnUrl;
    }

    private boolean unitsPrefIsImperial(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String unitsPref = sp.getString(
                this.getString(R.string.pref_units_key),
                this.getString(R.string.pref_units_defaultValue));

        boolean isImperial = unitsPref.equals(this.getString(R.string.pref_units_imperial_key));

        return isImperial;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // declare these outside of the try block so they can be closed
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        int numDays = NUM_DAYS;

        String locationSetting = Utilities.getPreferredLocationSetting(this);

        try {
            // build query URL and make urlConnection
            URL url = buildQueryURL(locationSetting, numDays);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // get the query response
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null){
                buffer.append(line + "\n");
            }
            Log.d(LOG_TAG, buffer.toString());

            // parse the response
            parseFromJsonAndBulkInsert(buffer.toString(), locationSetting, numDays, unitsPrefIsImperial());
        }
        catch(IOException e){
            Log.e(LOG_TAG, "Error ", e);
            e.printStackTrace();
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

}
