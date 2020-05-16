package com.zzzliu.easy163.providers;

import com.zzzliu.easy163.utils.Keyword;
import com.zzzliu.easy163.utils.Song;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by andro on 2020/5/3.
 */
public abstract class Provider
{
    static protected String keyword2Query(Keyword keyword)
    {
        String str = keyword.songName + " ";
        for (String singer : keyword.singers)
        {
            str += (singer + " ");
        }
        str = str.trim();
        try
        {
            str = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        return str;
    }

    static protected Song generateSong(String url)
    {
        Song song = null;
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("range", "bytes=0-8191");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_PARTIAL)
            {
                song = new Song();
                song.url = url;
                String content_range = connection.getHeaderField("content-range");
                if(content_range != null)
                {
                    int p = content_range.indexOf('/');
                    song.size = Integer.parseInt(content_range.substring(p+1));
                }else
                {
                    song.size = connection.getContentLength();
                }
                MessageDigest messageDigest = MessageDigest.getInstance("md5");
                song.md5 = messageDigest.digest(song.url.getBytes()).toString();
//                byte[] content = new byte[8192];
//                InputStream inputStream = connection.getInputStream();
//                inputStream.read(content);
//                inputStream.close();
//                song.br = BitRate.Detect(content);
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return song;
    }

    abstract public Song match(Keyword keyword);
}
