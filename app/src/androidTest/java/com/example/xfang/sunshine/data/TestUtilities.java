package com.example.xfang.sunshine.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.Map.Entry;
import java.util.Set;

public class TestUtilities extends AndroidTestCase {

    static final String TEST_LOCATION = "99705";
    static final String TEST_CITY_NAME = "North Pole";
    static final double TEST_CITY_LAT = 64.7488;
    static final double TEST_CITY_LONG = -147.353;
    static final long TEST_DATE = 1419033600L;  // December 20th, 2014


    static void validateCursor(String errorMsg, Cursor c, ContentValues expectedValues) {
        assertTrue(errorMsg + " Cursor is empty.", c.moveToFirst());
        validateCurrentRecord(errorMsg, c, expectedValues);
        c.close();
    }

    static void validateCurrentRecord(String errorMsg, Cursor c, ContentValues expectedValues){
        Set<Entry<String, Object>> set = expectedValues.valueSet();
        for(Entry<String, Object> entry : set){
            String columnName = entry.getKey();
            int columnId = c.getColumnIndex(columnName);
            assertTrue(errorMsg + " Column name " + columnName + " not found.",
                    columnId != -1);

            String foundVal= c.getString(columnId);
            String expectedVal = entry.getValue().toString();
            assertTrue(errorMsg + " Column " + columnName + " expects value " +
                    expectedVal + " but found value " + foundVal + ".",
                    foundVal.equals(expectedVal));
        }

    }

    static ContentValues createNorthPoleLocationValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION);
        testValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, TEST_CITY_NAME);
        testValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, TEST_CITY_LAT);
        testValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, TEST_CITY_LONG);

        return testValues;
    }

    static ContentValues createWeatherValues(long locationRowId) {
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, TEST_DATE);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321);

        return weatherValues;
    }

    static long insertNorthPoleLocationValues(Context context) {
        // insert our test records into the database
        WeatherDbHelper dbHelper = new WeatherDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();

        long locationRowId;
        locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert North Pole Location Values", locationRowId != -1);

        return locationRowId;
    }


}
