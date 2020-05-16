package com.zzzliu.easy163.hooks;

import com.alibaba.fastjson.JSONObject;
import com.zzzliu.easy163.core.Cache;
import com.zzzliu.easy163.proxy.hook.Hook;
import com.zzzliu.easy163.utils.Crypto;
import com.zzzliu.easy163.utils.Song;

/**
 * Created by andro on 2020/5/6.
 */
public class DownloadHook extends Hook
{
    @Override
    public boolean rule(String uri)
    {
        String host = uri2Host(uri);
        if(!host.endsWith("music.163.com"))
        {
            return false;
        }
        String path = uri2Path(uri);
        if(!path.endsWith("eapi/song/enhance/download/url"))
        {
            return false;
        }
        return true;
    }

    private void handleDownload(JSONObject jsonObject)
    {
        JSONObject songObject = jsonObject.getJSONObject("data");
        if(songObject.getString("url") == null || songObject.getIntValue("code") != 200)
        {
            String id = songObject.getString("id");
            Song providerSong = (Song) Cache.providerSongs.get(id);
            songObject.put("fee", 0);
            songObject.put("code", 200);
            songObject.put("url", providerSong.url);
            songObject.put("md5", providerSong.md5);
            songObject.put("br", providerSong.br);
            songObject.put("size", providerSong.size);
            songObject.put("freeTrialInfo", null);
            songObject.put("level", "standard");
            songObject.put("type", "mp3");
            songObject.put("flag", 0);
            songObject.put("encodeType", "mp3");
        }
    }

    @Override
    public byte[] hook(byte[] bytes) throws Exception
    {
        bytes = Crypto.aesDecrypt(bytes);
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleDownload(jsonObject);
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        return bytes;
    }
}
