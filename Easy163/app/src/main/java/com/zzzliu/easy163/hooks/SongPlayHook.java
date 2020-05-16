package com.zzzliu.easy163.hooks;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzzliu.easy163.proxy.hook.Hook;
import com.zzzliu.easy163.core.Cache;
import com.zzzliu.easy163.utils.Crypto;
import com.zzzliu.easy163.utils.Song;

/**
 * Created by andro on 2020/5/5.
 */
public class SongPlayHook extends Hook
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
        if(!path.contains("/eapi/song/enhance/player/url"))
        {
            return false;
        }
        return true;
    }

    private void handleNoFreeSong(JSONObject jsonObject) throws Exception
    {
        JSONArray songObjects = jsonObject.getJSONArray("data");
        for (Object obj : songObjects)
        {
            JSONObject songObject = (JSONObject) obj;
            if(songObject.getJSONObject("freeTrialInfo") != null || songObject.getIntValue("code") != 200)
            {
                String id = songObject.getString("id");
                Song providerSong = (Song) Cache.providerSongs.get(id);
                if(providerSong == null)
                {
                    throw new Exception("no provider found");
                }
                songObject.put("fee", 0);
                songObject.put("code", 200);
                songObject.put("url", providerSong.url);
                songObject.put("md5", providerSong.md5);
                songObject.put("br", providerSong.br);
                songObject.put("size", providerSong.size);
                songObject.put("freeTrialInfo", null);
                songObject.put("level", "standard");
                songObject.put("type", "mp3");
                songObject.put("encodeType", "mp3");
            }
        }
    }

    @Override
    public byte[] hook(byte[] bytes) throws Exception
    {
        bytes = Crypto.aesDecrypt(bytes);
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleNoFreeSong(jsonObject);
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        return bytes;
    }
}
