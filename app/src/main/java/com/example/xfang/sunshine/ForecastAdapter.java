package com.example.xfang.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

public class ForecastAdapter extends CursorAdapter{

    public ForecastAdapter(Context context, Cursor c, int flags){
        super(context, c, flags);
    }

    private String convertCursorRowToUXFormat(Cursor c){
        int idx_max = c.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP);
        int idx_min = c.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP);
        int idx_date = c.getColumnIndex(WeatherEntry.COLUMN_DATE);
        int idx_description = c.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC);

        double max = c.getDouble(idx_max);
        double min = c.getDouble(idx_min);
        long dateInMilliseconds = c.getLong(idx_date);
        String description = c.getString(idx_description);

        String day = Utilities.getReadableDate(dateInMilliseconds);

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
