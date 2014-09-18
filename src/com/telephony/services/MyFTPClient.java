package com.telephony.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPClient;

public class MyFTPClient extends FTPClient {

	protected URL url;
	protected boolean isAuthorized = false;

	public MyFTPClient() {
		isAuthorized = false;
		setControlEncoding("UTF8");
		setAutodetectUTF8(true);
	}

	public void connect(String surl) throws SocketException, IOException, MalformedURLException {
		String username = "";
		String password = "";
		url = new URL(surl);
		String authority = url.getUserInfo();
		if (authority != null) {
			String[] auth = authority.split(":");
			username = auth[0];
			password = auth[1];
		}
		super.connect(url.getHost(), url.getPort());
		enterLocalPassiveMode();
		isAuthorized = super.login(username, password);
	}

	public void mkdirs(String dir) throws IOException {
		String[] dirs = dir.split(File.separator);
		try {
			for (String d : dirs) {
				if (!d.equals("")) {
					if (!changeWorkingDirectory(d)) {
						if (!makeDirectory(d)) {
							throw new IOException("Unable to create remote directory '" + d + "'.  error='" + getReplyString() + "'");
						}
						if (!super.changeWorkingDirectory(d)) {
							throw new IOException("Unable to change into newly created remote directory '" + d + "'.  error='" + getReplyString()
									+ "'");
						}
					}
				}
			}
		} finally {
			changeWorkingDirectory(File.separator);
		}
	}

	public boolean uploadFile(File root_dir, File file) throws IOException {
		setFileType(FTPClient.BINARY_FILE_TYPE);
		FileInputStream in = new FileInputStream(file);
		try {
			String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), url.getPath());
			mkdirs(remote_dir);
			return storeFile(remote_dir + File.separator + file.getName(), in);
		} finally {
			in.close();
		}

	}

	public void setHidden(File file) {
		if (!file.getName().startsWith(".")) {
			file.renameTo(new File(file.getParent() + File.separator + "." + file.getName()));
		}
	}

	@Override
	public void disconnect() throws IOException {
		super.disconnect();
		isAuthorized = false;
	}

}
