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

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

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

        Cursor c = mQueryBuilder.query(db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        return c;
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

        Cursor c = mQueryBuilder.query(db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        return c;

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
        Uri dummy = WeatherContract.WeatherEntry.CONTENT_URI;
        return dummy;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs){
        return 0;
    }

    @Override
    public int delete (@NonNull Uri uri, String selection, String[] selectionArgs){
        return 0;
    }

}
