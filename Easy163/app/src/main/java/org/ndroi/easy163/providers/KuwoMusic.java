package org.ndroi.easy163.providers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.providers.utils.KeywordMatch;
import org.ndroi.easy163.providers.utils.ReadStream;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by andro on 2020/5/3.
 */
public class KuwoMusic extends Provider
{
    @Override
    public Song match(Keyword keyword)
    {
        String query = keyword2Query(keyword);
        String token = getToken(query);
        if (token == null)
        {
            return null;
        }
        String mId = getmId(query, token, keyword);
        if (mId == null)
        {
            return null;
        }
        Song song = getSong(mId);
        return song;
    }

    private JSONObject selectBestMatch(JSONArray candidates, Keyword keyword)
    {
        for (Object infoObj : candidates)
        {
            JSONObject info = (JSONObject) infoObj;
            Keyword candidateKeyword = new Keyword();
            candidateKeyword.songName = info.getString("name");
            candidateKeyword.singers.add(info.getString("artist"));
            if (KeywordMatch.match(keyword, candidateKeyword))
            {
                return info;
            }
        }
        return null;
    }

    private String getToken(String query)
    {

        String token = null;
        String url = "http://kuwo.cn/search/list?key=" + query;
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537.36");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                Map<String, List<String>> header = connection.getHeaderFields();
                String cookie = header.get("Set-Cookie").get(0);
                int p1 = cookie.indexOf("kw_token=") + "kw_token=".length();
                int p2 = cookie.indexOf(";");
                token = cookie.substring(p1, p2);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return token;
    }

    private String getmId(String query, String token, Keyword keyword)
    {
        String mId = null;
        String url = "http://www.kuwo.cn/api/www/search/searchMusicBykeyWord?key=" +
                query + "&pn=1&rn=30";
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", "http://kuwo.cn/search/list?key=" + query);
            connection.setRequestProperty("csrf", token);
            connection.setRequestProperty("Cookie", "kw_token=" + token);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = ReadStream.read(connection.getInputStream());
                String str = new String(content);
                JSONObject jsonObject = JSONObject.parseObject(str);
                if (jsonObject.getIntValue("code") == 200)
                {
                    JSONArray candidates = jsonObject.getJSONObject("data").getJSONArray("list");
                    JSONObject best = selectBestMatch(candidates, keyword);
                    if (best != null)
                    {
                        mId = best.getString("musicrid");
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return mId;
    }

    private Song getSong(String MId)
    {
        Song song = null;
        String url = "http://antiserver.kuwo.cn/anti.s?type=convert_url&format=mp3&response=url&rid=" + MId;
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = ReadStream.read(connection.getInputStream());
                String songUrl = new String(content);
                if (songUrl.startsWith("http"))
                {
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
