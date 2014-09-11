package com.telephony.services;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.telephony.services.Utils.PreferenceUtils;

public class ReglamentService extends Service {

	private static final String LogTag = "myLogs";
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
		Log.d(LogTag, getClass().getName() + " Create");
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
					// f.delete();
					Log.d(LogTag, "delete file: " + f.getAbsoluteFile());
				}
			}
		}

		public void run() {
			String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + sPref.getRootCallsDir();
			File root_dir = new File(filepath);
			if (root_dir.exists()) {
				deleteFiles(root_dir, new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						File f = new File(dir, filename);
						Date today = new Date();
						return new Date(f.lastModified()).before(new Date(today.getTime() - (1000L * 60 * 60 * 24 * sPref.getKeepDays())))
								&& !filename.endsWith(".nomedia");
					}
				});
			}

			stop();
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
		Log.d(LogTag, getClass().getName() + " Destroy");
	}

}
