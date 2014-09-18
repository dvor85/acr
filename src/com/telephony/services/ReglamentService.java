package com.telephony.services;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.telephony.services.Utils.PreferenceUtils;

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
		es = Executors.newFixedThreadPool(3);
		sPref = new PreferenceUtils(this);
		Log.d(Utils.LogTag, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId));
		return super.onStartCommand(intent, flags, startId);

	}

	private class RunService implements Runnable {
		final Intent intent;
		final int flags;
		final int startId;
		File[] FileList;

		public RunService(Intent intent, int flags, int startId) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
		}

		public void deleteFiles(File root, FilenameFilter filter) {

			File[] list = root.listFiles(filter);
			for (File f : list) {
				if (f.isDirectory()) {
					deleteFiles(f, filter);
				} else {
					if (f.delete()) {
						Log.d(Utils.LogTag, "delete file: " + f.getAbsoluteFile() + " success!");
					} else {
						Log.d(Utils.LogTag, "delete file: " + f.getAbsoluteFile() + " failed!");
					}
				}
			}
		}

		public void run() {
			try {				
				if (sPref.getRootDir().exists() && (sPref.getKeepDays() > 0) && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
					ArrayList<File> list = Utils.rlistFiles(sPref.getRootDir(), new FilenameFilter() {
						public boolean accept(File dir, String filename) {
							File f = new File(dir, filename);
							Date today = new Date();
							return !f.getName().equals(".nomedia")
									&& new Date(f.lastModified()).before(new Date(today.getTime() - (1000L * 60 * 60 * 24 * sPref.getKeepDays())));
						}
					});
					for (File file : list) {
						if (file.delete()) {
							Log.d(Utils.LogTag, "delete file: " + file.getAbsoluteFile() + " success!");
						} else {
							Log.d(Utils.LogTag, "delete file: " + file.getAbsoluteFile() + " failed!");
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
		Log.d(Utils.LogTag, getClass().getName() + " Destroy");
	}

}
