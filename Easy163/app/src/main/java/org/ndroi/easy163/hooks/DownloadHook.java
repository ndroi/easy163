package org.ndroi.easy163.hooks;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.providers.utils.Stream2Bytes;
import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.proxy.hook.RequestHookData;
import org.ndroi.easy163.proxy.hook.ResponseHookData;
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
        return path.endsWith("/song/enhance/download/url");
    }

    @Override
    public void hookRequest(RequestHookData data) throws Exception
    {
        Crypto.Request request = Crypto.decryptRequestBody(new String(data.getContent()));
        Log.d("hookRequest::path", request.path);
        Log.d("hookRequest::json", request.json.toString());
        request.path = "/api/song/enhance/player/url";
        String id = request.json.getString("id");
        request.json.put("ids", "[\"" + id + "\"]");
        request.json.remove("id");
        byte[] bytes = Crypto.encryptRequestBody(request).getBytes();
        data.setUri("http://music.163.com/eapi/song/enhance/player/url");
        data.setContent(bytes);
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
        JSONObject songObject = null;
        Object object = jsonObject.get("data");
        if(object.getClass().equals(JSONArray.class))
        {
            songObject = ((JSONArray)object).getJSONObject(0);
            jsonObject.put("data", songObject);
        } else
        {
            songObject = (JSONObject)object;
        }
        if(songObject.getString("url") == null || songObject.getIntValue("code") != 200)
        {
            String id = songObject.getString("id");
            Song providerSong = (Song) Cache.providerSongs.get(id);
            if(providerSong.md5.isEmpty())
            {
                providerSong.md5 = preDownloadForMd5(providerSong.url);
                Log.d("DownloadHook", "Pre-download for md5: " + providerSong.md5);
            }
            songObject.put("code", 200);
            songObject.put("url", providerSong.url);
            songObject.put("md5", providerSong.md5);
            songObject.put("br", providerSong.br);
            songObject.put("size", providerSong.size);
            songObject.put("freeTrialInfo", null);
            songObject.put("level", "standard");
            songObject.put("type", "mp3");
            songObject.put("encodeType", "mp3");
        }
        songObject.put("fee", 0) ;
        songObject.put("flag", 0);
    }

    @Override
    public void hookResponse(ResponseHookData data) throws Exception
    {
        byte[] bytes = Crypto.aesDecrypt(data.getContent());
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleDownload(jsonObject);
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        data.setContent(bytes);
    }
}
