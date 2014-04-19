package com.example.pictit;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

public class SyncIntentService extends IntentService implements LogUtils {
    private final String TAG = "SyncIntentService";
    // Database fields
    private DataBaseManager mDbHelper;

    public SyncIntentService() {
        super("SyncIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        mDbHelper = DataBaseManager.getInstance(this);
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
		//mDbHelper.onCreate(mDbHelper.getWritableDatabase());
		//mDbHelper = DataBaseManager.getInstance(getApplicationContext());
		//mDbHelper.getWritableDatabase();
        mDbHelper.startSync();
    }
}