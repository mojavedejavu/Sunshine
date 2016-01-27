package com.example.xfang.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements ForecastFragment.Callback{

    private static String mLocation;
    private static boolean mTwoPane;
    private static final String DETAIL_FRAGMENT_TAG = "DF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forecast_activity);

        // two panes?
        if (findViewById(R.id.weather_detail_container) != null){
            mTwoPane = true;

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.weather_detail_container, new DetailFragment(),DETAIL_FRAGMENT_TAG)
                        .commit();
            }
        }
        else{
            mTwoPane = false;
        }

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // fab
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // locationSetting
        mLocation = Utilities.getPreferredLocationSetting(this);
    }

    /*
     *   update weather if location has changed
     */
    @Override
    protected void onResume(){
        super.onResume();

        String userLocationSetting = Utilities.getPreferredLocationSetting(this);
        if (userLocationSetting != null && !mLocation.equals(userLocationSetting)){
            mLocation = userLocationSetting;
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().
                    findFragmentById(R.id.forecast_fragment);
            if (ff != null) {
                ff.onLocationChanged();
            }

            DetailFragment df = (DetailFragment)getSupportFragmentManager().
                    findFragmentByTag(DETAIL_FRAGMENT_TAG);
            if (df != null){
                df.onLocationChanged(mLocation);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_viewOnMap){
            String location = Utilities.getPreferredLocationSetting(this);

            Uri uri = Uri.parse("geo:0,0?").buildUpon().
                    appendQueryParameter("q",location).
                    build();
            Log.d("view on map: ", uri.toString());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
            else{
                Log.d("view on map ERROR: ", "no map application available");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri detailFragmentUri) {
        if (mTwoPane){
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI_KEY, detailFragmentUri);

            DetailFragment df = new DetailFragment();
            df.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(
                    R.id.weather_detail_container, df, DETAIL_FRAGMENT_TAG).
                    commit();
        }
        else{
            Intent detailActivityIntent = new Intent(this, DetailActivity.class);
            detailActivityIntent.setData(detailFragmentUri);
            startActivity(detailActivityIntent);
        }
    }
}
