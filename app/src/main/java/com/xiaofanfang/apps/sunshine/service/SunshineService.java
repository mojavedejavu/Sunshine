package com.xiaofanfang.apps.sunshine.service;


import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xiaofanfang.apps.sunshine.R;

public class SunshineService extends IntentService{

    String LOG_TAG = SunshineService.class.getSimpleName();

    public SunshineService(){
        super(SunshineService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {


    }

    public static class FetchWeatherAlarmReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent sunshineServiceIntent = new Intent(context, SunshineService.class);
            context.startService(sunshineServiceIntent);
        }
    }

}
