package org.ndroi.easy163.hooks;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.hooks.utils.JsonUtil;
import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Keyword;

/**
 * Created by andro on 2020/5/3.
 */
public class PlaylistHook extends Hook
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
        Log.d("check path", path);
        if(path.contains("/playlist/detail"))
        {
            return true;
        }
        if(path.contains("/eapi/album/v3/detail"))
        {
            return true;
        }
        if(path.contains("/discovery/recommend/songs"))
        {
            return true;
        }
        if(path.contains("/eapi/album/privilege"))
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
        if(path.contains("/api/v3/song/detail"))
        {
            return true;
        }
        if(path.contains("/eapi/playlist/v4/detail"))
        {
            return true;
        }
        if(path.contains("/eapi/playlist/"))
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
        cacheKeywords(jsonObject);
        modifyPrivileges(jsonObject);
        Log.d("PlaylistHook", jsonObject.toString());
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        return bytes;
    }

    private void cacheKeywords(JSONObject jsonObject)
    {
        JsonUtil.traverse(jsonObject, new JsonUtil.Rule()
        {
            @Override
            public void apply(JSONObject object)
            {
                if(object.containsKey("id") &&
                        object.containsKey("name") &&
                        object.containsKey("ar"))
                {
                    String songId = object.getString("id");
                    Keyword keyword = new Keyword();
                    keyword.songName = object.getString("name");
                    for (Object singerObj : object.getJSONArray("ar"))
                    {
                        JSONObject singer = (JSONObject) singerObj;
                        keyword.singers.add(singer.getString("name"));
                    }
                    Cache.neteaseKeywords.add(songId, keyword);
                }
            }
        });
    }

    private void modifyPrivileges(JSONObject jsonObject)
    {
        JsonUtil.traverse(jsonObject, new JsonUtil.Rule()
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
