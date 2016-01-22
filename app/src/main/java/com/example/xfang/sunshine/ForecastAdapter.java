package com.example.xfang.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;

public class ForecastAdapter extends CursorAdapter{

    public ForecastAdapter(Context context, Cursor c, int flags){
        super(context, c, flags);
    }

    private String convertCursorRowToUXFormat(Cursor c){
        double max = c.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        double min = c.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        long dateInMilliseconds = c.getLong(ForecastFragment.COL_WEATHER_DATE);
        String description = c.getString(ForecastFragment.COL_WEATHER_DESC);

        String day = Utilities.formatMillisecondsToReadableDate(dateInMilliseconds);

        return day + "   " + min + " / " + max + ", " + description;

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor){
        TextView tv = (TextView)view;
        tv.setText(convertCursorRowToUXFormat(cursor));
    }

}
