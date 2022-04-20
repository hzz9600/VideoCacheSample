### AndroidVideoCache(改造版)
[点击跳转到 AndroidVideoCache](https://github.com/danikula/AndroidVideoCache)

基于AndroidVideoCache(v2.7.1)进行修改。主要增加以下特性:
* 媒体文件预加载功能,用于实现短视频秒开场景
* M3u8边下边存的支持

#### 预加载
预加载功能类为 > com.danikula.videocache.preload.PreloadHelper
```
  //设置预加载缓存大小单位字节，默认 256KB -> 256*1024
  PreloadHelper.getInstance().setPreloadSize(512*1024);
  //加载制定url链接，url为源地址(非代理url)
  PreloadHelper.getInstance().load(cacheServer,url);
  //停止所有预加载。线程池默认有5个预加载线程,只能停止还没执行的
  PreloadHelper.getInstance().stopAllPreload();
//
```

#### M3u8边下边存
因M3u8本身就做了分片处理，这里没有再去做预加载功能，主要实现思路:
1. 获取代理M3u8链接，访问本地代理。
2. 如果文件还没缓存，去下载并保存到本地，有下载直接读缓存(缓存的文件和其它格式一样，内容也是源文件的内容)。
3. 读取缓存，解析文件内容，替换分片视频地址指向本地代理-M3u8ProxyUtil:rewriteProxyBody
4. 将上一步替换后的内容，响应到socket也就是播放器。播放器自动播放指向代理的分片，然后走普通媒体缓存逻辑。

注意一下M3u8始终要经过我们的代理服务，而普通媒体文件缓存后是直接返回File地址。
HttpProxyCacheServer.getProxyUrl(String url)
```
  public String getProxyUrl(String url, boolean allowCachedFileUri) {
        if (allowCachedFileUri && isCached(url) && !M3u8ProxyUtil.isM3u8Url(url)) {//m3u8始终走代理
            File cacheFile = getCacheFile(url);
            touchFileSafely(cacheFile);
            return Uri.fromFile(cacheFile).toString();
        }
        return isAlive() ? appendToProxyUrl(url) : url;
    }
```
