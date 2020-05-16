package com.zzzliu.easy163.hooks;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzzliu.easy163.core.Cache;
import com.zzzliu.easy163.hooks.utils.JsonUtil;
import com.zzzliu.easy163.utils.Crypto;
import com.zzzliu.easy163.proxy.hook.Hook;
import com.zzzliu.easy163.utils.Keyword;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by andro on 2020/5/3.
 */
public class PlaylistHook extends Hook
{
    private Set<String> paths = new HashSet<>();

    public PlaylistHook()
    {
        paths.add("");
    }

    @Override
    public boolean rule(String uri)
    {
        String host = uri2Host(uri);
        if(!host.endsWith("music.163.com"))
        {
            return false;
        }
        String path = uri2Path(uri);
        System.out.println("path:" + path);
        if(path.contains("/playlist/detail"))
        {
            return true;
        }
        if(path.contains("/discovery/recommend/songs"))
        {
            return true;
        }
        if(path.contains("/album/privilege"))
        {
            return true;
        }
        if(path.contains("/eapi/batch"))
        {
            return true;
        }
        if(path.contains("/eapi/play-record/playlist/list"))
        {
            return true;
        }
        if(path.contains("/artist/privilege"))
        {
            return true;
        }
        if(path.contains("/eapi/artist/top/song"))
        {
            return true;
        }
        if(path.contains("/eapi/song/enhance/privilege"))
        {
            return true;
        }
        return false;
    }

    @Override
    public byte[] hook(byte[] bytes) throws Exception
    {
        bytes = Crypto.aesDecrypt(bytes);
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        //cacheKeywords(jsonObject);
        modifyPrivileges(jsonObject);
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        return bytes;
    }

    private void cacheKeywords(JSONObject jsonObject)
    {
        JSONObject playlist = jsonObject.getJSONObject("playlist");
        JSONArray tracks = (JSONArray) playlist.get("tracks");
        for (Object trackObj : tracks)
        {
            JSONObject track = (JSONObject)trackObj;
            String songId = track.getString("id");
            Keyword keyword = new Keyword();
            keyword.songName = track.getString("name");
            for (Object singerObj : track.getJSONArray("ar"))
            {
                JSONObject singer = (JSONObject) singerObj;
                keyword.singers.add(singer.getString("name"));
            }
            Cache.neteaseKeywords.add(songId, keyword);
        }
    }

    private void modifyPrivileges(JSONObject jsonObject)
    {
        JsonUtil.replace(jsonObject, new JsonUtil.Rule()
        {
            @Override
            public void apply(JSONObject object)
            {
                if(object.containsKey("st") &&
                        object.containsKey("subp") &&
                        object.containsKey("pl") &&
                        object.containsKey("dl"))
                {
                    object.put("fee", 0);
                    object.put("st", 0);
                    object.put("subp", 1);
                    if(object.getIntValue("pl") == 0)
                    {
                        object.put("pl", 320000);
                    }
                    if(object.getIntValue("dl") == 0)
                    {
                        object.put("dl", 320000);
                    }
                }
            }
        });
    }
}
