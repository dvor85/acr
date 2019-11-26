package com.telephony.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReglamentService extends Service {

    private ExecutorService es;
    private PreferenceUtils sPref = null;
    private long interval = 0;
    private int oneTimeID = (int) (SystemClock.uptimeMillis() % 99999999);


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(oneTimeID, Utils.ServiceNotification(this));
        }
        es = Executors.newFixedThreadPool(1);
        sPref = PreferenceUtils.getInstance(this);
        Log.d(Utils.LOG_TAG, getClass().getName() + " Create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        es.execute(new RunService(intent, flags, startId, this));
        return START_STICKY;
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

        @Override
        public void run() {
            try {
                Log.d(Utils.LOG_TAG, context.getClass().getName() + ": start " + startId);
                interval = intent.getLongExtra(Utils.EXTRA_INTERVAL, 0);
                if (sPref.getRootDir().exists() && (sPref.getKeepDays() > 0) && (Utils.getExternalStorageStatus() == Utils.MEDIA_MOUNTED)) {
                    File[] flist = Utils.rlistFiles(sPref.getRootDir(), new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            Date today = new Date();
                            return f.isDirectory()
                                    || (!f.getName().equals(".nomedia") && new Date(f.lastModified()).before(new Date(today.getTime()
                                    - (Utils.DAY * sPref.getKeepDays()))));
                        }
                    });
                    for (File file : flist) {
                        if (file.isDirectory()) {
                            String[] list = file.list();
                            if (list.length == 0) {
                                if (file.delete()) {
                                    Log.d(Utils.LOG_TAG, "delete directory: " + file.getAbsoluteFile() + " success!");
                                } else {
                                    Log.d(Utils.LOG_TAG, "delete directory: " + file.getAbsoluteFile() + " failed!");
                                }
                            }
                        } else {
                            if (file.delete()) {
                                Log.d(Utils.LOG_TAG, "delete file: " + file.getAbsoluteFile() + " success!");
                            } else {
                                Log.d(Utils.LOG_TAG, "delete file: " + file.getAbsoluteFile() + " failed!");
                            }
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
                if (interval > 0) {
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);
                    am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, pi);
                }
            } finally {
                stopSelf();
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.shutdownAndAwaitTermination(es, 5, TimeUnit.SECONDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(STOP_FOREGROUND_DETACH | STOP_FOREGROUND_REMOVE);
        }
        Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
    }

}
