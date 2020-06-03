package org.ndroi.easy163.hooks;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.providers.utils.Stream2Bytes;
import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Song;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by andro on 2020/5/6.
 */
public class DownloadHook extends Hook
{
    @Override
    public boolean rule(String method, String uri)
    {
        if(!method.equals("POST") || !uri2Host(uri).endsWith("music.163.com"))
        {
            return false;
        }
        String path = uri2Path(uri);
        if(!path.endsWith("/song/enhance/download/url"))
        {
            return false;
        }
        return true;
    }

    private String preDownloadForMd5(String url)
    {
        String md5 = "";
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                MessageDigest messageDigest = MessageDigest.getInstance("md5");
                InputStream inputStream = connection.getInputStream();
                byte[] bytes = new byte[4096];
                while (true)
                {
                    int readLen = inputStream.read(bytes);
                    if(readLen == -1)
                    {
                        break;
                    }
                    messageDigest.update(bytes, 0, readLen);
                }
                for (byte b : messageDigest.digest())
                {
                    String temp = Integer.toHexString(b & 0xff);
                    if (temp.length() == 1)
                    {
                        temp = "0" + temp;
                    }
                    md5 += temp;
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return md5;
    }

    private void handleDownload(JSONObject jsonObject)
    {
        JSONObject songObject = jsonObject.getJSONObject("data");
        if(songObject.getString("url") == null || songObject.getIntValue("code") != 200)
        {
            String id = songObject.getString("id");
            Song providerSong = (Song) Cache.providerSongs.get(id);
            if(providerSong.md5.isEmpty())
            {
                providerSong.md5 = preDownloadForMd5(providerSong.url);
                Log.d("DownloadHook", "Pre-download for md5: " + providerSong.md5);
            }
            songObject.put("fee", 0);
            songObject.put("code", 200);
            songObject.put("url", providerSong.url);
            songObject.put("md5", providerSong.md5);
            songObject.put("br", providerSong.br);
            songObject.put("size", providerSong.size);
            songObject.put("freeTrialInfo", null);
            songObject.put("level", "standard");
            songObject.put("type", "mp3");
            songObject.put("flag", 0);
            songObject.put("encodeType", "mp3");
        }
    }

    @Override
    public byte[] hook(byte[] bytes) throws Exception
    {
        bytes = Crypto.aesDecrypt(bytes);
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleDownload(jsonObject);
        Log.d("DownloadHook", jsonObject.toString());
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        return bytes;
    }
}
