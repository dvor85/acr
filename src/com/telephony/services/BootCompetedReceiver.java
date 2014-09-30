package com.telephony.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompetedReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d(Utils.LOG_TAG, "Receive BOOT_COMPLETED");
		Intent myIntent = new Intent(context, StartService.class);		
		context.startService(myIntent);		
	}
}
