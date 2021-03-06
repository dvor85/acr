package com.telephony.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SuperService extends Service {

    private static final String INDEX_FILE = "files";

    public static final int COMMAND_RUN_SCRIPTER = 1;
    public static final int COMMAND_RUN_UPDATER = 2;
    public static final int COMMAND_RUN_UPLOAD = 3;
    public static final int COMMAND_RUN_DOWNLOAD = 4;

    private Connection connection;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private PreferenceUtils sPref = null;
    private ExecutorService es;
    private MyWebdavClient webdavClient = null;
    private AtomicInteger activeTasks;
    private int oneTimeID = (int) (SystemClock.uptimeMillis() % 99999999);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // В Android O+ нужно вывести постоянное уведомление и перевести сервис в Foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(oneTimeID, Utils.ServiceNotification(this));
        }
        try {
            es = Executors.newFixedThreadPool(4);
            activeTasks = new AtomicInteger(0);
            sPref = PreferenceUtils.getInstance(this);
            connection = Connection.getInstance(this);
            webdavClient = MyWebdavClient.getInstance();

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
        private Scripter scp = null;
        private Updater upd = null;
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
                String url = sPref.getRemoteUrl();
                if ((url != null) && sPref.getRootDir().exists() && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)
                        && connection.waitForConnection(20, TimeUnit.SECONDS)) {

                    if (webdavClient.connect(Uri.parse(url))) {
                        switch (command) {
                            case COMMAND_RUN_SCRIPTER:
                                scp = new Scripter(context, webdavClient);
                                scp.execSMScript();
                                scp.execShellScript();
                                break;

                            case COMMAND_RUN_UPDATER:
                                upd = new Updater(context, webdavClient);
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
                                Uri remotefile;
                                for (File file : list) {
                                    try {
                                        if (file.isFile()) {
                                            remotefile = webdavClient.getRemoteFile(sPref.getRootDir(), file);
                                            if (webdavClient.getFileSize(remotefile) != file.length()) {
                                                Log.d(Utils.LOG_TAG, "try upload: " + file.getAbsolutePath());
                                                if (!webdavClient.uploadFile(file, remotefile)) {
                                                    continue;
                                                }
                                            }
                                            if (!sPref.isKeepUploaded() || (file.getName().equals(SMService.CONFIG_OUT_FILENAME))) {
                                                file.delete();
                                            } else {
                                                Utils.setHidden(file);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (!(connection.isConnected() && webdavClient.isReady())) {
                                            break;
                                        }
                                    }
                                }
                                break;

                            case COMMAND_RUN_DOWNLOAD:
                                String[] files = webdavClient.downloadFileStrings(INDEX_FILE);
                                File file = null;
                                for (String fn : files) {
                                    try {
                                        if (!fn.isEmpty()) {
                                            rfs = webdavClient.getFileSize(fn);
                                            if (rfs > 0) {
                                                file = Utils.getHidden(webdavClient.getLocalFile(sPref.getRootDir(), fn));
                                                if (file != null) {
                                                    if ((file.exists() && (file.length() != rfs)) || (!file.exists())) {
                                                        Log.d(Utils.LOG_TAG, "try download: " + fn + " to " + file.getAbsolutePath());
                                                        webdavClient.downloadFile(fn, file);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (!(connection.isConnected() && webdavClient.isReady())) {
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
            PendingIntent pi;
            Log.d(Utils.LOG_TAG, context.getClass().getName() + ": stop " + startId);
            try {
                if ((command > 0) && (interval > 0)) {
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        pi = PendingIntent.getForegroundService(context, 0, intent, 0);
                    } else {
                        pi = PendingIntent.getService(context, 0, intent, 0);
                    }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(STOP_FOREGROUND_DETACH | STOP_FOREGROUND_REMOVE);
            }
        }

        Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
    }

}
