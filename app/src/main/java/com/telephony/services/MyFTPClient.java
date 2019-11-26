package com.telephony.services;

import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;

public class MyFTPClient extends FTPSClient {

    private URI url;
    private boolean isAuthorized = false;
    private static MyFTPClient sInstance;

    public static final MyFTPClient getInstance() {
        if (sInstance == null) {
            synchronized (MyFTPClient.class) {
                if (sInstance == null) {
                    sInstance = new MyFTPClient();
                }
            }
        }
        return sInstance;
    }

    private MyFTPClient() {
        super(false);
        FTPClientConfig config = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
        config.setServerLanguageCode("ru");
        configure(config);

        setControlEncoding("UTF8");
        setAutodetectUTF8(true);

        setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager());

    }

    /**
     * Готово ли соединение к передаче данных
     *
     * @return
     * @throws IOException
     */
    public synchronized boolean isReady() throws IOException {
        return isConnected() && isAuthorized && sendNoOp();
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
        int port = 21;
        String proto = "";

        if (!isReady()) {
            this.url = uri;
            String authority = url.getUserInfo();
            if (authority != null) {
                String[] auth = authority.split(":");
                username = auth[0];
                password = auth[1];
            }

            proto = url.getScheme();
            port = url.getPort();
            if (port == -1) {
                if ("ftp".equals(proto)) {
                    port = DEFAULT_PORT;
                } else if ("ftps".equals(proto)) {
                    port = DEFAULT_FTPS_PORT;
                }
            }

            setConnectTimeout(10000);
            setDefaultTimeout(10000);
            super.connect(url.getHost(), port);
            if (!FTPReply.isPositiveCompletion(getReplyCode())) {
                throw new IOException(getReplyString());
            }

            setSoTimeout(10000);
            execPBSZ(0);
            execPROT("P");

            isAuthorized = super.login(username, password);
            if (!FTPReply.isPositiveCompletion(getReplyCode())) {
                throw new IOException(getReplyString());
            }

            enterLocalPassiveMode();
            if (!FTPReply.isPositiveCompletion(getReplyCode())) {
                throw new IOException(getReplyString());
            }

            setFileType(BINARY_FILE_TYPE);
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
        FileInputStream in = null;
        try {
            in = new FileInputStream(local_file);
            storeFile(remotefile, in);
            if (!FTPReply.isPositiveCompletion(getReplyCode())) {
                throw new IOException(getReplyString());
            }
            return true;
        } finally {
            if (in != null) {
                in.close();
            }
        }
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
        try {
            if (getFileSize(remotefile) > 0) {
                out = new FileOutputStream(local_file);
                retrieveFile(remotefile, out);
                if (!FTPReply.isPositiveCompletion(getReplyCode())) {
                    throw new IOException(getReplyString());
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
        try {
            if (getFileSize(remotefile) > 0) {
                in = retrieveFileStream(remotefile);
                if (!FTPReply.isPositivePreliminary(getReplyCode())) {
                    throw new IOException(getReplyString());
                }
                if (in != null) {
                    int count = -1;
                    while ((count = in.read(buffer)) > 0) {
                        res.append(new String(buffer).substring(0, count));
                    }
                    in.close();
                    if (!completePendingCommand()) {
                        throw new IOException(getReplyString());
                    }
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

        if (hasFeature("SIZE")) {
            sendCommand("SIZE", remotefile);
            String reply = getReplyString();
            if (FTPReply.isPositiveCompletion(getReplyCode())) {
                String fz = reply.substring(reply.indexOf(" ")).trim();
                fileSize = Long.parseLong(fz);
            }
        } else {
            FTPFile[] files = listFiles(remotefile);
            if (files.length == 1 && files[0].isFile()) {
                fileSize = files[0].getSize();
            }
        }
        return fileSize;
    }

    @Override
    public synchronized boolean logout() throws IOException {
        isAuthorized = false;
        return super.logout();
    }

    @Override
    public synchronized void disconnect() throws IOException {
        if (isConnected()) {
            super.disconnect();
        }
        isAuthorized = false;
    }

}
