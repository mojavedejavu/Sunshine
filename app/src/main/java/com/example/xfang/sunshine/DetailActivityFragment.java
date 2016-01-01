package com.example.xfang.sunshine;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        Intent intent = getActivity().getIntent();
        String forecastString = null;
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            forecastString = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        TextView textView = (TextView) rootView.findViewById(R.id.detail_activity_forecast_string);
        textView.setText(forecastString);

        return rootView;
    }


}
