package com.telephony.services;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ReglamentService extends Service {

	private ExecutorService es;
	private PreferenceUtils sPref = null;

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
				if (sPref.getRootDir().exists() && (sPref.getKeepDays() > 0) && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
					ArrayList<File> list = Utils.rlistFiles(sPref.getRootDir(), new FilenameFilter() {
						public boolean accept(File dir, String filename) {
							File f = new File(dir, filename);
							Date today = new Date();
							return !f.getName().equals(".nomedia")
									&& new Date(f.lastModified()).before(new Date(today.getTime() - (Utils.DAY * sPref.getKeepDays())));
						}
					});
					for (File file : list) {
						if (file.delete()) {
							Log.d(Utils.LOG_TAG, "delete file: " + file.getAbsoluteFile() + " success!");
						} else {
							Log.d(Utils.LOG_TAG, "delete file: " + file.getAbsoluteFile() + " failed!");
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
