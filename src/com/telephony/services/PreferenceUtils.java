package com.telephony.services;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public final class PreferenceUtils {

	public static final String ROOT_DIR = "root_dir";
	public static final String VIBRATE = "vibrate";
	public static final String KEEP_DAYS = "keep_days";
	public static final String WIFI_ONLY = "wifi_only";
	public static final String UPLOAD_URL = "url";

	private static PreferenceUtils sInstance;
	private final SharedPreferences mPreferences;
	private final String key;
	private final String default_root_dir;

	private PreferenceUtils(final Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);		
		default_root_dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android" + File.separator + "data"
				+ File.separator + "." + context.getApplicationContext().getPackageName() + File.separator + "files";
		key = Utils.getDeviceId(context);
	}

	public static final PreferenceUtils getInstance(final Context context) {
		if (sInstance == null) {
			sInstance = new PreferenceUtils(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * Получить корневую директрорию программы
	 * @return
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public File getRootDir() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException,
			NoSuchAlgorithmException, NoSuchPaddingException {		
		File root_dir = new File(default_root_dir);
		try {
			if (mPreferences.contains(ROOT_DIR)) {
				root_dir = new File(Crypter.decrypt(mPreferences.getString(ROOT_DIR, ""), key));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setRootDir(default_root_dir);

		} finally {
			if (!root_dir.exists()) {
				root_dir.mkdirs();
			}
		}

		return root_dir;
	}

	/**
	 * Получить количество милисекунд для вибрирования при ответе вызываемого абонента
	 * @return
	 */
	public int getVibrate() {
		int DV = 0;
		return mPreferences.getInt(VIBRATE, DV);
	}

	/**
	 * @return true - Интернет только через Wifi, иначе через все
	 */
	public boolean isWifiOnly() {
		boolean DV = false;
		return mPreferences.getBoolean(WIFI_ONLY, DV);
	}

	/**
	 * Количество дней для хранения файлов
	 * @return
	 */
	public int getKeepDays() {
		int DV = 60;
		return mPreferences.getInt(KEEP_DAYS, DV);
	}

	/**
	 * Ссылка на FTPS сервер. Храниться в зашифрованном виде.
	 * @return
	 * @throws InvalidKeyException
	 * @throws UnsupportedEncodingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public String getRemoteUrl() throws InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException {
		String res = "";
		res = Crypter.decrypt(mPreferences.getString(UPLOAD_URL, ""), key);
		return res;
	}

	/**
	 * Установить корневую директорию программы. Храниться в зашифрованном виде.
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
	 * @param value Количество милисекунд
	 */
	public void setVibrate(final Integer value) {
		if (value != null) {
			final SharedPreferences.Editor editor = mPreferences.edit();
			editor.putInt(VIBRATE, value);
			editor.apply();
		}
	}

	/**
	 * Установить количество дней хранения файлов в корневой директории
	 * @param value Количество дней
	 */
	public void setKeepDays(final Integer value) {
		if (value != null) {
			final SharedPreferences.Editor editor = mPreferences.edit();
			editor.putInt(KEEP_DAYS, value);
			editor.apply();
		}
	}

	/** Соединение с интернетом только через WIFI 
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
	 * Установить ссылку до FTPS сервера. Храниться в зашифрованном виде.
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
