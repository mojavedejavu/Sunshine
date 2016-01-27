package com.example.xfang.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.ShareActionProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

import org.w3c.dom.Text;

public class DetailActivityFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>
{

    static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private static final int DETAIL_LOADER_ID = 10;

    String mForecastString;
    ShareActionProvider mShareActionProvider;

    final String SHARE_HASHTAG = "#SunshineApp";

    TextView mSmartDateView;
    TextView mDateView;
    TextView mDescView;
    TextView mHighView;
    TextView mLowView;
    TextView mHumidityView;
    TextView mWindView;
    TextView mPressureView;

    ImageView mIconView;

    private static final String[] DETAIL_FORECAST_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_WEATHER_ICON_ID = 9;


    private static String convertCursorRowToUXFormat(Cursor c){
        double max = c.getDouble(COL_WEATHER_MAX_TEMP);
        double min = c.getDouble(COL_WEATHER_MIN_TEMP);
        long dateInMilliseconds = c.getLong(COL_WEATHER_DATE);
        String description = c.getString(COL_WEATHER_DESC);

        String day = Utilities.formatMillisecondsToReadableDate(dateInMilliseconds);

        return day + "   " + min + " / " + max + ", " + description;

    }

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mSmartDateView = (TextView) rootView.findViewById(R.id.list_item_smart_date_textview);
        mDateView = (TextView) rootView.findViewById(R.id.list_item_date_textview);
        mDescView = (TextView) rootView.findViewById(R.id.list_item_description_textview);
        mHighView = (TextView) rootView.findViewById(R.id.list_item_high_textview);
        mLowView = (TextView) rootView.findViewById(R.id.list_item_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.list_item_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.list_item_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.list_item_pressure_textview);

        mIconView = (ImageView) rootView.findViewById(R.id.list_item_icon);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getActivity().getMenuInflater().inflate(R.menu.menu_fragment_detail, menu);

        MenuItem shareMenuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mForecastString != null){
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    private Intent createShareIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastString + " " + SHARE_HASHTAG);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle){
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            Uri queryUri = intent.getData();

            return new CursorLoader(
                    getActivity(),
                    queryUri,
                    DETAIL_FORECAST_COLUMNS, // projection
                    null, // selection
                    null, // selectionArgs
                    null  // sortOrder
            );
        }
        else{
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }

        mForecastString = convertCursorRowToUXFormat(cursor);

        int weatherId = cursor.getInt(COL_WEATHER_ICON_ID);

        long dateInMilliseconds = cursor.getLong(COL_WEATHER_DATE);
        String readableDate = Utilities.formatMillisecondsToReadableDate(dateInMilliseconds);

        String description = cursor.getString(COL_WEATHER_DESC);
        String maxTemp = cursor.getString(COL_WEATHER_MAX_TEMP);
        String minTemp = cursor.getString(COL_WEATHER_MIN_TEMP);
        String humidityString = getActivity().getString(R.string.humidity) + ": " +
                cursor.getString(COL_WEATHER_HUMIDITY);
        String windString = getActivity().getString(R.string.wind) + ": " +
                cursor.getString(COL_WEATHER_WIND_SPEED);
        String pressureString = String.format(getActivity().getString(R.string.format_pressure),
                cursor.getDouble(COL_WEATHER_PRESSURE));

        mDateView.setText(readableDate);
        mDescView.setText(description);
        mHighView.setText(maxTemp);
        mLowView.setText(minTemp);
        mHumidityView.setText(humidityString);
        mWindView.setText(windString);
        mPressureView.setText(pressureString);

        mIconView.setImageResource(Utilities.getArtResourceForWeatherCondition(weatherId));

        if (mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
    }
}
