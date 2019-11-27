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
import java.util.List;

import okhttp3.OkHttpClient;

public class MyWebdavClient {

    private URI url;
    private boolean isAuthorized = false;
    private static MyWebdavClient sInstance;
    private boolean isReady = false;
    private Sardine sardine;
    private OkHttpClient customHttp;

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

    private String webdavPath(String path) {
        String wScheme = url.getScheme();
        String wHost = url.getHost();
        String wPath = url.getPath();
        int wPort = url.getPort();
        return wScheme + "://" + wHost + "/" + wPath + "/" + path;
    }

    private MyWebdavClient() {
        customHttp = new CustomHttp().getCustomHttp();


    }

    /**
     * Готово ли соединение к передаче данных
     *
     * @return
     * @throws IOException
     */
    public synchronized boolean isReady() throws IOException {
        return isReady;
    }

    /**
     * Подключиться к серверу FTPS, залогиниться, установить режим передачи данных BINARY
     *
     * @param uri Ссылка к серверу с авторизационными данными
     * @return
     * @throws IOException
     * @throws SocketException
     */
    public synchronized boolean connect(URI uri) throws SocketException, IOException {
        String username = "";
        String password = "";

        if (!isReady()) {
            this.url = uri;
            String authority = url.getUserInfo();
            if (authority != null) {
                String[] auth = authority.split(":");
                username = auth[0];
                password = auth[1];
            }
            sardine = new OkHttpSardine(customHttp);
            sardine.setCredentials(username, password, true);
            isReady = true;
        }
        return isReady();
    }

    /**
     * Рекурсивно создает директории на ftp сервере
     *
     * @param dir
     * @throws IOException
     */
    public synchronized void mkdirs(String dir) throws IOException {
        String[] dirs = dir.split(File.separator);
        String path = "";

        for (String d : dirs) {
            path += d + File.separator;
            if (!sardine.exists(webdavPath(path))) {
                sardine.createDirectory(webdavPath(path));
            }
        }
    }

    /**
     * Возвращает имя файла на ftp сервере в который нужно закачать файл
     *
     * @param root_dir - корневая директория программы
     * @param file     - Файл для которого необходимо получить имя удаленного файла
     * @return
     * @throws IOException
     */
    public String getRemoteFile(File root_dir, File file) throws IOException {
        String remote_dir = "";
        if (url.getPath() != null) {
            remote_dir = file.getAbsoluteFile().getParent().replaceFirst(root_dir.getAbsolutePath(), url.getPath());
        }
        mkdirs(remote_dir);
        return remote_dir + File.separator + file.getName();
    }

    /**
     * Возвращает файл в который будет закачан удаленный файл
     *
     * @param root_dir   - корневая директория программы
     * @param remotefile - удаленный файл
     * @return
     */
    public File getLocalFile(File root_dir, String remotefile) {
        File local_dir = null;
        File rf;
        if ((url.getPath() != null) && (!url.getPath().isEmpty())) {
            rf = new File(url.getPath(), remotefile);
        } else {
            rf = new File(remotefile);
        }
        if ((rf.getParent() != null) && (!rf.getParent().isEmpty())) {
            if ((url.getPath() != null) && (!url.getPath().isEmpty())) {
                local_dir = new File(rf.getParent().replaceFirst(url.getPath(), root_dir.getAbsolutePath() + File.separator));
            } else {
                local_dir = new File(root_dir, rf.getParent());
            }
        } else {
            local_dir = root_dir;
        }
        if (!local_dir.exists()) {
            local_dir.mkdirs();
        }
        File local_file = new File(local_dir, rf.getName());
        return local_file;
    }

    public synchronized boolean uploadFile(File local_file, String remotefile) throws IOException {
        String rf = webdavPath(remotefile);
        sardine.put(rf, local_file, "application/octet-stream");
        return true;
    }

    /**
     * Скачать файл remotefile в файл local_file
     *
     * @param remotefile
     * @param local_file
     * @return
     * @throws IOException
     */
    public synchronized boolean downloadFile(String remotefile, File local_file) throws IOException {
        FileOutputStream out = null;
        InputStream in = null;
        byte[] buffer = new byte[1024];
        String rf = webdavPath(remotefile);
        try {
            if (getFileSize(remotefile) > 0) {
                out = new FileOutputStream(local_file);
                in = sardine.get(rf);
                if (in != null) {
                    int count = -1;
                    while ((count = in.read(buffer)) > 0) {
                        out.write(buffer);
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

    /**
     * Скачавает удаленный файл remotefile в массив строк
     *
     * @param remotefile Удаленный файл
     * @return Массив строк
     * @throws IOException
     */
    public synchronized String[] downloadFileStrings(String remotefile) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder res = new StringBuilder();
        InputStream in = null;
        String rf = webdavPath(remotefile);
        try {
            if (getFileSize(remotefile) > 0) {
                in = sardine.get(rf);
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

    /**
     * Размер удаленного файла
     *
     * @param remotefile Удаленный файл
     * @return
     * @throws IOException
     */
    public synchronized long getFileSize(String remotefile) throws IOException {
        long fileSize = -1;
        String rf = webdavPath(remotefile);
        final List<DavResource> resources;

        if (sardine.exists(rf)) {
            resources = sardine.list(rf);
            fileSize = resources.get(0).getContentLength();
        }
        return fileSize;
    }

}
