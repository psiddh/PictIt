package com.app.spicit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {
	private DataBaseManager mDbHelper;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (DataBaseManager.state != DataBaseManager.SyncState.SYNC_STATE_COMPLETED
				&& DataBaseManager.state != DataBaseManager.SyncState.SYNC_STATE_INPROGRESS
				&& isOnline(context)) {
			mDbHelper = DataBaseManager.getInstance(context);
			Thread thread = new Thread() {
				@Override
				public void run() {
					mDbHelper.startSync();
				}
			};
			thread.start();
		}
	}

	public boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		// should check null because in air plan mode it will be null
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}
}
