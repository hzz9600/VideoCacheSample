package com.danikula.videocache.preload;

import com.danikula.videocache.HttpUrlSource;
import com.danikula.videocache.ProxyCacheException;

public class PreloadTaskRunnable implements Runnable {

    private final String proxyUrl;
    private final long preloadSize;

    public PreloadTaskRunnable(String proxyUrl, long preloadSize) {
        this.proxyUrl = proxyUrl;
        this.preloadSize = preloadSize;
    }

    @Override
    public void run() {
        PreloadLog.debug("PreloadTaskRunnable run():" + proxyUrl);

        HttpUrlSource newSourceNoCache = new HttpUrlSource(proxyUrl);

        try {
            newSourceNoCache.open(0);
            byte[] buffer = new byte[8 * 1024];//8K
            int bufferLen = buffer.length;
            int readBytes;
            int offset = 0;

            while ((offset + bufferLen <= preloadSize) && (readBytes = newSourceNoCache.read(buffer)) != -1) {
                //undo no need write.
                // out.write(buffer, 0, readBytes);
                offset += readBytes;
            }
        } catch (ProxyCacheException e) {
            e.printStackTrace();
        } finally {
            PreloadLog.debug("PreloadTaskRunnable is finished!!! ");
            try {
                newSourceNoCache.close();
            } catch (ProxyCacheException e) {
                e.printStackTrace();
            }
        }
    }
}
