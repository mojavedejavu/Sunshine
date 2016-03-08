package com.xiaofanfang.apps.sunshine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class SunshineSyncService extends Service {

    private static final String LOG_TAG = SunshineSyncService.class.getSimpleName();
    private static SunshineSyncAdapter mSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object mLock = new Object();

    @Override
    public void onCreate() {
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        Log.d(LOG_TAG, "In onCreate");
        synchronized (mLock) {
            if (mSyncAdapter == null) {
                mSyncAdapter = new SunshineSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
         /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         */
        return mSyncAdapter.getSyncAdapterBinder();
    }
}
