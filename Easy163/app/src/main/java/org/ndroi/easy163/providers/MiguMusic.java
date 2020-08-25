package org.ndroi.easy163.providers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.providers.utils.MiguCrypto;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.ConcurrencyTask;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MiguMusic extends Provider
{
    public MiguMusic(Keyword targetKeyword)
    {
        super(targetKeyword);
    }

    private void setHttpHeader(HttpURLConnection connection)
    {
        connection.setRequestProperty("origin", "https://music.migu.cn/");
        connection.setRequestProperty("referer", "https://music.migu.cn/");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36");
    }

    @Override
    public void collectCandidateKeywords()
    {
        String query = keyword2Query(targetKeyword);
        String url = "https://m.music.migu.cn/migu/remoting/scr_search_tag?keyword=" +
                query + "&type=2&rows=20&pgc=1";
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            setHttpHeader(connection);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = ReadStream.read(connection.getInputStream());
                String str = new String(content);
                JSONObject jsonObject = JSONObject.parseObject(str);
                JSONArray candidates = jsonObject.getJSONArray("musics");
                for (Object infoObj : candidates)
                {
                    JSONObject songJSONObject = (JSONObject) infoObj;
                    String songName = songJSONObject.getString("songName");
                    Keyword candidateKeyword = new Keyword();
                    candidateKeyword.songName = songName;
                    candidateKeyword.singers = Arrays.asList(songJSONObject.getString("singerName").split(", "));
                    songJsonObjects.add(songJSONObject);
                    candidateKeywords.add(candidateKeyword);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void requestSongUrl(String mId, String type, Map<String, String> results)
    {
        String url = "https://music.migu.cn/v3/api/music/audioPlayer/getPlayInfo?dataType=2&";
        String param = "{\"copyrightId\":\"" + mId + "\",\"type\":" + type + "}";
        String req = url + MiguCrypto.Encrypt(param);
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(req).openConnection();
            connection.setRequestMethod("GET");
            setHttpHeader(connection);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                byte[] content = ReadStream.read(connection.getInputStream());
                String str = new String(content);
                JSONObject jsonObject = JSONObject.parseObject(str);
                String code = jsonObject.getString("returnCode");
                if (code.equals("000000"))
                {
                    String songUrl = jsonObject.getJSONObject("data").getString("playUrl");
                    if(songUrl == null || songUrl.isEmpty())
                    {
                        return;
                    }
                    if (!songUrl.startsWith("http:"))
                    {
                        songUrl = "http:" + songUrl;
                    }
                    synchronized(results)
                    {
                        results.put(type, songUrl);
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
        String mId = songJsonObject.getString("copyrightId");
        ConcurrencyTask concurrencyTask = new ConcurrencyTask();
        Map<String, String> typeSongUrls = new HashMap<>();
        for (String type : new String[]{"1", "2"})
        {
            concurrencyTask.addTask(new Thread(){
                @Override
                public void run()
                {
                    super.run();
                    requestSongUrl(mId, type, typeSongUrls);
                }
            });
        }
        concurrencyTask.waitAll();
        if(typeSongUrls.containsKey("2"))
        {
            return generateSong(typeSongUrls.get("2"));
        }
        if(typeSongUrls.containsKey("1"))
        {
            return generateSong(typeSongUrls.get("1"));
        }
        return null;
    }
}
