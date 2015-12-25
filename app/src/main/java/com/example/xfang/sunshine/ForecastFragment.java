package com.example.xfang.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    public ForecastFragment() {
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

        String[] forecastData = {"boo","bee","blob"};


        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_forecast, forecastData);
        listView.setAdapter(adapter);

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
            FetchWeatherTask task = new FetchWeatherTask();
            task.execute("94043");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String>{

        String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String doInBackground(String... params){
            // declare these outside of the try block so they can be closed
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon().
                        appendQueryParameter("q", params[0]).
                        appendQueryParameter("units", "metric").
                        appendQueryParameter("cnt","7").
                        appendQueryParameter("APPID",getResources().getString(R.string.WeatherAPIKey)).build();

                URL url = new URL(builtUri.toString());
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
                return buffer.toString();
            }
            catch(IOException e){
                Log.e(LOG_TAG, "Error ", e);
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
    }
}
