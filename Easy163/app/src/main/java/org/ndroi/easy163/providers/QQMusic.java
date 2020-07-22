package org.ndroi.easy163.providers;

import android.util.Log;
import android.util.Pair;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.providers.utils.KeywordMatch;
import org.ndroi.easy163.providers.utils.ReadStream;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class QQMusic extends Provider
{
    @Override
    public Song match(Keyword keyword)
    {
        String query = keyword2Query(keyword);
        Pair<String, String> mIdAndMediaId = getMIdAndMediaId(query, keyword);
        if (mIdAndMediaId == null)
        {
            return null;
        }
        Song song = getSong(mIdAndMediaId);
        return song;
    }

    private JSONObject selectBestMatch(JSONArray candidates, Keyword keyword)
    {
        for (Object infoObj : candidates)
        {
            JSONObject info = (JSONObject) infoObj;
            int pay = info.getJSONObject("pay").getIntValue("pay_play");
            if (pay != 0)
            {
                continue;
            }
            String title = info.getString("title");
            if (title.toLowerCase().endsWith("(live)"))
            {
                Log.d("QQMusic", "Skip Live Version");
                continue;
            }
            Keyword candidateKeyword = new Keyword();
            candidateKeyword.songName = info.getString("name");
            JSONArray singersObj = info.getJSONArray("singer");
            for (Object singerObj : singersObj)
            {
                String singer = ((JSONObject) singerObj).getString("name");
                candidateKeyword.singers.add(singer);
            }
            if (KeywordMatch.match(keyword, candidateKeyword))
            {
                return info;
            }
        }
        return null;
    }

    private Pair<String, String> getMIdAndMediaId(String query, Keyword keyword)
    {
        String url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?" +
                "ct=24&qqmusic_ver=1298&new_json=1&remoteplace=txt.yqq.center&" +
                "searchid=46804741196796149&t=0&aggr=1&cr=1&catZhida=1&lossless=0&" +
                "flag_qc=0&p=1&n=20&w=" + query + "&" +
                "g_tk=5381&jsonpCallback=MusicJsonCallback10005317669353331&loginUin=0&hostUin=0&" +
                "format=jsonp&inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq&needNewCode=0";
        Pair<String, String> mIdAndMediaId = null;
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
                int p1 = str.indexOf('(');
                int p2 = str.lastIndexOf(')');
                str = str.substring(p1 + 1, p2);
                JSONObject jsonObject = JSONObject.parseObject(str);
                if (jsonObject.getIntValue("code") == 0)
                {
                    JSONArray candidates = jsonObject.getJSONObject("data")
                            .getJSONObject("song")
                            .getJSONArray("list");
                    JSONObject best = selectBestMatch(candidates, keyword);
                    if (best != null)
                    {
                        String mId = best.getString("mid");
                        String mediaId = best.getJSONObject("file").getString("media_mid");
                        mIdAndMediaId = new Pair<>(mId, mediaId);
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return mIdAndMediaId;
    }

    private Song getSong(Pair<String, String> best)
    {
        String mId = best.first;
        String mediaId = best.second;
        String filename = "M500" + mediaId + ".mp3";
        String url = "https://u.y.qq.com/cgi-bin/musicu.fcg?data=" +
                "{\"req_0\":{\"module\":\"vkey.GetVkeyServer\"," +
                "\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"7332953645\"," +
                "\"loginflag\":1,\"filename\":[\"" +
                filename +
                "\"],\"songmid\":[\"" +
                mId +
                "\"],\"songtype\":[0],\"uin\":\"0\",\"platform\":\"20\"}}}";
        Song song = null;
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
                if (jsonObject.getIntValue("code") == 0)
                {
                    String vkey = jsonObject.getJSONObject("req_0")
                            .getJSONObject("data")
                            .getJSONArray("midurlinfo")
                            .getJSONObject(0)
                            .getString("vkey");
                    if (!vkey.isEmpty())
                    {
                        String songUrl = "http://dl.stream.qqmusic.qq.com/" + filename +
                                "?vkey=" + vkey + "&uin=0&fromtag=8&guid=7332953645";
                        song = generateSong(songUrl);
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return song;
    }
}
