package com.telephony.services;

import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MyRecorder extends MediaRecorder {
    private boolean started = false;
    private File outputFile = null;
    private long min_filesize_bytes = 0;
    private int min_duration_ms = 0;
    private long current_time_ms = 0;

    private static int[] AUDIO_SOURCES = {MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.AudioSource.VOICE_UPLINK,
            MediaRecorder.AudioSource.VOICE_DOWNLINK, MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.MIC};

    public MyRecorder() {
        setMinDuration(1000);
        setMinFileSize(1024);
    }

    /**
     * Запущена ли запись.
     *
     * @return true - если запись идет.
     */
    public synchronized boolean isStarted() {
        return started;
    }

    /**
     * Начинает запись. Предварительно перебирает источники записи из массива AUDIO_SOURCES
     *
     * @param source       Предпочитаемый источник записи. Если он не поддерживается (вызывается исключение), выбирается следующий из массива AUDIO_SOURCES.
     * @param file         Файл для записи
     * @param max_duration Максимальная длительность записи в миллисекундах
     * @throws IOException Если ни один источник записи не сработал.
     */
    public synchronized void startRecorder(int source, File file, int max_duration) throws IOException {
        if (!started) {
            int i = 0, k = AUDIO_SOURCES.length;

            for (int s : AUDIO_SOURCES) {
                if (s == source) {
                    k = i;
                }
                if (i >= k) {
                    try {
                        reset();
                        setAudioSource(s);
                        setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        setOutputFile(file.getAbsolutePath());
                        setMaxDuration(max_duration);

                        prepare();
                        start();
                        Log.d(Utils.LOG_TAG, "source = " + s + " index = " + i);
                        return;
                    } catch (Exception e) {
                    }
                }
                i++;
            }
            throw new IOException("Error while start media recorder!");
        }
    }

    public synchronized void setMinDuration(int min_duration_ms) {
        this.min_duration_ms = min_duration_ms;
    }

    public synchronized void setMinFileSize(long min_filesize_bytes) {
        this.min_filesize_bytes = min_filesize_bytes;
    }

    public synchronized File getOutputFile() {
        return outputFile;
    }

    @Override
    public synchronized void setOutputFile(String path) throws IllegalStateException {
        super.setOutputFile(path);
        outputFile = new File(path);
    }

    @Override
    public synchronized void start() throws IllegalStateException {
        super.start();
        started = true;
        current_time_ms = SystemClock.elapsedRealtime();
    }

    @Override
    public synchronized void stop() throws IllegalStateException {
        if (started) {
            super.stop();
            eraseFileIfLessThan();
            started = false;
        }
    }

    @Override
    public synchronized void reset() {
        if (started) {
            stop();
        }
        super.reset();
        outputFile = null;
    }

    @Override
    public synchronized void release() {
        if (started) {
            reset();
        }
        super.release();
    }

    private void eraseFileIfLessThan() {
        if ((outputFile != null)
                && ((outputFile.length() < min_filesize_bytes) || ((SystemClock.elapsedRealtime() - current_time_ms) < min_duration_ms))) {
            outputFile.delete();
        }
    }

}
