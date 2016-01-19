package com.example.xfang.sunshine.data;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

import junit.framework.Test;


public class TestProvider extends AndroidTestCase{

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    public static final String TEST_LOCATION = "Paris, France";
    public static final long TEST_DATE = 1419033600L;  // December 20th, 2014

    public static final Uri TEST_URI_WEATHER = WeatherEntry.CONTENT_URI;
    public static final Uri TEST_URI_LOCATION = LocationEntry.CONTENT_URI;
    public static final Uri TEST_URI_WEATHER_WITH_LOCATION =
            WeatherEntry.buildUriWithLocation(TEST_LOCATION);
    public static final Uri TEST_URI_WEATHER_WITH_LOCATION_AND_DATE =
            WeatherEntry.buildUriWithLocationAndDate(TEST_LOCATION, TEST_DATE);

    @Override
    public void setUp(){
        deleteAllRecordsFromDB();
    }

    public void deleteAllRecordsFromDB(){
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();

        db.delete(LocationEntry.TABLE_NAME, null, null);
        db.delete(WeatherEntry.TABLE_NAME, null, null);
        db.close();
    }

    public void testUriMatcher(){
        UriMatcher matcher = WeatherProvider.buildUriMatcher();

        assertEquals(matcher.match(TEST_URI_WEATHER),
                WeatherProvider.WEATHER);
        assertEquals(matcher.match(TEST_URI_LOCATION),
                WeatherProvider.LOCATION);
        assertEquals(matcher.match(TEST_URI_WEATHER_WITH_LOCATION),
                WeatherProvider.WEATHER_WITH_LOCATION);
        assertEquals(matcher.match(TEST_URI_WEATHER_WITH_LOCATION_AND_DATE),
                WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE);
    }

    public void testProviderRegistry(){
        PackageManager pm = mContext.getPackageManager();

        ComponentName componentName = new ComponentName(mContext.getPackageName(), WeatherProvider.class.getName());
        Log.d(LOG_TAG, "componentName is: " + componentName);

        try {
            ProviderInfo info = pm.getProviderInfo(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

            String foundAuthority = info.authority;
            assertEquals(foundAuthority, WeatherContract.AUTHORITY);

        }catch(PackageManager.NameNotFoundException e){
            assertTrue("Error: WeatherProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    public void testGetType(){

        ContentResolver resolver = mContext.getContentResolver();

        String type = resolver.getType(TEST_URI_WEATHER);
        assertEquals(type, WeatherEntry.CONTENT_TYPE_DIR);

        type = resolver.getType(TEST_URI_LOCATION);
        assertEquals(type, LocationEntry.CONTENT_TYPE_DIR);

        type = resolver.getType(TEST_URI_WEATHER_WITH_LOCATION);
        assertEquals(type, WeatherEntry.CONTENT_TYPE_DIR);

        type = resolver.getType(TEST_URI_WEATHER_WITH_LOCATION_AND_DATE);
        assertEquals(type, WeatherEntry.CONTENT_TYPE_ITEM);

    }

    public void testBasicLocationQueries(){

        long rowId = TestUtilities.insertNorthPoleLocationValues(mContext);

        // test basic location query
        Cursor c = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        ContentValues northPole = TestUtilities.createNorthPoleLocationValues();
        TestUtilities.validateCursor("Error: basic location query failed.", c, northPole);

        // test query by rowId
        c = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                LocationEntry._ID + " = ?",
                new String[]{String.valueOf(rowId)},
                null);
        TestUtilities.validateCursor("Error: location query by rowId failed.", c, northPole);

        // test if Notification Uri is set up correctly
        if (Build.VERSION.SDK_INT >= 19){
            assertEquals("Error: Location query did not set up Notification Uri correctly.",
                    LocationEntry.CONTENT_URI, c.getNotificationUri());
        }
    }

    public void testBasicWeatherQueries(){
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();

        long northPoleRowId = TestUtilities.insertNorthPoleLocationValues(mContext);
        ContentValues weatherValues = TestUtilities.createWeatherValues(northPoleRowId);
        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        // Verify we got a row back.
        assertTrue("Error: Failure to insert weather Values", weatherRowId != -1);
        db.close();

        Cursor c = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        TestUtilities.validateCursor("Error: basic weather query failed.", c, weatherValues);

        // test if Notification Uri is set up correctly
        if (Build.VERSION.SDK_INT >= 19){
            assertEquals("Error: Weather query did not set up Notification Uri correctly.",
                    WeatherEntry.CONTENT_URI, c.getNotificationUri());
        }
    }

    public void testBulkInsert(){

        long northPoleRowId = TestUtilities.insertNorthPoleLocationValues(mContext);
        ContentValues[] expectedValuesArray = TestUtilities.createWeatherValuesForBulkInsert(northPoleRowId, 7);

        mContext.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, expectedValuesArray);

        Cursor cursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                WeatherEntry.COLUMN_DATE + " ASC");

        assertTrue("BulkInsert test failed. Cursor is empty.", cursor.moveToFirst());

        for(int i = 0; i < expectedValuesArray.length; i++){
            TestUtilities.validateCurrentRecord("BulkInsert test failed.", cursor, expectedValuesArray[i]);
            cursor.moveToNext();
        }
        cursor.close();
    }
}
