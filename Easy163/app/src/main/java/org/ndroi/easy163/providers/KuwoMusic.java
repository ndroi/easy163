package org.ndroi.easy163.providers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class KuwoMusic extends Provider
{
    public KuwoMusic(Keyword targetKeyword)
    {
        super(targetKeyword);
    }

    @Override
    public void collectCandidateKeywords()
    {
        String query = keyword2Query(targetKeyword);
        String token = "1234567890";
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
                    for (Object obj : candidates)
                    {
                        JSONObject songJsonObject = (JSONObject) obj;
                        Keyword candidateKeyword = new Keyword();
                        candidateKeyword.songName = songJsonObject.getString("name");
                        candidateKeyword.singers = Arrays.asList(songJsonObject.getString("artist").split("&"));
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
        String mId = songJsonObject.getString("musicrid");
        if(mId == null)
        {
            return null;
        }
        Song song = null;
        String url = "http://antiserver.kuwo.cn/anti.s?type=convert_url&format=mp3&response=url&rid=" + mId;
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
