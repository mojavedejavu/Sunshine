package com.example.xfang.sunshine.data;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
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

    /*
    This test uses the provider to insert and then update the data. Uncomment this test to
    see if your update location is functioning correctly.
 */
    public void testUpdateLocation() {
        // Create a new map of values, where column names are the keys
        ContentValues values = TestUtilities.createNorthPoleLocationValues();

        Uri locationUri = mContext.getContentResolver().
                insert(LocationEntry.CONTENT_URI, values);
        long locationRowId = ContentUris.parseId(locationUri);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(LocationEntry._ID, locationRowId);
        updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village");

        // Create a cursor with observer to make sure that the content provider is notifying
        // the observers as expected
        Cursor locationCursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI, null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        locationCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                LocationEntry.CONTENT_URI, updatedValues, LocationEntry._ID + "= ?",
                new String[] { Long.toString(locationRowId)});
        assertEquals(count, 1);

        // Test to make sure our observer is called.  If not, we throw an assertion.
        //
        // Students: If your code is failing here, it means that your content provider
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();

        locationCursor.unregisterContentObserver(tco);
        locationCursor.close();

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,   // projection
                LocationEntry._ID + " = " + locationRowId,
                null,   // Values for the "where" clause
                null    // sort order
        );

        TestUtilities.validateCursor("testUpdateLocation.  Error validating location entry update.",
                cursor, updatedValues);

        cursor.close();
    }

    public void testQuerySpecificDate(){
        // insert location
        ContentValues northPole = TestUtilities.createNorthPoleLocationValues();
        Uri northPoleUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, northPole);
        long locationRowId = ContentUris.parseId(northPoleUri);
        assertTrue(locationRowId != -1);

        // insert weather
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);
        Uri weatherInsertUri = mContext.getContentResolver()
                .insert(WeatherEntry.CONTENT_URI, weatherValues);
        assertTrue(weatherInsertUri != null);

        // Get the joined Weather data for a specific date

        Uri queryUri = WeatherEntry.buildUriWithLocationAndDate(TestUtilities.TEST_LOCATION,
                WeatherContract.normalizeDate(TestUtilities.TEST_DATE));
        Cursor weatherCursor = mContext.getContentResolver().query(
                queryUri,
                null,
                null,
                null,
                null
        );

        weatherValues.putAll(northPole);
        TestUtilities.validateCursor("testQuerySpecificDate.  Error validating joined Weather and Location data for a specific date.",
                weatherCursor, weatherValues);
    }


    // Make sure we can still delete after adding/updating stuff
    // insert and query functionality must also be complete before this test can be used.
    public void testInsertReadProvider() {
        ContentValues northPole = TestUtilities.createNorthPoleLocationValues();

        // Register a content observer for our insert.  This time, directly with the content resolver
        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(LocationEntry.CONTENT_URI, true, tco);
        Uri northPoleUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, northPole);

        // Did our content observer get called?
        // getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long locationRowId = ContentUris.parseId(northPoleUri);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating LocationEntry.",
                cursor, northPole);

        // Fantastic.  Now that we have a location, add some weather!
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);
        // The TestContentObserver is a one-shot class
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(WeatherEntry.CONTENT_URI, true, tco);

        Uri weatherInsertUri = mContext.getContentResolver()
                .insert(WeatherEntry.CONTENT_URI, weatherValues);
        assertTrue(weatherInsertUri != null);

        // Did our content observer get called?
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,  // Table to Query
                null,
                null,
                null,
                null
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating WeatherEntry insert.",
                weatherCursor, weatherValues);

        // Add the location values in with the weather data so that we can make
        // sure that the join worked and we actually get all the values back
        weatherValues.putAll(northPole);

        // Get the joined Weather and Location data
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildUriWithLocation(TestUtilities.TEST_LOCATION),
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Weather and Location Data.",
                weatherCursor, weatherValues);

        // Get the joined Weather and Location data with a start date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildUriWithLocationAndStartDate(
                        TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Weather and Location Data with start date.",
                weatherCursor, weatherValues);

        // Get the joined Weather data for a specific date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildUriWithLocationAndDate(TestUtilities.TEST_LOCATION, WeatherContract.normalizeDate(TestUtilities.TEST_DATE)),
                null,
                null,
                null,
                null
        );

        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Weather and Location data for a specific date.",
                weatherCursor, weatherValues);
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
