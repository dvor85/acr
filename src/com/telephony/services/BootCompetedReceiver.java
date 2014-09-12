package com.telephony.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompetedReceiver extends BroadcastReceiver {
	private static final String LogTag = "myLogs";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LogTag, "Receive BOOT_COMPLETED");
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent myIntent = new Intent(context, ReglamentService.class);
		PendingIntent pmyIntent = PendingIntent.getService(context, 0, myIntent, 0);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_DAY, pmyIntent);
	}
}
