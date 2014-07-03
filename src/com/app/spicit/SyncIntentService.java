package com.app.spicit;

import android.app.IntentService;
import android.content.Intent;

public class SyncIntentService extends IntentService implements LogUtils {
    // Database fields
    private DataBaseManager mDbHelper;

    public SyncIntentService() {
        super("SyncIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mDbHelper = DataBaseManager.getInstance(this);
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mDbHelper.startSync();
    }
}