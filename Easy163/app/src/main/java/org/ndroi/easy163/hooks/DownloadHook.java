package org.ndroi.easy163.hooks;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.utils.Song;
import org.ndroi.easy163.vpn.hookhttp.Request;
import org.ndroi.easy163.vpn.hookhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by andro on 2020/5/6.
 */
public class DownloadHook extends BaseHook
{
    @Override
    public boolean rule(Request request)
    {
        String method = request.getMethod();
        String host = request.getHeaderFields().get("Host");
        if (!method.equals("POST") || !host.endsWith("music.163.com"))
        {
            return false;
        }
        String path = getPath(request);
        return path.endsWith("/song/enhance/download/url");
    }

    @Override
    public void hookRequest(Request request)
    {
        super.hookRequest(request);
        Crypto.Request cryptoRequest = Crypto.decryptRequestBody(new String(request.getContent()));
        cryptoRequest.path = "/api/song/enhance/player/url";
        String id = cryptoRequest.json.getString("id");
        cryptoRequest.json.put("ids", "[\"" + id + "\"]");
        cryptoRequest.json.remove("id");
        byte[] bytes = Crypto.encryptRequestBody(cryptoRequest).getBytes();
        request.setUri("http://music.163.com/eapi/song/enhance/player/url");
        request.setContent(bytes);
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
                byte[] bytes = new byte[4096*2];
                while (true)
                {
                    int readLen = inputStream.read(bytes);
                    if (readLen == -1)
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
        if (object.getClass().equals(JSONArray.class))
        {
            songObject = ((JSONArray) object).getJSONObject(0);
            jsonObject.put("data", songObject);
        } else
        {
            songObject = (JSONObject) object;
        }
        if (songObject.getString("url") == null ||
                songObject.getIntValue("code") != 200 ||
                songObject.getJSONObject("freeTrialInfo") != null)
        {
            String id = songObject.getString("id");
            Song providerSong = (Song) Cache.providerSongs.get(id);
            if (providerSong.md5.equals("unknown"))
            {
                providerSong.md5 = preDownloadForMd5(providerSong.url);
                //Log.d("DownloadHook", "Pre-download for md5: " + providerSong.md5);
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
        songObject.put("fee", 0);
        songObject.put("flag", 0);
    }

    @Override
    public void hookResponse(Response response)
    {
        super.hookResponse(response);
        byte[] bytes = Crypto.aesDecrypt(response.getContent());
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        handleDownload(jsonObject);
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        response.setContent(bytes);
    }
}
