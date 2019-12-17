package com.telephony.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Connection {

    private Context context;
    private static Connection sInstance;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private PreferenceUtils sPref;


    /**
     * Поток для ожидания соединения
     *
     * @author Dmitriy
     */
    private class ConnectionThread extends Thread {

        public ConnectionThread() {
            setName("WaitForConnection");
            start();
        }

        @Override
        public void run() {
            try {
                while (!isConnected.get()) {
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
     * @param context Контекст
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

    /**
     * @param context
     */
    private Connection(Context context) {
        this.context = context;
        sPref = PreferenceUtils.getInstance(context);

        ConnectivityManager.NetworkCallback network_callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(Utils.LOG_TAG, "Network is Available!");
                isConnected.set(true);
                synchronized (this) {
                    this.notifyAll();
                }
            }

            @Override
            public void onLost(Network network) {
                Log.d(Utils.LOG_TAG, "Network is Lost!");
                isConnected.set(false);
            }
        };
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder requestbuilder = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        if (!sPref.isWifiOnly()) {
            requestbuilder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        connectivityManager.registerNetworkCallback(requestbuilder.build(), network_callback);
    }

    /**
     * Получить текущее состояние сети или подождать это состояние секунду
     *
     * @return true если есть подключение к сети, иначе false
     */
    public boolean isConnected() {
        return waitForConnection(1, TimeUnit.SECONDS);
    }

    /**
     * Ждать подключение к сети<br>
     * Запускаеться в отдельном потоке<br>
     * Прекращение ожидания происходит либо по таймауту, либо если поток вызовет метод notify() на этом экземпляре или interrupt() на этом потоке
     *
     * @param timeout - Время ожидания
     * @param unit    - Единица измерения timeout
     * @return Если подключение появиться в течении timeout, то true, иначе false
     */
    public boolean waitForConnection(long timeout, TimeUnit unit) {
        ConnectionThread connThread = new ConnectionThread();
        try {
            unit.timedJoin(connThread, timeout);
            connThread.interrupt();
        } catch (InterruptedException ie) {
        }
        return isConnected.get();
    }

}
