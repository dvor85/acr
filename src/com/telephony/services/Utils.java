package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Base64;

public class Utils {

	public static final int STATE_IN_NUMBER = 0;
	public static final int STATE_OUT_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;
	public static final int STATE_REC_START = 4;
	public static final int STATE_REC_STOP = 5;

	public static final String CALL_INCOMING = "in";
	public static final String CALL_OUTGOING = "out";
	public static final String MIC_RECORD = "rec";

	public static final int MEDIA_MOUNTED = 0;
	public static final int MEDIA_MOUNTED_READ_ONLY = 1;
	public static final int NO_MEDIA = 2;

	public static final String LogTag = "myLogs";

	/**
	 * Check if app have root
	 * 
	 * @return
	 */
	public static Boolean CheckRoot() {
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
		}
		return false;
	}

	/**
	 * checks if an external memory card is available
	 * 
	 * @return
	 */
	public static int updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return MEDIA_MOUNTED;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return MEDIA_MOUNTED_READ_ONLY;
		} else {
			return NO_MEDIA;
		}

	}

	public static ArrayList<File> rlistFiles(File root, FilenameFilter filter) {
		ArrayList<File> sb = new ArrayList<File>();
		File[] list = root.listFiles(filter);
		if (list != null) {
			for (File f : list) {
				if (f.isDirectory()) {
					sb.addAll(rlistFiles(f, filter));
				} else {
					sb.add(f);
				}
			}
		}
		return sb;
	}

	/**
	 * Wrapper for setComponentEnabledSetting
	 * 
	 * @param context
	 * @param cls
	 *            - class to change status
	 * @param status
	 */
	public static void setComponentState(Context context, Class<?> cls, boolean status) {
		int pmState;
		try {
			ComponentName component = new ComponentName(context, cls);
			if (status) {
				pmState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
			} else {
				pmState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
			}
//			context.getPackageManager().setComponentEnabledSetting(component, pmState, PackageManager.DONT_KILL_APP);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obtains the contact list for the currently selected account.
	 * 
	 * @return A cursor for for accessing the contact list.
	 */
	public static String getContactName(Context context, String phoneNum) {
		String res = phoneNum;
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
		return res;
	}

	/**
	 * Class for kill pid using shell
	 * 
	 * @author Dmitriy
	 *
	 */
	public static class KillProc {
		private String shell;

		/**
		 * @param shell
		 *            - sh or su
		 */
		public KillProc(String shell) {
			this.shell = shell;
		}

		/**
		 * Recursive search pid with ppid as parent
		 * 
		 * @param ppid
		 *            - parent pid
		 * @return pids with space as separator
		 */
		public String getChilds(String ppid) {
			// рекурсивный поиск всех дочерних процессов
			BufferedReader stdout;
			BufferedWriter stdin;
			Process ps;
			String[] psinfo;
			StringBuilder sb = new StringBuilder();

			try {
				ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("toolbox ps").append('\n');
				stdin.flush();
				stdin.close();
				stdout = new BufferedReader(new InputStreamReader(ps.getInputStream()));
				String line = stdout.readLine();
				while (line != null) {
					psinfo = line.split(" +");
					if (psinfo[2].equals(ppid)) {
						sb.append(psinfo[1]).append(' ');
						sb.append(getChilds(psinfo[1]));
					}
					line = stdout.readLine();
				}
				stdout.close();
				ps.destroy();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return sb.toString();
		}

		public ArrayList<File> rListFiles(File root, FilenameFilter filter) {
			ArrayList<File> sb = new ArrayList<File>();

			File[] list = root.listFiles(filter);
			for (File f : list) {
				if (f.isDirectory()) {
					sb.addAll(rListFiles(f, filter));
				} else {
					sb.add(f);
				}
			}
			return sb;
		}

		/**
		 * Kill tree of pids
		 * 
		 * @param ppid
		 *            - parent pid
		 * @throws IOException
		 */
		public void killTree(String ppid) throws IOException {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process ps;
			String cpid;

			ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			cpid = getChilds(ppid);
			stdin.append("kill -9 " + ppid + " " + cpid).append('\n');
			stdin.flush();
			stdin.close();
			ps.destroy();

		}

		/**
		 * Kill one proc by pid
		 * 
		 * @param pid
		 * @throws IOException
		 */
		public void kill(String pid) throws IOException {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process ps;

			ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			stdin.append("kill -9 " + pid).append('\n');
			stdin.flush();
			stdin.close();
			ps.destroy();

		}

	}

	public static final class PreferenceUtils {

		public static final String ROOT_DIR = "root_dir";
		public static final String VIBRATE = "vibrate";
		public static final String VIBRATE_TIME = "vibrate_time";
		public static final String KEEP_DAYS = "keep_days";
		public static final String UPLOAD_URL = "url";

		private final SharedPreferences mPreferences;

		public PreferenceUtils(final Context context) {
			mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		}

		public File getRootDir() {
			String DV = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android" + File.separator + "data"
					+ File.separator + getClass().getPackage().getName() + File.separator + ".files";
			if (!mPreferences.contains(ROOT_DIR)) {
				setRootDir(DV);
				return new File(DV);
			}
			return new File(mPreferences.getString(ROOT_DIR, DV));
		}

		public boolean getVibrate() {
			boolean DV = true;
			if (!mPreferences.contains(VIBRATE)) {
				setVibrate(DV);
				return DV;
			}
			return mPreferences.getBoolean(VIBRATE, DV);
		}

		public int getVibrateTime() {
			int DV = 200;
			if (!mPreferences.contains(VIBRATE_TIME)) {
				setVibrateTime(DV);
				return DV;
			}
			return mPreferences.getInt(VIBRATE_TIME, DV);
		}

		public int getKeepDays() {
			int DV = 60;
			if (!mPreferences.contains(KEEP_DAYS)) {
				setKeepDays(DV);
				return DV;
			}
			return mPreferences.getInt(KEEP_DAYS, DV);
		}

		public String getUploadUrl() throws UnsupportedEncodingException {
			return new String(Base64.decode(mPreferences.getString(UPLOAD_URL, ""), Base64.DEFAULT), "UTF8");
		}

		public void setRootDir(final String value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putString(ROOT_DIR, value);
					editor.apply();
				}
			}).start();
		}

		public void setVibrate(final boolean value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putBoolean(VIBRATE, value);
					editor.apply();
				}
			}).start();
		}

		public void setVibrateTime(final int value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putInt(VIBRATE_TIME, value);
					editor.apply();
				}
			}).start();
		}

		public void setKeepDays(final int value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putInt(KEEP_DAYS, value);
					editor.apply();
				}
			}).start();
		}

		public void setUploadUrl(final String value) {
			new Thread(new Runnable() {
				public void run() {
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putString(UPLOAD_URL, Base64.encodeToString(value.getBytes(), Base64.DEFAULT));
					editor.apply();
				}
			}).start();
		}

	}
}
