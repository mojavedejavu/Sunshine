package com.example.xfang.sunshine.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.HashSet;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

import junit.framework.Test;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    @Override
    public void setUp(){
        deleteDatabase();
    }

    private boolean deleteDatabase(){
        return mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    public void testCreateDb() throws Exception {
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();
        assertTrue("Error: Database can't be opened.", db.isOpen());

        /*
         * check if both tables exist
         */
        HashSet<String> tableHashSet = new HashSet<>();
        tableHashSet.add(WeatherEntry.TABLE_NAME);
        tableHashSet.add(LocationEntry.TABLE_NAME);

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        assertTrue("Error: Database wasn't created correctly.", c.moveToFirst());

        int columnId = c.getColumnIndex("name");
        assertTrue(columnId == 0);
        do{
            String tableName = c.getString(columnId);
            Log.d(LOG_TAG, "Database contains table " + tableName);
            tableHashSet.remove(tableName);
        }
        while (c.moveToNext());

        assertTrue("Error: Database doesn't contain all the required tables.", tableHashSet.isEmpty());

        /*
         *  check if weather table has all the required entries
         */
        HashSet<String> weatherColumnsSet = new HashSet<>();
        weatherColumnsSet.add(WeatherEntry._ID);
        weatherColumnsSet.add(WeatherEntry.COLUMN_LOC_KEY);
        weatherColumnsSet.add(WeatherEntry.COLUMN_WEATHER_ID);
        weatherColumnsSet.add(WeatherEntry.COLUMN_SHORT_DESC);
        weatherColumnsSet.add(WeatherEntry.COLUMN_DATE);
        weatherColumnsSet.add(WeatherEntry.COLUMN_MAX_TEMP);
        weatherColumnsSet.add(WeatherEntry.COLUMN_MIN_TEMP);
        weatherColumnsSet.add(WeatherEntry.COLUMN_PRESSURE);
        weatherColumnsSet.add(WeatherEntry.COLUMN_HUMIDITY);
        weatherColumnsSet.add(WeatherEntry.COLUMN_WIND_SPEED);
        weatherColumnsSet.add(WeatherEntry.COLUMN_DEGREES);

        c = db.rawQuery("PRAGMA table_info(" + WeatherEntry.TABLE_NAME + ")",
                null);
        assertTrue("Error: Failed to query database for weather table info.", c.moveToFirst());

        columnId = c.getColumnIndex("name");
        do{
            String columnName = c.getString(columnId);
            assertTrue("Error: weather table contains extraneous column " + columnName + ".",
                    weatherColumnsSet.remove(columnName));
        }while(c.moveToNext());

        assertTrue("Error: Weather table doesn't contain all the required columns.",
                weatherColumnsSet.isEmpty());


        /*
         *  check if location table has all the required entries
         *  (same as above)
         */

        HashSet<String> locationColumnsSet = new HashSet<>();
        locationColumnsSet.add(LocationEntry._ID);
        locationColumnsSet.add(LocationEntry.COLUMN_LOCATION_SETTING);
        locationColumnsSet.add(LocationEntry.COLUMN_CITY_NAME);
        locationColumnsSet.add(LocationEntry.COLUMN_COORD_LAT);
        locationColumnsSet.add(LocationEntry.COLUMN_COORD_LONG);

        c = db.rawQuery("PRAGMA table_info(" + LocationEntry.TABLE_NAME + ")",
                null);
        assertTrue("Error: Failed to query database for location table info.", c.moveToFirst());

        columnId = c.getColumnIndex("name");
        do{
            String columnName = c.getString(columnId);
            assertTrue("Error: location table contains extraneous column " + columnName + ".",
                    locationColumnsSet.remove(columnName));
        }while(c.moveToNext());

        assertTrue("Error: Location table doesn't contain all the required columns.",
                locationColumnsSet.isEmpty());

        /*
         *  clean up
         */
        c.close();
        db.close();
    }

    public long insertLocation(){
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();
        assertTrue("Error: Database can't be opened.", db.isOpen());

        // insert a record to location table
        ContentValues northPole = TestUtilities.createNorthPoleLocationValues();
        long locationRowId = db.insert(LocationEntry.TABLE_NAME, null, northPole);
        assertTrue("Error: insert to location table failed.",
                locationRowId != -1L);

        // read it out
        Cursor c = db.query(LocationEntry.TABLE_NAME,
                null, // projection
                null, // selection
                null, // selectionArgs
                null, // groupBy
                null, // having
                null // orderBy
                );

        // validate
        assertTrue("Error: no result returned from location table query.", c.moveToFirst());
        TestUtilities.validateCurrentRecord("Error: location query validation failed.", c, northPole);

        assertFalse("Error: location query returned more than one record.", c.moveToNext());

        // clean-up
        c.close();
        db.close();
        return locationRowId;
    }

    public long testLocationTable(){
        return insertLocation();
    }

    public long testWeatherTable(){

        long locationRowId = insertLocation();

        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();
        assertTrue("Error: Database can't be opened.", db.isOpen());

        ContentValues weather = TestUtilities.createWeatherValues(locationRowId);
        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weather);

        assertTrue("Error: insert to weather table failed.",
                weatherRowId != -1L);

        // read it out
        Cursor c = db.query(WeatherEntry.TABLE_NAME,
                null, // projection
                null, // selection
                null, // selectionArgs
                null, // groupBy
                null, // having
                null // orderBy
        );

        // validate
        assertTrue("Error: no result returned from weather table query.", c.moveToFirst());
        TestUtilities.validateCurrentRecord("Error: weather query validation failed.", c, weather);

        assertFalse("Error: weather query returned more than one record.", c.moveToNext());

        // clean-up
        c.close();
        db.close();
        return weatherRowId;
    }
}
