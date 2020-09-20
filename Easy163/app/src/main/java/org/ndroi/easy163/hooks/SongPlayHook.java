package org.ndroi.easy163.hooks;

import android.util.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.utils.ConcurrencyTask;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Song;
import org.ndroi.easy163.vpn.hookhttp.Request;
import org.ndroi.easy163.vpn.hookhttp.Response;

/**
 * Created by andro on 2020/5/5.
 */
public class SongPlayHook extends BaseHook
{
    @Override
    public boolean rule(Request request)
    {
        String method = request.getMethod();
        String Host = request.getHeaderFields().get("Host");
        String host = request.getHeaderFields().get("host");
        String path = getPath(request);
        if(host != null)
        {
            if(method.equals("GET") && host.endsWith("music.163.com") && path.contains("/song/enhance/player/url"))
            {
                Log.d("check rule","Rewind");
                isRewind = true;
                return true;
            }
        }
        if(Host != null)
        {
            if (!method.equals("POST") || !Host.endsWith("music.163.com"))
            {
                return false;
            }
        }

        return path.contains("/song/enhance/player/url");
    }

    private void handleNoFreeSong(JSONObject jsonObject)
    {
        ConcurrencyTask concurrencyTask = new ConcurrencyTask();
        JSONArray songObjects = jsonObject.getJSONArray("data");
        if(!isRewind)
        {
            for (Object obj : songObjects)
            {
                JSONObject songObject = (JSONObject) obj;
                if (songObject.getJSONObject("freeTrialInfo") != null || songObject.getIntValue("code") != 200)
                {
                    concurrencyTask.addTask(new Thread()
                    {
                        @Override
                        public void run()
                        {
                            super.run();
                            String id = songObject.getString("id");
                            Song providerSong = (Song) Cache.providerSongs.get(id);
                            if (providerSong == null)
                            {
                                Log.d("SongPlayHook", "no provider found");
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
        }
        else
        {
            for (Object obj : songObjects)
            {
                JSONObject songObject = (JSONObject) obj;
                if (songObject.getJSONObject("freeTrialInfo") != null || songObject.getJSONObject("url") == null)
                {
                    concurrencyTask.addTask(new Thread()
                    {
                        @Override
                        public void run()
                        {
                            super.run();
                            String id = songObject.getString("id");
                            Song providerSong = (Song) Cache.providerSongs.get(id);
                            if (providerSong == null)
                            {
                                Log.d("SongPlayHook", "no provider found");
                                return;
                            }
                            songObject.put("fee", 0);;
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
        }

        concurrencyTask.waitAll();
    }

    @Override
    public void hookResponse(Response response)
    {
        super.hookResponse(response);
        byte[] bytes = response.getContent();
        Log.d("ResponseSong:",new String(bytes));
        if (!isRewind)
        {
            bytes = Crypto.aesDecrypt(bytes);
        }
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleNoFreeSong(jsonObject);
        bytes = JSONObject.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue).getBytes();
        if(!isRewind)
        {
            bytes = Crypto.aesEncrypt(bytes);
        }
        Log.d("ResponseSong hooked:",new String(bytes));
        response.setContent(bytes);
    }
}
