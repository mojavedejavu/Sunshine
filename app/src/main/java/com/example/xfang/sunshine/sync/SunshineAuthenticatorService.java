package com.example.xfang.sunshine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class SunshineAuthenticatorService extends Service{

    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate(){
        mAuthenticator = new SunshineAuthenticator(this);
    }



    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
