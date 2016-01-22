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
import android.widget.TextView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

public class DetailActivityFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>
{

    static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private static final int DETAIL_LOADER_ID = 10;

    String mForecastString;
    ShareActionProvider mShareActionProvider;

    final String SHARE_HASHTAG = "#SunshineApp";


    private static final String[] FORECAST_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;

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
        Log.d(LOG_TAG, "---> mForecastString: " + mForecastString);
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
                    FORECAST_COLUMNS, // projection
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
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor){
        if (!cursor.moveToFirst()) {
            return;
        }
        mForecastString = convertCursorRowToUXFormat(cursor);

        TextView textView = (TextView)getView().findViewById(R.id.detail_activity_forecast_string);
        textView.setText(mForecastString);

        if (mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareIntent());
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
    }

}
