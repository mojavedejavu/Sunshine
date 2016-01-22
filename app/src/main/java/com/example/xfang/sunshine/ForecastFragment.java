package com.example.xfang.sunshine;


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
import android.widget.ListView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;


public class ForecastFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{

    final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private static final int LOADER_ID = 0;

    ForecastAdapter mForecastAdapter;

    public ForecastFragment() {
    }


    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }

    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        super.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String forecastText = mAdapter.getItem(position);
//                Intent detailActivityIntent = new Intent(getActivity(), DetailActivity.class);
//                detailActivityIntent.putExtra(Intent.EXTRA_TEXT, forecastText);
//                startActivity(detailActivityIntent);
//            }
//        });


        return rootView;
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
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void updateWeather(){
        FetchWeatherTask task = new FetchWeatherTask(getActivity());
        String location = Utilities.getPreferredLocationSetting(getActivity());
        task.execute(location);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle){
        String location = Utilities.getPreferredLocationSetting(getActivity());

        return new CursorLoader(
                getActivity(),
                WeatherEntry.buildUriWithLocationAndStartDate(location, System.currentTimeMillis()),
                null, // projection
                null, // selection
                null, // selectionArgs
                WeatherEntry.COLUMN_DATE + " ASC "  // sortOrder
                );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor){
        mForecastAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mForecastAdapter.swapCursor(null);
    }
}
