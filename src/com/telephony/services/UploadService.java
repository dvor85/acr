package com.telephony.services;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.telephony.services.Utils.PreferenceUtils;

public class UploadService extends Service {

	private ExecutorService es;
	private PreferenceUtils sPref = null;
	private MyFTPClient ftp;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();
		es = Executors.newFixedThreadPool(3);
		sPref = new PreferenceUtils(this);
		sPref.setUploadUrl("ftp://upload:ghjuhtcc@10.0.0.253:21");
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

		public RunService(Intent intent, int flags, int startId) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
		}

		public void run() {
			try {
				ftp = new MyFTPClient();
				ftp.connect(sPref.getUploadUrl());
				Log.d(Utils.LogTag, ftp.getReplyString());
				if (ftp.isAuthorized) {					
					if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
						ArrayList<File> list = Utils.rlistFiles(sPref.getRootDir(), new FilenameFilter() {
							public boolean accept(File dir, String filename) {
								File f = new File(dir, filename);
								Date today = new Date();
								return !f.isHidden()
										&& new Date(f.lastModified()).before(new Date(today.getTime() - (1000L * 60 * 15)));
							}
						});
						for (File file : list) {
							Log.d(Utils.LogTag, "try upload: " + file.getAbsolutePath());
							if (ftp.uploadFile(sPref.getRootDir(), file)) {
								ftp.setHidden(file);
							} else {
								throw new IOException(ftp.getReplyString());
							}
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
				ftp.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
			stopSelf();
		}

	}

	@Override
	public void onDestroy() {

		super.onDestroy();

		ftp = null;
		es = null;
		sPref = null;
		ftp = null;
		Log.d(Utils.LogTag, getClass().getName() + " Destroy");
	}

}
