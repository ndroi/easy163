package com.zzzliu.easy163.providers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzzliu.easy163.providers.utils.KeywordMatch;
import com.zzzliu.easy163.providers.utils.MiguCrypto;
import com.zzzliu.easy163.utils.Keyword;
import com.zzzliu.easy163.utils.Song;
import com.zzzliu.easy163.providers.utils.Stream2Bytes;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Created by andro on 2020/5/3.
 */
public class MiguMusic extends Provider
{
    @Override
    public Song match(Keyword keyword)
    {
        String query = keyword2Query(keyword);
        String mId = getMId(query, keyword);
        if (mId == null)
        {
            return null;
        }
        Song song = getSong(mId);
        return song;
    }

    private JSONObject selectBestMatch(JSONArray candidates, Keyword keyword)
    {
        for(Object infoObj : candidates)
        {
            JSONObject info = (JSONObject) infoObj;
            Keyword candidateKeyword = new Keyword();
            candidateKeyword.songName = info.getString("songName");
            candidateKeyword.singers.add(info.getString("singerName"));
            if(KeywordMatch.match(keyword, candidateKeyword))
            {
                return info;
            }
        }
        return null;
    }

    private String getMId(String query, Keyword keyword)
    {
        String mId = null;
        String url = "http://m.music.migu.cn/migu/remoting/scr_search_tag?keyword=" +
                query + "&type=2&rows=20&pgc=1";
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = Stream2Bytes.stream2Bytes(connection.getInputStream());
                String str = new String(content);
                JSONObject jsonObject = JSONObject.parseObject(str);
                JSONArray candidates = jsonObject.getJSONArray("musics");
                JSONObject best = selectBestMatch(candidates, keyword);
                if(best != null)
                {
                    mId = best.getString("copyrightId");
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return mId;
    }

    public Song getSong(String mId)
    {
        String url = "http://music.migu.cn/v3/api/music/audioPlayer/getPlayInfo?dataType=2&";
        String req = "{\"copyrightId\":\"" + mId + "\",\"type\":2}";
        url = url + MiguCrypto.Encrypt(req);
        Song song = null;
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("origin", "http://music.migu.cn/");
            connection.setRequestProperty("referer", "http://music.migu.cn/");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = Stream2Bytes.stream2Bytes(connection.getInputStream());
                String str = new String(content);
                JSONObject jsonObject = JSONObject.parseObject(str);
                String code = jsonObject.getString("returnCode");
                if(code.equals("000000"))
                {
                    String songUrl = jsonObject.getJSONObject("data").getString("playUrl");
                    song = generateSong(songUrl);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return song;
    }
}
