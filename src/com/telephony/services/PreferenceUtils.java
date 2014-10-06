package com.telephony.services;

import java.io.File;

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

	private PreferenceUtils(final Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static final PreferenceUtils getInstance(final Context context) {
		if (sInstance == null) {
			sInstance = new PreferenceUtils(context.getApplicationContext());
		}
		return sInstance;
	}

	public File getRootDir() {
		String DV = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android"
				+ File.separator + "data" + File.separator + "." + getClass().getPackage().getName() + File.separator
				+ "files";
		File root_dir = new File(DV);

		try {
			if (mPreferences.contains(ROOT_DIR)) {
				root_dir = new File(Crypter.decrypt(mPreferences.getString(ROOT_DIR, "")));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setRootDir(DV);

		} finally {
			if (!root_dir.exists()) {
				root_dir.mkdirs();
			}
		}

		return root_dir;
	}

	public int getVibrate() {
		int DV = 0;
		return mPreferences.getInt(VIBRATE, DV);
	}
	
	public boolean isWifiOnly() {
		boolean DV = false;
		return mPreferences.getBoolean(WIFI_ONLY, DV);
	}

	public int getKeepDays() {
		int DV = 60;
		return mPreferences.getInt(KEEP_DAYS, DV);
	}

	public String getRemoteUrl() {
		String res = "";
		try {
			res = Crypter.decrypt(mPreferences.getString(UPLOAD_URL, ""));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public void setRootDir(final String value) {
		if (value != null) {
			new Thread(new Runnable() {
				public void run() {
					try {
						final SharedPreferences.Editor editor = mPreferences.edit();
						editor.putString(ROOT_DIR, Crypter.encrypt(value));
						editor.apply();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public void setVibrate(final Integer value) {
		if (value != null) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putInt(VIBRATE, value);
					editor.apply();
				}
			}).start();
		}
	}

	public void setKeepDays(final Integer value) {
		if (value != null) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putInt(KEEP_DAYS, value);
					editor.apply();
				}
			}).start();
		}
	}
	
	public void setWifiOnly(final Boolean value) {
		if (value != null) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putBoolean(WIFI_ONLY, value);
					editor.apply();
				}
			}).start();
		}
	}

	public void setRemoteUrl(final String value) {
		if (value != null) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					try {
						String crStr = Crypter.encrypt(value);
						editor.putString(UPLOAD_URL, crStr);
						editor.apply();
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}).start();
		}
	}

}
