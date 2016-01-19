package com.example.xfang.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

import java.sql.SQLException;

public class WeatherProvider extends ContentProvider{

    private WeatherDbHelper mWeatherDbHelper;

    public static final int WEATHER = 0;
    public static final int LOCATION = 1;
    public static final int WEATHER_WITH_LOCATION = 100;
    public static final int WEATHER_WITH_LOCATION_AND_DATE = 200;

    SQLiteQueryBuilder mQueryBuilder;

    static {
        SQLiteQueryBuilder mQueryBuilder = new SQLiteQueryBuilder();

        String INNER_JOIN_SQL = WeatherEntry.TABLE_NAME + " INNER JOIN " +
                LocationEntry.TABLE_NAME + " ON " +
                WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_LOC_KEY + " = " +
                LocationEntry.TABLE_NAME + "." + LocationEntry._ID;

        mQueryBuilder.setTables(INNER_JOIN_SQL);
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }

    public static UriMatcher buildUriMatcher(){
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        String authority = WeatherContract.AUTHORITY;

        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*",
                WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#",
                WEATHER_WITH_LOCATION_AND_DATE);

        return matcher;
    }

    private Cursor queryWithLocation(Uri uri, String[] projection, String sortOrder){

        SQLiteDatabase db = mWeatherDbHelper.getReadableDatabase();

        String selection = LocationEntry.TABLE_NAME + "." +
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?";
        String[] selectionArgs;

        String location = WeatherEntry.getLocationFromUri(uri);
        long startDate = WeatherEntry.getStartDateFromUri(uri);

        if (startDate == 0){

            selectionArgs = new String[]{location};
        }
        else{
            selection += " AND " + WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_DATE + " >= ?";
            selectionArgs = new String[]{location, String.valueOf(startDate)};
        }

        Cursor cursor = mQueryBuilder.query(db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        return cursor;
    }

    private Cursor queryWithLocationAndDate(Uri uri, String[] projection, String sortOrder){
        String location = WeatherEntry.getLocationFromUri(uri);
        long startDate = WeatherEntry.getStartDateFromUri(uri);

        SQLiteDatabase db = mWeatherDbHelper.getReadableDatabase();

        String selection = LocationEntry.TABLE_NAME + "." +
                LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                WeatherEntry.TABLE_NAME + "." +
                WeatherEntry.COLUMN_DATE + " = ?";
        String[] selectionArgs = new String[]{location, String.valueOf(startDate)};

        Cursor cursor = mQueryBuilder.query(db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        return cursor;

    }


    @Override
    public boolean onCreate() {
        mWeatherDbHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public String getType (@NonNull Uri uri){
        UriMatcher matcher = buildUriMatcher();
        switch (matcher.match(uri)){
            case WEATHER:
                return WeatherEntry.CONTENT_TYPE_DIR;
            case LOCATION:
                return LocationEntry.CONTENT_TYPE_DIR;
            case WEATHER_WITH_LOCATION:
                return WeatherEntry.CONTENT_TYPE_DIR;
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherEntry.CONTENT_TYPE_ITEM;
            default:
                return null;
        }
    }

    @Override
    public Cursor query (@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        Cursor c;

        SQLiteDatabase db = mWeatherDbHelper.getReadableDatabase();
        int type = buildUriMatcher().match(uri);

        switch(type){
            case WEATHER:
                c = db.query(WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case LOCATION:
                c = db.query(LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case WEATHER_WITH_LOCATION:
                c = queryWithLocation(uri, projection, sortOrder);
                break;
            case WEATHER_WITH_LOCATION_AND_DATE:
                c = queryWithLocationAndDate(uri, projection, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public Uri insert (@NonNull Uri uri, ContentValues values){
        SQLiteDatabase db = mWeatherDbHelper.getWritableDatabase();
        int match = buildUriMatcher().match(uri);

        Uri returnUri;

        switch(match){
            case WEATHER:
                normalizeDate(values);
                long rowId = db.insert(WeatherEntry.TABLE_NAME, null, values);
                if (rowId == -1){
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                else{
                    returnUri = WeatherEntry.buildUriWithRowId(rowId);
                }
                break;
            case LOCATION:
                rowId = db.insert(LocationEntry.TABLE_NAME, null, values);
                if (rowId == -1){
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                else{
                    returnUri = LocationEntry.buildUriWithRowId(rowId);
                }
                break;
            default:
                throw new UnsupportedOperationException("Failed to insert row into unknown uri: " + uri);
        }
        Log.d("WeatherProvider.insert", "Inserted a record at Uri: " + uri);

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }


    @Override
    public int bulkInsert(Uri uri, ContentValues[] valuesArray){
        SQLiteDatabase db = mWeatherDbHelper.getWritableDatabase();
        int match = buildUriMatcher().match(uri);

        int rowsInserted = 0;

        switch(match){
            case WEATHER:
                db.beginTransaction();
                try {
                    for (ContentValues values : valuesArray) {
                        normalizeDate(values);
                        long rowId = db.insert(WeatherEntry.TABLE_NAME, null, values);
                        if (rowId == -1){
                            throw new android.database.SQLException("Failed to insert row into " + uri);
                        }
                        else{
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();

                }
                finally{
                    db.endTransaction();
                }

                break;
            case LOCATION:
                db.beginTransaction();
                try {
                    for (ContentValues values : valuesArray) {
                        normalizeDate(values);
                        long rowId = db.insert(LocationEntry.TABLE_NAME, null, values);
                        if (rowId == -1){
                            throw new android.database.SQLException("Failed to insert row into " + uri);
                        }
                        else{
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();

                }
                finally{
                    db.endTransaction();
                }

                break;

            default:
                throw new UnsupportedOperationException("Failed to insert row into unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsInserted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs){
        SQLiteDatabase db = mWeatherDbHelper.getWritableDatabase();
        int match = buildUriMatcher().match(uri);

        int rowsUpdated;

        switch(match){
            case WEATHER:
                normalizeDate(values);
                rowsUpdated = db.update(WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Failed to update at unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete (@NonNull Uri uri, String selection, String[] selectionArgs){
        SQLiteDatabase db = mWeatherDbHelper.getWritableDatabase();
        int match = buildUriMatcher().match(uri);

        int rowsDeleted;

        // official hack to make db.delete return rowsDeleted even when selection is null
        if (selection == null){
            selection = "1";
        }

        switch(match){
            case WEATHER:
                rowsDeleted = db.delete(WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Failed to delete at unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

}
