package com.telephony.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract.PhoneLookup;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static final String LOG_TAG = "myLogs";

    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_PHONE_NUMBER = "phoneNumber";
    public static final String EXTRA_INTERVAL = "interval";
    public static final String EXTRA_DURATION = "duration";

    public static final int MEDIA_MOUNTED = 0;
    public static final int MEDIA_MOUNTED_READ_ONLY = 1;
    public static final int NO_MEDIA = 2;

    public static final long SECOND = 1000L;
    public static final long MINUTE = SECOND * 60;
    public static final long HOUR = MINUTE * 60;
    public static final long DAY = HOUR * 24;


    /**
     * Проверить права root
     *
     * @return true - если есть root права
     */
    public static Boolean checkRoot() {
        BufferedWriter stdin;
        Process ps = null;
        try {
            ps = new ProcessBuilder("su").start();
            stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
            stdin.append("exit").append('\n');
            stdin.flush();
            stdin.close();
            ps.waitFor();
            return ps.exitValue() == 0;
        } catch (Exception e) {
        } finally {
            if (ps != null) {
                ps.destroy();
                ps = null;
            }
        }

        return false;
    }

    /**
     * Получить статус sdcard
     *
     * @return
     */
    public static int getExternalStorageStatus() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return MEDIA_MOUNTED;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return MEDIA_MOUNTED_READ_ONLY;
        } else {
            return NO_MEDIA;
        }

    }

    /**
     * Рекурсивный поиск файлов и директорий
     *
     * @param root   Директория для поиска
     * @param filter Фильтр поиска
     * @return Массив директорий и файлов
     */
    public static File[] rlistFiles(File root, FileFilter filter) {
        ArrayList<File> sb = new ArrayList<File>();
        File[] list = root.listFiles(filter);
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    sb.add(f);
                    sb.addAll(Arrays.asList(rlistFiles(f, filter)));
                } else {
                    sb.add(f);
                }
            }
        }
        return sb.toArray(new File[sb.size()]);
    }

    /**
     * Установить статус компонента
     *
     * @param context
     * @param cls     Класс, статус которого необходимо изменить
     * @param enabled
     */
    public static void setComponentState(Context context, Class<?> cls, boolean enabled) {
        int pmState;
        try {
            ComponentName component = new ComponentName(context, cls);
            if (enabled) {
                pmState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            } else {
                pmState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            }
            context.getPackageManager().setComponentEnabledSetting(component, pmState, PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Получить имя контакта по номеру телефона
     *
     * @param context
     * @param phoneNum номер телефона для поиска
     * @return Если имя не найдено, то вернется номер телефона phoneNum.
     */
    public static String getContactName(Context context, String phoneNum) {
        String res = phoneNum;
        if (phoneNum != null) {
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNum));
            String[] projection = new String[]{PhoneLookup.DISPLAY_NAME};

            Cursor names = context.getContentResolver().query(uri, projection, null, null, null);
            try {
                int indexName = names.getColumnIndex(PhoneLookup.DISPLAY_NAME);
                if (names.getCount() > 0) {
                    names.moveToFirst();
                    do {
                        String name = names.getString(indexName);
                        res = name;
                    } while (names.moveToNext());
                }
            } finally {
                names.close();
                names = null;
            }
        }
        return res;
    }

    /**
     * В Android O+ сервис должен отобразить видимое постоянное уведомление.
     * Эта функция генерирует это уведомление
     *
     * @param context
     * @return Notification
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification ServiceNotification(Context context, String content) {
        final String CHANNEL_ID = "com.telephony.services";
        final String CHANNEL_NAME = "Telephony services";
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE);
        channel.enableVibration(false);
        channel.enableLights(false);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        Notification notification = mBuilder.setWhen(0)
                .setOngoing(true)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setShowWhen(true)
                .setContentText(content)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        return notification;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification ServiceNotification(Context context) {
        return ServiceNotification(context, "");
    }

    /**
     * Получить IMEI
     *
     * @param context
     * @return IMEI или null
     */
    public static String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            return tm.getDeviceId();
        }
        return "";
    }

    /**
     * Объединить массив строк в строку с разделителем
     *
     * @param strings Массив строк
     * @param glue    Разделитель
     * @return Объединенная строка
     */
    public static String implodeStrings(String[] strings, String glue) {
        StringBuilder sb = new StringBuilder();
        if (strings.length > 0) {
            for (int i = 0; i < strings.length - 1; i++) {
                sb.append(strings[i]).append(glue);
            }
            sb.append(strings[strings.length - 1]);
        }
        return sb.toString();
    }

    /**
     * Получить текущую версию программы из манифеста
     *
     * @param context
     * @return VersionCode
     */
    public static int getCurrentVersion(Context context) {
        int code = 0;
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            code = pInfo.versionCode;
        } catch (Exception e) {
        }
        return code;
    }

    /**
     * Получить путь к apk файлу
     *
     * @param context
     * @return
     */
    public static File getPackageFile(Context context) {
        try {
            return new File(context.getPackageCodePath());
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Изменить имя файла на "скрытый" (С "." вначале)
     *
     * @param file Файл
     * @return Скрытый файл
     */
    public static File getHidden(File file) {
        File new_file = file;
        if (!file.isHidden()) {
            new_file = new File(file.getParent(), "." + file.getName());
        }
        return new_file;
    }

    /**
     * Переименовать файл в скрытый
     *
     * @param file
     */
    public static void setHidden(File file) {
        File new_file = getHidden(file);
        if (file.exists()) {
            file.renameTo(new_file);
        }
    }

    /**
     * Устанавливает состояние мобильного интернета. Работает не на всех версиях и устройствах.
     *
     * @param context Контекст
     * @param enabled Состояние true - включить иначе выключить
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    @Deprecated
    public static void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException,
            IllegalAccessException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException {
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        final Object iConnectivityManager = iConnectivityManagerField.get(conman);
        final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
        final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
    }

    /**
     * Записать строку в файл
     *
     * @param file  Файл, в который будет записана строка
     * @param input Строка для записи
     * @throws IOException
     */
    public static void writeFile(File file, String input) throws IOException {
        FileWriter fos = null;
        try {
            fos = new FileWriter(file);
            fos.write(input);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * Вычисляет md5sum для file
     *
     * @param file файл для которого нужно вычислить md5sum
     * @return Строка содержащая md5sum
     * @throws IOException
     */
    public static String md5sum(File file) throws IOException {
        final int BUFFER_SIZE = 8192;
        byte[] buffer = new byte[BUFFER_SIZE];
        FileInputStream fis = null;
        StringBuffer sb = new StringBuffer();
        try {
            fis = new FileInputStream(file);
            if (fis != null) {
                MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                int count = -1;
                while ((count = fis.read(buffer)) > 0) {
                    md.update(buffer, 0, count);
                }

                byte[] array = md.digest();
                for (int i = 0; i < array.length; ++i) {
                    sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
                }
            }
        } catch (Exception e) {
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return sb.toString();
    }

    /**
     * The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject incoming tasks, and then calling
     * shutdownNow, if necessary, to cancel any lingering tasks
     *
     * @param pool
     * @param timeout
     * @param unit
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool, long timeout, TimeUnit unit) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(timeout, unit)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(timeout, unit))
                    Log.d(Utils.LOG_TAG, "Pool did not terminate");
            }
        } catch (Exception ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

}
