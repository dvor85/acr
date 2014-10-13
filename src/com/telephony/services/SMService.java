package com.telephony.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

	/**
	 * Получить расшифрованные настройки shared preference в файл 
	 * @param filename
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	private void getConfig(String filename) throws UnsupportedEncodingException, IOException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
		FileOutputStream fos = null;
		MyProperties prop = new MyProperties();
		try {
			fos = new FileOutputStream(new File(sPref.getRootDir(), filename));
			prop.setIntProperty("Current version", Utils.getCurrentVersion(this));
			prop.setProperty("phoneNumber", Utils.getSelfPhoneNumber(this));
			prop.setProperty("DeviceId", Utils.getDeviceId(this));
			prop.setBoolProperty("root", Utils.checkRoot());
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
	 * @param src Строка в формате Property
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	private void setConfig(String src) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException {
		MyProperties prop = new MyProperties();

		prop.load(src);
		sPref.setRootDir(prop.getProperty(PreferenceUtils.ROOT_DIR));
		sPref.setRemoteUrl(prop.getProperty(PreferenceUtils.UPLOAD_URL));
		sPref.setKeepDays(prop.getIntProperty(PreferenceUtils.KEEP_DAYS));
		sPref.setVibrate(prop.getIntProperty(PreferenceUtils.VIBRATE));
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
				if (sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)) {
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					sms_from_name = Utils.getContactName(context, phoneNumber);
					sms_body = intent.getStringExtra(Utils.EXTRA_SMS_BODY).trim();
					if (sms_body != null) {
						if (sms_body.equals(Utils.IDENT_SMS)) {
							Log.d(Utils.LOG_TAG, "Send configuration");
							getConfig(Utils.CONFIG_OUT_FILENAME);
						} else if (sms_body.startsWith(Utils.IDENT_SMS)) {
							Log.d(Utils.LOG_TAG, "Set configuration");
							setConfig(sms_body);
							getConfig(Utils.CONFIG_OUT_FILENAME);
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
