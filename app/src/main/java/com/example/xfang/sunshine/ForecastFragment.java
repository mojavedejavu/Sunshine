package com.example.xfang.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    final String LOG_TAG = ForecastFragment.class.getSimpleName();

    final int NUM_DAYS = 7;
    ArrayAdapter<String> mAdapter;

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
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        final ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.list_item_forecast, new ArrayList<String>());
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecastText = mAdapter.getItem(position);
                Intent detailActivityIntent = new Intent(getActivity(), DetailActivity.class);
                detailActivityIntent.putExtra(Intent.EXTRA_TEXT, forecastText);
                startActivity(detailActivityIntent);
            }
        });


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
        FetchWeatherTask task = new FetchWeatherTask();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = sp.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_defaultValue));
        task.execute(location);
    }


    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params){
            // declare these outside of the try block so they can be closed
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String format = "json";
            String unit = "metric";
            int numDays = NUM_DAYS;

            try {
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String API_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon().
                        appendQueryParameter(QUERY_PARAM, params[0]).
                        appendQueryParameter(FORMAT_PARAM, format).
                        appendQueryParameter(UNITS_PARAM, unit).
                        appendQueryParameter(DAYS_PARAM, String.valueOf(numDays)).
                        appendQueryParameter(API_PARAM,getResources().getString(R.string.WeatherAPIKey)).build();

                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "URL: " + url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null){
                    buffer.append(line + "\n");
                }
                Log.d(LOG_TAG, buffer.toString());

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String unitsPref = sp.getString(
                        getString(R.string.pref_units_key),
                        getString(R.string.pref_units_defaultValue));
                boolean toImperial = unitsPref.equals(getString(R.string.pref_units_imperial_key));

                String[] parsedForecasts = Utilities.getWeatherDataFromJson(buffer.toString(), numDays, toImperial);
                return parsedForecasts;
            }
            catch(IOException e){
                Log.e(LOG_TAG, "Error ", e);
                e.printStackTrace();
                return null;
            }

            finally{
                if (urlConnection != null){
                    urlConnection.disconnect();
                }
                if (reader != null){
                    try {
                        reader.close();
                    }
                    catch(IOException e){
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String[] array){
            if (array != null) {
                mAdapter.clear();
                mAdapter.addAll(array);
            }

        }
    }
}
