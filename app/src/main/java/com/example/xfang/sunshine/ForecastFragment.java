package com.example.xfang.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;
import com.example.xfang.sunshine.service.SunshineService;

public class ForecastFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{

    ForecastAdapter mForecastAdapter;

    static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private static final int LOADER_ID = 0;
    private static int mSelectedItemPosition;
    private static String SELECTED_ITEM_POSITION_KEY = "position_key";
    private ListView mListView;
    private boolean mUseTodayLayout;

    private static final String[] FORECAST_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_ICON_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }

    @Override
    public void onStart(){
        super.onStart();
        fetchWeather();
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        // Users, did you know you turning their device sideways
        // does crazy lifecycle related things?!?!
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_ITEM_POSITION_KEY)) {
            mSelectedItemPosition = savedInstanceState.getInt(SELECTED_ITEM_POSITION_KEY);
        }

        // use special today layout?
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        View rootView = inflater.inflate(R.layout.forecast_fragment, container, false);
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedItemPosition = position;

                Cursor cursor = (Cursor) mForecastAdapter.getItem(position);
                String locationSetting = cursor.getString(ForecastFragment.COL_LOCATION_SETTING);
                long date = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
                Uri uri = WeatherEntry.buildUriWithLocationAndDate(locationSetting, date);

                Callback callback = (Callback) getActivity();
                callback.onItemSelected(uri);
            }
        });


        return rootView;
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;

        // set the adapter here too
        if (mForecastAdapter != null){
            mForecastAdapter.setUseTodayLayout(useTodayLayout);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        if(mSelectedItemPosition != ListView.INVALID_POSITION) {
            savedInstanceState.putInt(SELECTED_ITEM_POSITION_KEY, mSelectedItemPosition);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            fetchWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void fetchWeather(){
        // start the service
        Intent intent = new Intent(getActivity(), SunshineService.class);
        getActivity().startService(intent);
//        String location = Utilities.getPreferredLocationSetting(getActivity());
//        intent.putExtra(Intent.EXTRA_TEXT, location);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle){
        String location = Utilities.getPreferredLocationSetting(getActivity());

        return new CursorLoader(
                getActivity(),
                WeatherEntry.buildUriWithLocationAndStartDate(location, System.currentTimeMillis()),
                FORECAST_COLUMNS, // projection
                null, // selection
                null, // selectionArgs
                WeatherEntry.COLUMN_DATE + " ASC "  // sortOrder
                );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor){
        mForecastAdapter.swapCursor(cursor);

        mListView.smoothScrollToPosition(mSelectedItemPosition);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mForecastAdapter.swapCursor(null);
    }

    // fetch weather and restart loader
    public void onLocationChanged(){
        fetchWeather();
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }


    /*
     * Callback interface to handle communication between
     * ForecastFragment and DetailFragment
     */
    public interface Callback{
        void onItemSelected(Uri detailFragmentUri);
    }
}
