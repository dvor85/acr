package com.telephony.services;


import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

public class MyWebdavClient {

    private boolean isAuthorized = false;
    private static MyWebdavClient sInstance;
    private boolean isReady = false;
    private Sardine sardine;
    private OkHttpClient customHttp;
    private URI baseUrl;
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

    private URI webdavPath(String rel_path) throws URISyntaxException {
        rel_path = rel_path.replaceFirst(basePath, "/");
        String wPath = new File(basePath, rel_path).toString();
        return new URI(baseUrl + wPath);

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

    private MyWebdavClient() {
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
    public synchronized boolean connect(URI uri) {
        String username = "";
        String password = "";
        String wScheme = uri.getScheme();
        String wHost = uri.getHost();
        int wPort = uri.getPort();

        basePath = uri.getPath();
        if (!basePath.isEmpty() && basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (basePath.isEmpty()) {
            basePath = "/";
        }
        try {
            baseUrl = new URI(wScheme + "://" + wHost + ":" + wPort);
        } catch (URISyntaxException e) {
        }
        if (!isReady()) {
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
        return isReady();
    }

    /**
     * Рекурсивно создает директории на webdav сервере
     *
     * @param remote_uri
     * @throws IOException
     */
    public synchronized boolean mkdirs(URI remote_uri) throws IOException, URISyntaxException {
        File fdir = new File(remote_uri.getPath());
        String head = null, tail = null;

        if (!sardine.exists(remote_uri.toString())) {
            head = fdir.getParent();
            tail = fdir.getName();
            if (head != null && tail != null) {
                if (mkdirs(new URI(baseUrl + head))) {
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
    public URI getRemoteFile(File root_dir, File file) throws URISyntaxException {
        String remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), basePath);
        String rf = new File(remote_dir, file.getName()).toString();
        return new URI(baseUrl + rf);

    }

    /**
     * Возвращает файл в который будет закачан удаленный файл
     *
     * @param root_dir   - корневая директория программы
     * @param remotefile - удаленный файл
     * @return
     */
    public File getLocalFile(File root_dir, URI remotefile) {
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

    public File getLocalFile(File root_dir, String remotefile) throws URISyntaxException {
        return getLocalFile(root_dir, webdavPath(remotefile));
    }

    public synchronized boolean uploadFile(File local_file, URI remotefile) throws IOException, URISyntaxException {
        String remote_dir = new File(remotefile.getPath()).getParent();

        if (local_file.isDirectory()) {
            return false;

        } else {
            if (mkdirs(new URI(baseUrl + remote_dir))) {
                sardine.put(remotefile.toString(), local_file, "application/octet-stream");
                return true;
            }
            return false;
        }
    }

    public synchronized boolean uploadFile(File local_file, String remotefile) throws IOException, URISyntaxException {
        return uploadFile(local_file, webdavPath(remotefile));
    }

    /**
     * Скачать файл remotefile в файл local_file
     *
     * @param remotefile
     * @param local_file
     * @return
     * @throws IOException
     */
    public synchronized boolean downloadFile(URI remotefile, File local_file) throws IOException {
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

    public synchronized boolean downloadFile(String remotefile, File local_file) throws IOException, URISyntaxException {
        return downloadFile(webdavPath(remotefile), local_file);
    }

    /**
     * Скачавает удаленный файл remotefile в массив строк
     *
     * @param remotefile Удаленный файл
     * @return Массив строк
     * @throws IOException
     */
    public synchronized String[] downloadFileStrings(URI remotefile) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder res = new StringBuilder();
        InputStream in = null;
        try {
            if (getFileSize(remotefile) > 0) {
                in = sardine.get(remotefile.toString());
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
        }
    }

    public synchronized String[] downloadFileStrings(String remotefile) throws IOException, URISyntaxException {
        return downloadFileStrings(webdavPath(remotefile));
    }

    /**
     * Размер удаленного файла
     *
     * @param remotefile Удаленный файл
     * @return
     * @throws IOException
     */
    public synchronized long getFileSize(URI remotefile) throws IOException {
        long fileSize = -1;
        final List<DavResource> resources;

        if (sardine.exists(remotefile.toString())) {
            resources = sardine.list(remotefile.toString());
            fileSize = resources.get(0).getContentLength();
        }
        return fileSize;
    }

    public synchronized long getFileSize(String remotefile) throws IOException, URISyntaxException {
        return getFileSize(webdavPath(remotefile));
    }

}
