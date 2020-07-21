package org.ndroi.easy163.core;

import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.providers.utils.ReadStream;
import org.ndroi.easy163.utils.Keyword;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/* given song id, find the keyword from netease server */
public class Find
{
    public static Keyword find(String id)
    {
        Keyword keyword = null;
        String url = "http://music.163.com/api/song/detail?ids=[" + id + "]";
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = ReadStream.read(connection.getInputStream());
                String str = new String(content);
                JSONObject jsonObject = JSONObject.parseObject(str);
                JSONObject songObj = jsonObject.getJSONArray("songs")
                        .getJSONObject(0);
                keyword = new Keyword();
                keyword.songName = songObj.getString("name");
                for (Object singerObj : songObj.getJSONArray("artists"))
                {
                    JSONObject singer = (JSONObject) singerObj;
                    keyword.singers.add(singer.getString("name"));
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return keyword;
    }
}
