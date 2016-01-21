package com.example.xfang.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    final String LOG_TAG = ForecastFragment.class.getSimpleName();

    ForecastAdapter mForecastAdapter;

    public ForecastFragment() {
    }


    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        super.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String locationSetting = Utilities.getPreferredLocationSetting(getActivity());
        Uri queryUri = WeatherEntry.buildUriWithLocationAndStartDate(
                locationSetting, System.currentTimeMillis());
        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";

        Cursor cursor = getActivity().getContentResolver().query(queryUri,
                null, null, null, sortOrder);
        mForecastAdapter = new ForecastAdapter(getActivity(), cursor, 0);

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
}
