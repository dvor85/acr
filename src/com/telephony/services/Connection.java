package com.telephony.services;

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Connection {

	private Context context;
	private static Connection sInstance;

	/**
	 * ����� ��� �������� ����������
	 * 
	 * @author Dmitriy
	 */
	private class ConnectionThread extends Thread {
		private boolean wifiOnly;

		public ConnectionThread(boolean wifiOnly) {
			this.wifiOnly = wifiOnly;
			setName("WaitForConnection");
			start();
		}

		@Override
		public void run() {
			try {
				while (!isConnected(wifiOnly)) {
					synchronized (sInstance) {
						sInstance.wait();
					}
				}
			} catch (InterruptedException ie) {
			}
		}
	}

	/**
	 * �������� ������ �� ������
	 * 
	 * @param context
	 *            ��������
	 * @param wifiOnly
	 *            true - ���� ����� ������ wifi ����
	 * @return ������ ������ Connection
	 */
	public static final Connection getInstance(Context context) {
		if (sInstance == null) {
			synchronized (Connection.class) {
				if (sInstance == null) {
					sInstance = new Connection(context);
				}
			}
		}
		return sInstance;
	}

	private Connection(Context context) {
		this.context = context;
	}

	/**
	 * �������� ������� ��������� ����
	 * 
	 * @return true ���� ���� ����������� � ����, ����� false
	 */
	public synchronized boolean isConnected(boolean wifiOnly) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
		return false;
	}

	/**
	 * ����� ����������� � ����<br>
	 * ������������ � ��������� ������<br>
	 * ����������� �������� ���������� ���� �� ��������, ���� ���� ����� ������� ����� notify() �� ���� ���������� ��� interrupt() �� ���� ������
	 * 
	 * @param timeout
	 *            - ����� ��������
	 * @param unit
	 *            - ������� ��������� timeout
	 * @return ���� ����������� ��������� � ������� timeout, �� true, ����� false
	 */
	public boolean waitForConnection(final boolean wifiOnly, long timeout, TimeUnit unit) {

		ConnectionThread connThread = new ConnectionThread(wifiOnly);

		try {
			unit.timedJoin(connThread, timeout);
			connThread.interrupt();
		} catch (InterruptedException ie) {
		}

		return isConnected(wifiOnly);
	}

}
