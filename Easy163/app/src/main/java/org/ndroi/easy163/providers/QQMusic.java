package org.ndroi.easy163.providers;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.core.Local;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class QQMusic extends Provider
{
    public QQMusic(Keyword targetKeyword)
    {
        super("qq", targetKeyword);
    }

    @Override
    public void collectCandidateKeywords()
    {
        String query = keyword2Query(targetKeyword);
        String url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?" +
                "ct=24&qqmusic_ver=1298&new_json=1&remoteplace=txt.yqq.center&" +
                "searchid=46343560494538174&t=0&aggr=1&cr=1&catZhida=1&lossless=0&" +
                "flag_qc=0&p=1&n=10&w=" + query + "&" +
                "g_tk_new_20200303=5381&g_tk=5381&loginUin=0&hostUin=0&" +
                "format=json&inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0";
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
                Log.i("QQ", str);
                JSONObject jsonObject = JSONObject.parseObject(str);
                if (jsonObject.getIntValue("code") == 0)
                {
                    JSONArray candidates = jsonObject.getJSONObject("data")
                            .getJSONObject("song")
                            .getJSONArray("list");
                    for (Object infoObj : candidates)
                    {
                        JSONObject songJsonObject = (JSONObject) infoObj;
                        int pay = songJsonObject.getJSONObject("pay").getIntValue("pay_play");
                        if (pay != 0)
                        {
                            continue;
                        }
                        int fnote = songJsonObject.getIntValue("fnote");
                        if (fnote == 4002)
                        {
                            continue;
                        }
                        JSONObject files = songJsonObject.getJSONObject("file");
                        if(files.getIntValue("size_128") == 0 && files.getIntValue("size_320") == 0)
                        {
                            continue;
                        }
                        String songName = songJsonObject.getString("title");
                        Keyword candidateKeyword = new Keyword();
                        candidateKeyword.songName = songName;
                        JSONArray singersObj = songJsonObject.getJSONArray("singer");
                        for (Object singerObj : singersObj)
                        {
                            String singer = ((JSONObject) singerObj).getString("name");
                            candidateKeyword.singers.add(singer);
                        }
                        songJsonObjects.add(songJsonObject);
                        candidateKeywords.add(candidateKeyword);
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public Song fetchSelectedSong()
    {
        if(selectedIndex == -1)
        {
            return null;
        }
        JSONObject songJsonObject = songJsonObjects.get(selectedIndex);
        String mId = songJsonObject.getString("mid");
        String mediaMId = songJsonObject.getJSONObject("file").getString("media_mid");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", mId);
        jsonObject.put("media_mid", mediaMId);
        Song song = fetchSongByJson(jsonObject);
        if(song != null)
        {
            Local.put(targetKeyword.id, providerName, jsonObject);
        }
        return song;
    }

    @Override
    public Song fetchSongByJson(JSONObject jsonObject)
    {
        String mId = jsonObject.getString("mid");
        String mediaMId = jsonObject.getString("media_mid");
        if(mId == null || mediaMId == null)
        {
            return null;
        }
        String filename = "M500" + mediaMId + ".mp3";
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
                JSONObject jo = JSONObject.parseObject(str);
                if (jo.getIntValue("code") == 0)
                {
                    String vkey = jo.getJSONObject("req_0")
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