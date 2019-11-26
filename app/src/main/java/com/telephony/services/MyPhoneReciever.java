package com.telephony.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String phoneNumber;
        Intent myIntent = new Intent(context, CallRecordService.class);

        Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            myIntent.putExtra(Utils.EXTRA_COMMAND, CallRecordService.STATE_CALL_IO);
            myIntent.putExtra(CallRecordService.EXTRA_CALL_DIRECTION, CallRecordService.CALL_OUTGOING);
            myIntent.putExtra(Utils.EXTRA_PHONE_NUMBER, phoneNumber);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(myIntent);
            } else {
                context.startService(myIntent);
            }

        } else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
                if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    myIntent.putExtra(Utils.EXTRA_PHONE_NUMBER, phoneNumber);
                    myIntent.putExtra(Utils.EXTRA_COMMAND, CallRecordService.STATE_CALL_IO);
                    myIntent.putExtra(CallRecordService.EXTRA_CALL_DIRECTION, CallRecordService.CALL_INCOMING);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(myIntent);
                    } else {
                        context.startService(myIntent);
                    }
                }

            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
                myIntent.putExtra(Utils.EXTRA_COMMAND, CallRecordService.STATE_CALL_START);
                if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    myIntent.putExtra(Utils.EXTRA_PHONE_NUMBER, phoneNumber);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(myIntent);
                    } else {
                        context.startService(myIntent);
                    }
                }


            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
                myIntent.putExtra(Utils.EXTRA_COMMAND, CallRecordService.STATE_CALL_END);
                if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                    phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    myIntent.putExtra(Utils.EXTRA_PHONE_NUMBER, phoneNumber);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(myIntent);
                } else {
                    context.startService(myIntent);
                }
            }
        }
    }
}

