/*
 *  Copyright 2012 Kobi Krasnoff
 * 
 * This file is part of Call recorder For Android.

    Call recorder For Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Call recorder For Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Call recorder For Android.  If not, see <http://www.gnu.org/licenses/>
 */
package com.call.recorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneReciever extends BroadcastReceiver {

	public static final String FILE_DIRECTORY = ".calls";

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	public static final int MEDIA_MOUNTED = 0;
	public static final int MEDIA_MOUNTED_READ_ONLY = 1;
	public static final int NO_MEDIA = 2;

	private static final String LogTag = "CallRecorder";
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

		if (updateExternalStorageState() == MEDIA_MOUNTED) {
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				Intent myIntent = new Intent(context, RecordService.class);
				myIntent.putExtra("commandType", STATE_OUT_NUMBER);
				myIntent.putExtra("phoneNumber", phoneNumber);
				context.startService(myIntent);
			} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
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