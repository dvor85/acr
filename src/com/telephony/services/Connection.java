package com.telephony.services;

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Connection {

	private Context context;
	private static Connection sInstance;

	/**
	 * Поток для ожидания соединения
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
	 * Получить ссылку на объект
	 * 
	 * @param context
	 *            Контекст
	 * @param wifiOnly
	 *            true - если нужен только wifi сеть
	 * @return Объект класса Connection
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
	 * Получить текущее состояние сети
	 * 
	 * @return true если есть подключение к сети, иначе false
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
	 * Ждать подключение к сети<br>
	 * Запускаеться в отдельном потоке<br>
	 * Прекращение ожидания происходит либо по таймауту, либо если поток вызовет метод notify() на этом экземпляре или interrupt() на этом потоке
	 * 
	 * @param timeout
	 *            - Время ожидания
	 * @param unit
	 *            - Единица измерения timeout
	 * @return Если подключение появиться в течении timeout, то true, иначе false
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
