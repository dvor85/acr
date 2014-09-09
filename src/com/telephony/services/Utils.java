package com.telephony.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;

public class Utils {
	public static Boolean CheckRoot() {
		BufferedWriter stdin;
		Process ps = null;
		Boolean res = false;
		try {
			ps = new ProcessBuilder("su").start();
			stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
			stdin.append("exit").append('\n');
			stdin.flush();
			stdin.close();
			ps.waitFor();
			res = ps.exitValue() == 0;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
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

	public static class KillProc {
		private String shell;

		public KillProc(String shell) {
			this.shell = shell;
		}

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
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return sb.toString();
		}

		public void killTree(String ppid) {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process ps;
			String cpid;
			try {
				ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				cpid = getChilds(ppid);
				stdin.append("kill -9 " + ppid + " " + cpid).append('\n');
				stdin.flush();
				stdin.close();
				ps.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

		public void kill(String pid) {
			// убить процесс и все дочерние процессы
			// делать это с правами запущенного процесса. Process.destroy
			// убивает только с пользовательскими правами
			BufferedWriter stdin;
			Process ps;
			try {
				ps = new ProcessBuilder(shell).redirectErrorStream(true).start();
				stdin = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));
				stdin.append("kill -9 " + pid).append('\n');
				stdin.flush();
				stdin.close();
				ps.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}
}
