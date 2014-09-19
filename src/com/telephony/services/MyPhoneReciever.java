package com.telephony.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneReciever extends BroadcastReceiver {

	private String phoneNumber;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));

		if (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED) {
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				Intent myIntent = new Intent(context, CallRecordService.class);
				myIntent.putExtra("commandType", Utils.STATE_OUT_NUMBER);
				myIntent.putExtra("phoneNumber", phoneNumber);
				context.startService(myIntent);
			} else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
					phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
					Intent myIntent = new Intent(context, CallRecordService.class);
					myIntent.putExtra("commandType", Utils.STATE_IN_NUMBER);
					myIntent.putExtra("phoneNumber", phoneNumber);
					context.startService(myIntent);
				} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
					Intent myIntent = new Intent(context, CallRecordService.class);
					myIntent.putExtra("commandType", Utils.STATE_CALL_START);
					myIntent.putExtra("phoneNumber", phoneNumber);
					context.startService(myIntent);
				} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
					Intent myIntent = new Intent(context, CallRecordService.class);
					myIntent.putExtra("commandType", Utils.STATE_CALL_END);
					context.startService(myIntent);
				}
			}
		}
	}
}
