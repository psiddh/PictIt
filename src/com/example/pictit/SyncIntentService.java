package com.example.pictit;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

public class SyncIntentService extends IntentService implements LogUtils {
    private final String TAG = "SyncIntentService";
    //private final String PARAM_STATE
    DataBaseManager mDbManager;

    public SyncIntentService() {
        super("SyncIntentService");
        mDbManager = new DataBaseManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
		mDbManager.onCreate(mDbManager.getWritableDatabase());
		mDbManager.startSync();
    }
}