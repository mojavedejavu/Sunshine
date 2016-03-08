package com.xiaofanfang.apps.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.xiaofanfang.apps.sunshine.MainActivity;
import com.xiaofanfang.apps.sunshine.R;
import com.xiaofanfang.apps.sunshine.Utilities;
import com.xiaofanfang.apps.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/*
        The control flow

1. Your MainActivity is created and the sync adapter is initialized.
2. During initialization, getSyncAccount is called.
3. getSyncAccount will create a new account if no sunshine.example.com account exists.
If this is the case, onAccountCreated will be called.
4. onAccountCreated configures the periodic sync and calls for an immediate sync.
At this point, Sunshine will sync with the Open Weather API either every 3 hours
(if the build version is less than KitKat) or everyone 1 hour (if the build version
is greater than or equal to KitKat)

*/

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter{

    private final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    private final int NUM_DAYS = 7;

    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    private Context mContext;
    private ContentResolver mContentResolver;

    /*
        projection, column indices, and some constants for
        notification data
     */
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    //  reuse the notification ID to post no more than one notification.
    private static final int WEATHER_NOTIFICATION_ID = 3004;


    public SunshineSyncAdapter(Context context, boolean autoInitialize) {

        super(context, autoInitialize);
        mContext = context;
        mContentResolver = context.getContentResolver();

    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon){
        long rowId;
        Cursor cursor = mContentResolver.query(WeatherContract.LocationEntry.CONTENT_URI,
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
            Uri insertedUri = mContentResolver.insert(
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
                max = Utilities.formatTemp(mContext,tempObject.getString("max"), toImperial);
                min = Utilities.formatTemp(mContext,tempObject.getString("min"), toImperial);

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

            mContentResolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, forecasts);

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




    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.d(LOG_TAG, "Starting sync");

        int numDays = NUM_DAYS;
        String locationSetting = Utilities.getPreferredLocationSetting(mContext);

        // declare these outside of the try block so they can be closed
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

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

            // notification
            notifyWeather();
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

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }


    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);

        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    private void notifyWeather() {
        Context mContext = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String lastNotificationKey = mContext.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.
            String locationQuery = Utilities.getPreferredLocationSetting(mContext);

            Uri weatherUri = WeatherContract.WeatherEntry.buildUriWithLocationAndDate(locationQuery, System.currentTimeMillis());

            // we'll query our contentProvider, as always
            Cursor cursor = mContext.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

                int iconResourceId = Utilities.getArtResourceForWeatherCondition(weatherId);
                String title = mContext.getString(R.string.app_name);

                // Define the text of the forecast.
                boolean isImperial = Utilities.unitsPrefIsImperial(mContext);
                String contentText = String.format(mContext.getString(R.string.format_notification),
                        desc,
                        Utilities.formatTemp(mContext, high, isImperial),
                        Utilities.formatTemp(mContext, low, isImperial));

                //build notification here.
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
                mBuilder.setSmallIcon(iconResourceId)
                        .setContentTitle(mContext.getString(R.string.app_name))
                        .setContentText(contentText);
                Intent intent = new Intent(mContext, MainActivity.class);
                // build an artificial back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent to the top of the stack
                stackBuilder.addNextIntent(intent);

                PendingIntent pendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(pendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());


                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }
        }

    }

}
