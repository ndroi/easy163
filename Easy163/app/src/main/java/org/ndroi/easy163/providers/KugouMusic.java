package org.ndroi.easy163.providers;

import android.util.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.core.Local;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Song;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class KugouMusic extends Provider
{
    public KugouMusic(Keyword targetKeyword)
    {
        super("kugou", targetKeyword);
    }

    @Override
    public void collectCandidateKeywords()
    {
        String query = keyword2Query(targetKeyword);
        String url = "http://songsearch.kugou.com/song_search_v2?keyword=" + query + "&page=1";
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
                if (jsonObject.getIntValue("status") == 1)
                {
                    try
                    {
                        JSONArray candidates = jsonObject.getJSONObject("data").getJSONArray("lists");
                        for (Object obj : candidates)
                        {
                            JSONObject songJsonObject = (JSONObject) obj;
                            Keyword candidateKeyword = new Keyword();
                            candidateKeyword.songName = songJsonObject.getString("SongName");
                            candidateKeyword.singers = Arrays.asList(songJsonObject.getString("SingerName").split("、"));
                            songJsonObjects.add(songJsonObject);
                            candidateKeywords.add(candidateKeyword);
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
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
        //Log.d("kugou", songJsonObject.toJSONString());
        String mId = songJsonObject.getString("HQFileHash");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", mId);
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
        if(mId == null)
        {
            return null;
        }
        Song song = null;
        String key = "";
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("md5");
            for (byte b : messageDigest.digest((mId + "kgcloudv2").getBytes()))
            {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1)
                {
                    temp = "0" + temp;
                }
                key += temp;
            }
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        String url = "http://trackercdn.kugou.com/i/v2/?key=" + key + "&hash=" + mId +
                "&br=hq&appid=1005&pid=2&cmd=25&behavior=play";
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
                song = new Song();
                song.url = jo.getJSONArray("url").getString(0);
                song.br = jo.getIntValue("bitRate");
                song.size = jo.getIntValue("fileSize");
                song.md5 = mId.toLowerCase();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return song;
    }
}
