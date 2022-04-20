package com.danikula.videocache;

import android.text.TextUtils;

import com.danikula.videocache.file.FileCache;
import com.danikula.videocache.preload.PreloadLog;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Locale;

import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * {@link ProxyCache} that read http url and writes data to {@link Socket}
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class HttpProxyCache extends ProxyCache {

    private static final float NO_CACHE_BARRIER = .2f;

    private final HttpUrlSource source;
    private final FileCache cache;
    private CacheListener listener;

    public HttpProxyCache(HttpUrlSource source, FileCache cache) {
        super(source, cache);
        this.cache = cache;
        this.source = source;
    }

    public void registerCacheListener(CacheListener cacheListener) {
        this.listener = cacheListener;
    }

    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        long offset = request.rangeOffset;

        //m3u8单独处理,需要获取到内容后进行替换
        if (M3u8ProxyUtil.isM3u8Url(request.uri)) {
            responseM3u8WithCache(socket, out, offset);
            return;
        }
        String responseHeaders = newResponseHeaders(request);
        out.write(responseHeaders.getBytes("UTF-8"));
        if (isUseCache(request)) {
            responseWithCache(out, offset);
        } else {
            responseWithoutCache(out, offset);
        }
    }

    private boolean isUseCache(GetRequest request) throws ProxyCacheException {
        long sourceLength = source.length();
        boolean sourceLengthKnown = sourceLength > 0;
        long cacheAvailable = cache.available();
        // do not use cache for partial requests which too far from available cache. It seems user seek video.
        return !sourceLengthKnown || !request.partial || request.rangeOffset <= cacheAvailable + sourceLength * NO_CACHE_BARRIER;
    }

    private String newResponseHeaders(GetRequest request) throws IOException, ProxyCacheException {
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        long length = cache.isCompleted() ? cache.available() : source.length();
        boolean lengthKnown = length >= 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(mimeKnown ? format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    private void responseM3u8WithCache(Socket socket, OutputStream out, long offset) throws ProxyCacheException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //边下边存，同时记录内容
        responseWithCache(bos, offset);

        String host = socket.getLocalAddress().getHostAddress();
        int port = socket.getLocalPort();
        String body = bos.toString();
        URL url = new URL(source.getUrl());
        String proxyAddress = format("http://%s:%d", host, port);

        String requestHost = url.getPort() > 0 ? url.getHost() + ":" + url.getPort() : url.getHost();
        String requestAddress = format("%s://%s", url.getProtocol(), requestHost);
        //将预先下载完的内容替换ts为代理链接
        String resultProxyBody = M3u8ProxyUtil.rewriteProxyBody(proxyAddress, requestAddress, body);

        //响应回播放器
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        long length = resultProxyBody.length();
        String outHttp = new StringBuilder()
                .append("HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(format("Content-Length: %d\n", length))
                .append(mimeKnown ? format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .append(resultProxyBody)
                .append("\n") // http content end
                .toString();

        PreloadLog.debug(outHttp);//打印相应到播放器的内容
        out.write(outHttp.getBytes("UTF-8"));
        out.flush();
        bos.close();
    }

    private void responseWithCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
            offset += readBytes;
        }
        out.flush();
    }

    private void responseWithoutCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        HttpUrlSource newSourceNoCache = new HttpUrlSource(this.source);
        try {
            newSourceNoCache.open((int) offset);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                offset += readBytes;
            }
            out.flush();
        } finally {
            newSourceNoCache.close();
        }
    }

    private String format(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }

    @Override
    protected void onCachePercentsAvailableChanged(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.file, source.getUrl(), percents);
        }
    }
}
