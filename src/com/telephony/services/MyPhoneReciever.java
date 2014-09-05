package com.telephony.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneReciever extends BroadcastReceiver {

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	public static final int MEDIA_MOUNTED = 0;
	public static final int MEDIA_MOUNTED_READ_ONLY = 1;
	public static final int NO_MEDIA = 2;

	private static final String LogTag = "myLogs";
	private String phoneNumber;

	/**
	 * checks if an external memory card is available
	 * 
	 * @return
	 */
	public static int updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return MEDIA_MOUNTED;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return MEDIA_MOUNTED_READ_ONLY;
		} else {
			return NO_MEDIA;
		}

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LogTag,intent.toUri(Intent.URI_INTENT_SCHEME));

		if (updateExternalStorageState() == MEDIA_MOUNTED) {
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				Intent myIntent = new Intent(context, RecordService.class);
				myIntent.putExtra("commandType", STATE_OUT_NUMBER);
				myIntent.putExtra("phoneNumber", phoneNumber);
				context.startService(myIntent);
			} else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
					phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
					Intent myIntent = new Intent(context, RecordService.class);
					myIntent.putExtra("commandType", STATE_IN_NUMBER);
					myIntent.putExtra("phoneNumber", phoneNumber);
					context.startService(myIntent);
				} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
					Intent myIntent = new Intent(context, RecordService.class);
					myIntent.putExtra("commandType", STATE_CALL_START);
					myIntent.putExtra("phoneNumber", phoneNumber);
					context.startService(myIntent);
				} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
					Intent myIntent = new Intent(context, RecordService.class);
					myIntent.putExtra("commandType", STATE_CALL_END);
					context.startService(myIntent);
				}
			}
		}
	}
}
