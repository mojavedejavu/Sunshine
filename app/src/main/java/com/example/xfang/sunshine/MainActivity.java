package com.example.xfang.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
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
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String location = sp.getString(
                    getString(R.string.pref_location_key),
                    getString(R.string.pref_location_defaultValue));

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
}
