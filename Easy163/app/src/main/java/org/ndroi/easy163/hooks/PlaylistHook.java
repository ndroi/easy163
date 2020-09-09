package org.ndroi.easy163.hooks;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.hooks.utils.JsonUtil;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.vpn.hookhttp.Request;
import org.ndroi.easy163.vpn.hookhttp.Response;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andro on 2020/5/3.
 */
public class PlaylistHook extends BaseHook
{
    private List<String> paths = Arrays.asList(
            "/playlist/detail",
            "/eapi/playlist/v4/detail",
            "/eapi/play-record/playlist/list",
            "/eapi/album/v3/detail",
            "/discovery/recommend/songs",
            "/eapi/album/privilege",
            "/eapi/playlist/privilege",
            "/eapi/batch",
            "/artist/privilege",
            "/eapi/artist/top/song",
            "/eapi/v3/song/detail",
            "/eapi/song/enhance/privilege",
            "/eapi/song/enhance/info/get",
            "/search/song/get",
            "/search/complex/get/",
            "/eapi/v1/artist/songs",
            "/eapi/v1/search/get"
    );

    @Override
    public boolean rule(Request request)
    {
        String method = request.getMethod();
        String host = request.getHeaderFields().get("Host");
        Log.d("check rule", host + "" + getPath(request));
        if (!method.equals("POST") || !host.endsWith("music.163.com"))
        {
            return false;
        }
        String path = getPath(request);
        for (String p : paths)
        {
            if (path.contains(p))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void hookResponse(Response response)
    {
        super.hookResponse(response);
        byte[] bytes = Crypto.aesDecrypt(response.getContent());
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        cacheKeywords(jsonObject);
        modifyPrivileges(jsonObject);
        bytes = JSONObject.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue).getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        response.setContent(bytes);
    }

    private void cacheKeywords(JSONObject jsonObject)
    {
        JsonUtil.traverse(jsonObject, new JsonUtil.Rule()
        {
            @Override
            public void apply(JSONObject object)
            {
                if (object.containsKey("id") &&
                        object.containsKey("name") &&
                        object.containsKey("ar"))
                {
                    String songId = object.getString("id");
                    Keyword keyword = new Keyword();
                    keyword.id = songId;
                    keyword.applyRawSongName(object.getString("name"));
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
                if(object.containsKey("fee"))
                {
                    object.put("fee", 0);
                }
                if (object.containsKey("st") &&
                        object.containsKey("subp") &&
                        object.containsKey("pl") &&
                        object.containsKey("dl"))
                {
                    object.put("st", 0);
                    object.put("subp", 1);
                    if (object.getIntValue("pl") == 0)
                    {
                        object.put("pl", 320000);
                    }
                    if (object.getIntValue("dl") == 0)
                    {
                        object.put("dl", 320000);
                    }
                }
            }
        });
    }
}
