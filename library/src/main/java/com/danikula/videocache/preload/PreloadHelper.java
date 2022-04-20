package com.danikula.videocache.preload;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.FileCache;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PreloadHelper {
    private static final long DEFAULT_PRELOAD_SIZE = 256 * 1024;//最小缓存大小 256kb
    private static final int DEFAULT_PRELOAD_THREAD_COUNT = 5;//默认预缓存线程数

    private final ExecutorService preloadThreadPool = Executors.newFixedThreadPool(DEFAULT_PRELOAD_THREAD_COUNT);
    private long preloadSize = DEFAULT_PRELOAD_SIZE;

    private PreloadHelper() {
    }

    public static PreloadHelper getInstance() {
        return PreloadHelperHolder.INSTANCE;
    }

    public void setPreloadSize(long size) {
        this.preloadSize = Math.max(preloadSize, size);
    }

    public void load(HttpProxyCacheServer cacheServer, String url) {
        String proxyUrl = cacheServer.getProxyUrl(url);
        File cacheFile = cacheServer.getCacheFile(url);
        File downloadCacheFile = new File(cacheFile.getParentFile(), cacheFile.getName() + FileCache.TEMP_POSTFIX);

        if (!proxyUrl.startsWith("http")) {
            PreloadLog.info("PreloadHelper::load(proxyUrl) => proxyUrl is not httpUrl, if startsWith File://.. it has been cached " + proxyUrl);
            return;
        }

        if (cacheFile.exists() || (downloadCacheFile.exists() && downloadCacheFile.length() > preloadSize)) {
            PreloadLog.info("PreloadHelper::load() =>  The file is preloaded." + proxyUrl);
            return;
        }

        preloadThreadPool.execute(new PreloadTaskRunnable(proxyUrl, preloadSize));
    }

    public void stopAllPreload() {
        //没有加载的停止预加载
        preloadThreadPool.shutdown();
    }

    private static class PreloadHelperHolder {
        private static final PreloadHelper INSTANCE = new PreloadHelper();
    }
}
