package com.example.xfang.sunshine.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.HashSet;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

/**
 * Created by xfang on 1/4/16.
 */
public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

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

        db.close();
    }
}
