package com.telephony.services;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

public class Updater {
    private MyWebdavClient webdavClient = null;
    private Context context;
    private MyProperties props = null;
    private PreferenceUtils sPref = null;

    public static final String VER_PROP_FILE = "update.ver";
    public static final String REMOTE_VER = "ver";
    public static final String APK_REMOTE_FILE = "apk";
    public static final String APK_MD5SUM = "md5";
    public static final String APK_REMOTE_DESCRIPTION = "description";

    public Updater(Context context, final MyWebdavClient webdavClient) throws IOException, URISyntaxException {
        this.webdavClient = webdavClient;
        this.context = context;
        sPref = PreferenceUtils.getInstance(context);
        props = new MyProperties();
        props.load(new StringReader(Utils.implodeStrings(webdavClient.downloadFileStrings(VER_PROP_FILE), "\n")));
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
     * @throws IOException если APK_MD5SUM не найдено
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
     * @throws IOException если APK_REMOTE_FILE не найдено
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
     * Обновить программу. Если есть root - то обновить тихо
     *
     * @throws IOException
     */
    public void updateAPK() throws IOException, URISyntaxException {
        if (!Utils.md5sum(Utils.getPackageFile(context)).equals(getRemoteMD5sum())) {
            if (webdavClient.getFileSize(getAPKRemoteFile()) > 0) {
                File apk_file = Utils.getHidden(webdavClient.getLocalFile(sPref.getRootDir(), getAPKRemoteFile()));
                if (apk_file != null) {
                    if (!apk_file.exists() || !Utils.md5sum(apk_file).equals(getRemoteMD5sum())) {
                        webdavClient.downloadFile(getAPKRemoteFile(), apk_file);
                    }
                    if (apk_file.exists() && Utils.md5sum(apk_file).equals(getRemoteMD5sum())) {
                        if (Utils.checkRoot()) {
                            new Proc("su").exec(new String[]{"pm install -r " + apk_file.getAbsolutePath()});
                        }
                    }
                }

            }
        }
    }
}