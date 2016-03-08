package com.xiaofanfang.apps.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ForecastAdapter extends CursorAdapter{

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_NOT_TODAY = 1;
    private static boolean mUseTodayLayout;

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }

    public ForecastAdapter(Context context, Cursor c, int flags){
        super(context, c, flags);
    }

    private static String convertCursorRowToUXFormat(Cursor c){
        double max = c.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        double min = c.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        long dateInMilliseconds = c.getLong(ForecastFragment.COL_WEATHER_DATE);
        String description = c.getString(ForecastFragment.COL_WEATHER_DESC);

        String day = Utilities.formatMillisecondsToReadableDate(dateInMilliseconds);

        return day + "   " + min + " / " + max + ", " + description;

    }

    @Override
    public int getViewTypeCount(){
        return 2;
    }

    @Override
    public int getItemViewType(int position){
        if (position == 0 && mUseTodayLayout){
            return VIEW_TYPE_TODAY;
        }
        else{
            return VIEW_TYPE_NOT_TODAY;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        int layoutId;
        int viewType = getItemViewType(cursor.getPosition());
        if (viewType == VIEW_TYPE_TODAY){
            layoutId = R.layout.list_item_forecast_today;
        }
        else{
            layoutId = R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor){
        long dateInMilliseconds = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        String smartDate = Utilities.getDayName(context, dateInMilliseconds);

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.dateView.setText(smartDate);
        viewHolder.descView.setText(cursor.getString(ForecastFragment.COL_WEATHER_DESC));
        viewHolder.highView.setText(cursor.getString(ForecastFragment.COL_WEATHER_MAX_TEMP));
        viewHolder.lowView.setText(cursor.getString(ForecastFragment.COL_WEATHER_MIN_TEMP));

        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_ICON_ID);
        int viewType = getItemViewType(cursor.getPosition());
        if (viewType == VIEW_TYPE_TODAY){
            int artResource = Utilities.getArtResourceForWeatherCondition(weatherId);
            viewHolder.iconView.setImageResource(artResource);
        }
        else{
            int icResource = Utilities.getArtResourceForWeatherCondition(weatherId);
            viewHolder.iconView.setImageResource(icResource);
        }
    }

    public class ViewHolder{
        private View mView;
        TextView dateView;
        TextView descView;
        TextView highView;
        TextView lowView;
        ImageView iconView;

        public ViewHolder(View view){
            mView = view;
            dateView = (TextView) mView.findViewById(R.id.list_item_date_textview);
            descView = (TextView) mView.findViewById(R.id.list_item_description_textview);
            highView = (TextView) mView.findViewById(R.id.list_item_high_textview);
            lowView = (TextView) mView.findViewById(R.id.list_item_low_textview);
            iconView = (ImageView) mView.findViewById(R.id.list_item_icon);
        }
    }
}
