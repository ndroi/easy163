package org.ndroi.easy163.hooks;

import android.util.Log;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.hooks.utils.JsonUtil;
import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Keyword;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andro on 2020/5/3.
 */
public class PlaylistHook extends Hook
{
    private List<String> hosts = Arrays.asList(
            "music.163.com",
            "interface.music.163.com",
            "interface3.music.163.com"
    );
    private List<String> paths = Arrays.asList(
            "/playlist/detail",
            "/eapi/playlist/v4/detail",
            "/eapi/play-record/playlist/list",
            "/eapi/album/v3/detail",
            "/discovery/recommend/songs",
            "/eapi/album/privilege",
            "/eapi/batch",
            "/artist/privilege",
            "/eapi/artist/top/song",
            "/api/v3/song/detail",
            "/eapi/song/enhance/privilege"
    );

    @Override
    public boolean rule(String method, String uri)
    {
        if(!method.equals("POST") || !hosts.contains(uri2Host(uri)))
        {
            return false;
        }
        String path = uri2Path(uri);
        Log.d("check path", path);
        for(String p : paths)
        {
            if(path.contains(p))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] hook(byte[] bytes)
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