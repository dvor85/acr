package com.telephony.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String phoneNumber;
		
		Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));		

		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
			phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			Intent myIntent = new Intent(context, CallRecordService.class);
			myIntent.putExtra(Utils.EXTRA_COMMAND, Utils.STATE_OUT_NUMBER);
			myIntent.putExtra(Utils.EXTRA_PHONE_NUMBER, phoneNumber);
			context.startService(myIntent);
			
		} else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
			
			if (TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
				phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
				Intent myIntent = new Intent(context, CallRecordService.class);
				myIntent.putExtra(Utils.EXTRA_COMMAND, Utils.STATE_IN_NUMBER);
				myIntent.putExtra(Utils.EXTRA_PHONE_NUMBER, phoneNumber);
				context.startService(myIntent);
				
			} else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
				Intent myIntent = new Intent(context, CallRecordService.class);
				myIntent.putExtra(Utils.EXTRA_COMMAND, Utils.STATE_CALL_START);				
				context.startService(myIntent);
				
			} else if (TelephonyManager.EXTRA_STATE_IDLE.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
				Intent myIntent = new Intent(context, CallRecordService.class);
				myIntent.putExtra(Utils.EXTRA_COMMAND, Utils.STATE_CALL_END);
				context.startService(myIntent);
			}
		}
	}
}
