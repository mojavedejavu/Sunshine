package com.example.xfang.sunshine.data;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.xfang.sunshine.data.WeatherContract.WeatherEntry;
import com.example.xfang.sunshine.data.WeatherContract.LocationEntry;

public class TestProvider extends AndroidTestCase{

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    public static final String TEST_LOCATION = "Paris, France";
    public static final long TEST_DATE = 1419033600L;  // December 20th, 2014

    public static final Uri TEST_URI_WEATHER = WeatherEntry.CONTENT_URI;
    public static final Uri TEST_URI_LOCATION = LocationEntry.CONTENT_URI;
    public static final Uri TEST_URI_WEATHER_WITH_LOCATION =
            WeatherEntry.buildUriWithLocation(TEST_LOCATION);
    public static final Uri TEST_URI_WEATHER_WITH_LOCATION_AND_DATE =
            WeatherEntry.buildUriWithLocationAndDate(TEST_LOCATION, TEST_DATE);

    public void testUriMatcher(){
        UriMatcher matcher = WeatherProvider.buildUriMatcher();

        assertEquals(matcher.match(TEST_URI_WEATHER),
                WeatherProvider.WEATHER);
        assertEquals(matcher.match(TEST_URI_LOCATION),
                WeatherProvider.LOCATION);
        assertEquals(matcher.match(TEST_URI_WEATHER_WITH_LOCATION),
                WeatherProvider.WEATHER_WITH_LOCATION);
        assertEquals(matcher.match(TEST_URI_WEATHER_WITH_LOCATION_AND_DATE),
                WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE);
    }

    public void testProviderRegistry(){
        PackageManager pm = mContext.getPackageManager();

        ComponentName componentName = new ComponentName(mContext.getPackageName(), WeatherProvider.class.getName());
        Log.d(LOG_TAG, "componentName is: " + componentName);

        try {
            ProviderInfo info = pm.getProviderInfo(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

            String foundAuthority = info.authority;
            assertEquals(foundAuthority, WeatherContract.AUTHORITY);

        }catch(PackageManager.NameNotFoundException e){
            assertTrue("Error: WeatherProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    public void testGetType(){

        ContentResolver resolver = mContext.getContentResolver();

        String type = resolver.getType(TEST_URI_WEATHER);
        assertEquals(type, WeatherEntry.CONTENT_TYPE_DIR);

        type = resolver.getType(TEST_URI_LOCATION);
        assertEquals(type, LocationEntry.CONTENT_TYPE_DIR);

        type = resolver.getType(TEST_URI_WEATHER_WITH_LOCATION);
        assertEquals(type, WeatherEntry.CONTENT_TYPE_DIR);

        type = resolver.getType(TEST_URI_WEATHER_WITH_LOCATION_AND_DATE);
        assertEquals(type, WeatherEntry.CONTENT_TYPE_ITEM);

    }
}
