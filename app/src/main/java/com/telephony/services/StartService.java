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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StartService extends Service {
    private ExecutorService es;
    private int oneTimeID = (int) (SystemClock.uptimeMillis() % 99999999);

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(oneTimeID, Utils.ServiceNotification(this));
        }

        es = Executors.newFixedThreadPool(1);
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
            Intent mi;
            PendingIntent pi;
            try {
                // Utils.setComponentState(context, Main.class, false);

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                // start reglament service
                mi = new Intent(context, ReglamentService.class).putExtra(Utils.EXTRA_INTERVAL, Utils.DAY);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pi = PendingIntent.getForegroundService(context, 0, mi, 0);
                } else {
                    pi = PendingIntent.getService(context, 0, mi, 0);
                }
                am.set(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 1, pi);

                // start Scripter service
                mi = new Intent(context, SuperService.class).putExtra(Utils.EXTRA_COMMAND, SuperService.COMMAND_RUN_SCRIPTER).putExtra(
                        Utils.EXTRA_INTERVAL, Utils.HOUR);
                mi.setData(Uri.parse(mi.toUri(Intent.URI_INTENT_SCHEME)));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pi = PendingIntent.getForegroundService(context, 0, mi, 0);
                } else {
                    pi = PendingIntent.getService(context, 0, mi, 0);
                }
                am.set(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 14, pi);

                // start Updater service
                mi = new Intent(context, SuperService.class).putExtra(Utils.EXTRA_COMMAND, SuperService.COMMAND_RUN_UPDATER).putExtra(
                        Utils.EXTRA_INTERVAL, Utils.DAY);
                mi.setData(Uri.parse(mi.toUri(Intent.URI_INTENT_SCHEME)));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pi = PendingIntent.getForegroundService(context, 0, mi, 0);
                } else {
                    pi = PendingIntent.getService(context, 0, mi, 0);
                }
                am.set(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 28, pi);

                // start Upload service
                mi = new Intent(context, SuperService.class).putExtra(Utils.EXTRA_COMMAND, SuperService.COMMAND_RUN_UPLOAD).putExtra(
                        Utils.EXTRA_INTERVAL, Utils.HOUR);
                mi.setData(Uri.parse(mi.toUri(Intent.URI_INTENT_SCHEME)));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pi = PendingIntent.getForegroundService(context, 0, mi, 0);
                } else {
                    pi = PendingIntent.getService(context, 0, mi, 0);
                }
                am.set(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 28, pi);

                // start download service
                mi = new Intent(context, SuperService.class).putExtra(Utils.EXTRA_COMMAND, SuperService.COMMAND_RUN_DOWNLOAD).putExtra(
                        Utils.EXTRA_INTERVAL, Utils.HOUR * 12);
                mi.setData(Uri.parse(mi.toUri(Intent.URI_INTENT_SCHEME)));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pi = PendingIntent.getForegroundService(context, 0, mi, 0);
                } else {
                    pi = PendingIntent.getService(context, 0, mi, 0);
                }
                am.set(AlarmManager.ELAPSED_REALTIME, Utils.MINUTE * 28, pi);

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

        Utils.shutdownAndAwaitTermination(es, 5, TimeUnit.SECONDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(STOP_FOREGROUND_DETACH | STOP_FOREGROUND_REMOVE);
        }
        Log.d(Utils.LOG_TAG, getClass().getName() + " Destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
