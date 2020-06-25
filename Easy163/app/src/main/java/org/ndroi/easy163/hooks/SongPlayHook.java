package org.ndroi.easy163.hooks;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.hooks.utils.ConcurrencyTask;
import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.proxy.hook.ResponseHookData;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Song;

/**
 * Created by andro on 2020/5/5.
 */
public class SongPlayHook extends Hook
{
    @Override
    public boolean rule(String method, String uri)
    {
        if(!method.equals("POST") || !uri2Host(uri).endsWith("music.163.com"))
        {
            return false;
        }
        String path = uri2Path(uri);
        if(!path.contains("/song/enhance/player/url"))
        {
            return false;
        }
        return true;
    }

    private void handleNoFreeSong(JSONObject jsonObject) throws Exception
    {
        ConcurrencyTask concurrencyTask = new ConcurrencyTask();
        JSONArray songObjects = jsonObject.getJSONArray("data");
        for (Object obj : songObjects)
        {
            JSONObject songObject = (JSONObject) obj;
            if(songObject.getJSONObject("freeTrialInfo") != null || songObject.getIntValue("code") != 200)
            {
                concurrencyTask.addTask(new Thread()
                {
                    @Override
                    public void run()
                    {
                        super.run();
                        String id = songObject.getString("id");
                        Song providerSong = (Song) Cache.providerSongs.get(id);
                        if(providerSong == null)
                        {
                            Log.d("easy163", "no provider found");
                            return;
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
                });
            }
        }
        concurrencyTask.waitAll();
    }

    @Override
    public void hookResponse(ResponseHookData data) throws Exception
    {
        byte[] bytes = Crypto.aesDecrypt(data.getContent());
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleNoFreeSong(jsonObject);
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        data.setContent(bytes);
    }
}
