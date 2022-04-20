package com.danikula.videocache;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class M3u8ProxyUtil {
    private static final String TAG_MEDIA_EXT_INF = "#EXTINF";
    public static final String TAG_MEDIA_EXT_STEAM_INF = "#EXT-X-STREAM-INF";

    public static boolean isM3u8Url(String url) {
        if (!TextUtils.isEmpty(url) && url.endsWith(".m3u8")) {
            return true;
        }
        return false;
    }

    public static String rewriteProxyBody(String localProxySuffix, String requestHost, String m3u8Content) throws IOException {
        StringReader reader = new StringReader(m3u8Content);
        BufferedReader bufferReader = new BufferedReader(reader);

        String line;
        boolean mediaNextLine = false;
        StringBuilder result = new StringBuilder();
        while ((line = bufferReader.readLine()) != null) {
            line = line.trim();
            if (TextUtils.isEmpty(line))
                continue;

            /**
             * #EXTM3U
             * #EXT-X-VERSION:3           -->Constants.TAG_VERSION
             * #EXT-X-MEDIA-SEQUENCE:0    -->Constants.TAG_MEDIA_SEQUENCE
             * #EXT-X-ALLOW-CACHE:YES
             * #EXT-X-TARGETDURATION:16   -->Constants.TAG_TARGET_DURATION
             * #EXTINF:15.520000,         -->Constants.TAG_MEDIA_DURATION
             * out-0000.ts
             * #EXTINF:14.360000,
             * out-0001.ts
             * #EXT-X-ENDLIST             --> Constants.TAG_ENDLIST
             */

            if (mediaNextLine) {
                if (!line.startsWith("http")) {
                    line = requestHost + "/" + line;
                }
                line = localProxySuffix + "/" + ProxyCacheUtils.encode(line);//拼接成代理url
            }

            mediaNextLine = !mediaNextLine && (line.startsWith(TAG_MEDIA_EXT_INF) || line.startsWith(TAG_MEDIA_EXT_STEAM_INF));
            result.append(line).append("\n");
        }
        return result.toString();
    }


}
