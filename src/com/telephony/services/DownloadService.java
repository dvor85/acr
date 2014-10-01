package com.telephony.services;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DownloadService extends Service {

	private static final String INDEX_FILE = "files";
	private PreferenceUtils sPref = null;
	private ExecutorService es;
	private MyFTPClient ftp;

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
		sPref.setRemoteUrl("ftps://upload:ghjuhtcc@10.0.0.253:990");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId, this));

		return super.onStartCommand(intent, flags, startId);

	}

	private class RunService implements Runnable {
		final Intent intent;
		final int flags;
		final int startId;
		final Context context;

		public RunService(Intent intent, int flags, int startId, Context context) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
			this.context = context;
		}

		public void run() {
			try {
				if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
					ftp = new MyFTPClient();
					ftp.connect(sPref.getRemoteUrl());
					if (ftp.isAuthorized) {
						String[] files = ftp.downloadFileStrings(INDEX_FILE);
						File file = null;
						long rfs = -1;
						long b = System.currentTimeMillis();
						for (String fn : files) {
							try {
								if (!fn.equals("")) {
									rfs = ftp.getFileSize(fn);
									if (rfs > 0) {
										file = ftp.getHidden(ftp.getLocalFile(sPref.getRootDir(), fn));
										if (file != null) {
											if ((file.exists() && (file.length() != rfs)) || (!file.exists())) {
												Log.d(Utils.LOG_TAG, "try download: " + fn + " to " + file.getAbsolutePath());
												ftp.downloadFile(fn, file);
											}
										}
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						Log.d(Utils.LOG_TAG, "time: " + (System.currentTimeMillis() - b));
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
				if (ftp != null) {
					ftp.disconnect();
				}
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
		ftp = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
