package com.telephony.services;


import android.net.Uri;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

public class MyWebdavClient {

    private static MyWebdavClient sInstance;
    private boolean isReady = false;
    private Sardine sardine;
    private OkHttpClient customHttp;
    private Uri baseUrl;
    private String basePath;


    public static final MyWebdavClient getInstance() {
        if (sInstance == null) {
            synchronized (MyWebdavClient.class) {
                if (sInstance == null) {
                    sInstance = new MyWebdavClient();
                }
            }
        }
        return sInstance;
    }

    private MyWebdavClient() {
        isReady = false;
    }

    private Uri true_uri(String path) {
        path = Uri.encode(path.replaceAll("/+", "/"), "/");
        return Uri.withAppendedPath(baseUrl, path);
    }

    private Uri webdavPath(String rel_path) {
        rel_path = rel_path.replaceFirst(basePath, "/");
        String wPath = new File(basePath, rel_path).toString();
        return true_uri(wPath);

    }

    private OkHttpClient getCustomHttp() {
        final OkHttpClient client;
        ConnectionSpec.Builder connectionSpecBuilder = new ConnectionSpec.Builder(true)
                .allEnabledCipherSuites()
                .allEnabledTlsVersions();

        ConnectionSpec connectionSpec = connectionSpecBuilder.build();
        final List<ConnectionSpec> specs = new ArrayList<>();
        specs.add(connectionSpec);

        client = new OkHttpClient.Builder()
                .connectionSpecs(specs)
                .build();
        return client;
    }

    /**
     * Готово ли соединение к передаче данных
     *
     * @return
     */
    public synchronized boolean isReady() {
        return isReady;
    }

    /**
     * Подключиться к серверу webdav, залогиниться
     *
     * @param uri Ссылка к серверу с авторизационными данными
     * @return
     * @throws IOException
     * @throws SocketException
     */
    public synchronized boolean connect(Uri uri) {
        if (uri != null && !isReady) {
            String username = "";
            String password = "";
            String wScheme = uri.getScheme();
            String wHost = uri.getHost();
            int wPort = uri.getPort();

            basePath = uri.getPath();
            if (basePath == null || basePath.isEmpty()) {
                basePath = "/";
            }
            if (basePath.endsWith("/")) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
            if (wPort > 0) {
                baseUrl = Uri.parse(wScheme + "://" + wHost + ":" + wPort);
            } else {
                baseUrl = Uri.parse(wScheme + "://" + wHost);
            }

            String authority = uri.getUserInfo();
            if (authority != null) {
                String[] auth = authority.split(":");
                username = auth[0];
                password = auth[1];
            }
            sardine = new OkHttpSardine();
            sardine.setCredentials(username, password, true);
            isReady = true;
        }
        return isReady;
    }

    /**
     * Рекурсивно создает директории на webdav сервере
     *
     * @param remote_uri
     * @throws IOException
     */
    private boolean mkdirs(Uri remote_uri) throws IOException {
        File fdir = new File(remote_uri.getPath());
        String head = null, tail = null;

        if (!sardine.exists(remote_uri.toString())) {
            head = fdir.getParent();
            tail = fdir.getName();
            if (head != null && tail != null) {
                if (mkdirs(true_uri(head))) {
                    try {
                        sardine.createDirectory(remote_uri.toString());
                        return true;
                    } catch (IOException e) {
                        throw new IOException("Unable to create remote directory '" + remote_uri.toString() + "'");
                    }
                }
            }
        }
        return true;
    }

    /**
     * Возвращает имя файла на webdav сервере в который нужно закачать файл
     *
     * @param root_dir - корневая директория программы
     * @param file     - Файл для которого необходимо получить имя удаленного файла
     * @return
     */
    public Uri getRemoteFile(File root_dir, File file) {
        String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), basePath);
        String rf = new File(remote_dir, file.getName()).toString();
        return true_uri(rf);

    }

    /**
     * Возвращает файл в который будет закачан удаленный файл
     *
     * @param root_dir   - корневая директория программы
     * @param remotefile - удаленный файл
     * @return
     */
    public File getLocalFile(File root_dir, Uri remotefile) {
        File local_dir = null;
        File rf;
        rf = new File(remotefile.getPath());
        if (!rf.getParent().isEmpty()) {
            local_dir = new File(rf.getParent().replaceFirst(basePath, root_dir.getAbsolutePath()));
        } else {
            local_dir = root_dir;
        }
        if (!local_dir.exists()) {
            local_dir.mkdirs();
        }
        File local_file = new File(local_dir, rf.getName());
        return local_file;
    }

    public File getLocalFile(File root_dir, String remotefile) {
        return getLocalFile(root_dir, webdavPath(remotefile));
    }

    public boolean uploadFile(File local_file, Uri remotefile) throws IOException {
        String remote_dir = new File(remotefile.getPath()).getParent();

        if (isReady() && !local_file.isDirectory()) {
            if (mkdirs(true_uri(remote_dir))) {
                sardine.put(remotefile.toString(), local_file, "application/octet-stream");
                return true;
            }
        }
        return false;
    }

    public boolean uploadFile(File local_file, String remotefile) throws IOException {
        return uploadFile(local_file, webdavPath(remotefile));
    }

    /**
     * Размер удаленного файла
     *
     * @param remotefile Удаленный файл
     * @return
     * @throws IOException
     */
    public long getFileSize(Uri remotefile) throws IOException {
        long fileSize = -1;
        final List<DavResource> resources;

        if (isReady() && sardine.exists(remotefile.toString())) {
            resources = sardine.list(remotefile.toString());
            fileSize = resources.get(0).getContentLength();
        }
        return fileSize;
    }

    public long getFileSize(String remotefile) throws IOException {
        return getFileSize(webdavPath(remotefile));
    }

    /**
     * Скачать файл remotefile в файл local_file
     *
     * @param remotefile
     * @param local_file
     * @return
     * @throws IOException
     */
    public boolean downloadFile(Uri remotefile, File local_file) throws IOException {
        FileOutputStream out = null;
        InputStream in = null;
        byte[] buffer = new byte[1024];
        try {
            if (getFileSize(remotefile) > 0) {
                out = new FileOutputStream(local_file);
                in = sardine.get(remotefile.toString());
                if (in != null) {
                    int count = -1;
                    while ((count = in.read(buffer)) > 0) {
                        out.write(buffer, 0, count);
                    }
                    in.close();
                }
                return true;
            }
            return false;
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }

        }
    }

    public boolean downloadFile(String remotefile, File local_file) throws IOException {
        return downloadFile(webdavPath(remotefile), local_file);
    }

    /**
     * Скачавает удаленный файл remotefile в массив строк
     *
     * @param remotefile Удаленный файл
     * @return Массив строк
     * @throws IOException
     */
    public String[] downloadFileStrings(Uri remotefile) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder res = new StringBuilder();
        InputStream in = null;
        File tmp = File.createTempFile("acr", ".tmp");
        try {
            if (downloadFile(remotefile, tmp) && tmp.length() > 0) {
                in = new FileInputStream(tmp);
                if (in != null) {
                    int count = -1;
                    while ((count = in.read(buffer)) > 0) {
                        res.append(new String(buffer).substring(0, count));
                    }
                    in.close();
                }
            }
            return res.toString().split("[ \r]*\n+");
        } finally {
            if (in != null) {
                in.close();
            }
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    public String[] downloadFileStrings(String remotefile) throws IOException {
        return downloadFileStrings(webdavPath(remotefile));
    }


}
