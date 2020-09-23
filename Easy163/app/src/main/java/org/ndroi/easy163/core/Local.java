package org.ndroi.easy163.core;

import android.util.Log;
import com.alibaba.fastjson.JSONObject;
import org.ndroi.easy163.providers.Provider;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Song;
import org.ndroi.easy163.vpn.LocalVPNService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Local
{
    static class Item
    {
        public String providerName;
        public JSONObject jsonObject;
    }

    private static Map<String, Item> items = new HashMap<>();
    private static String diskFilename = "easy163_id_mid";

    private static File getCacheFile()
    {
        File cacheDir = LocalVPNService.getContext().getCacheDir();
        return new File(cacheDir, diskFilename);
    }

    public static void load()
    {
        items.clear();
        String data = "";
        try
        {
            FileInputStream inputStream = new FileInputStream(getCacheFile());
            byte[] bytes = ReadStream.read(inputStream);
            data = new String(bytes);
            inputStream.close();
        } catch (FileNotFoundException e)
        {
            EasyLog.log("未发现本地缓存");
            Log.d("Local", "未发现本地缓存");
            return;
        } catch (IOException e)
        {
            e.printStackTrace();
            EasyLog.log("本地缓存读取失败");
            Log.d("Local", "本地缓存读取失败");
            return;
        }
        String[] lines = data.split("\n");
        for (String line : lines)
        {
            int p1 = line.indexOf(' ');
            String id = line.substring(0, p1);
            int p2 = line.indexOf(' ', p1 + 1);
            Item item = new Item();
            item.providerName = line.substring(p1 + 1, p2);
            item.jsonObject = JSONObject.parseObject(line.substring(p2 + 1));
            items.put(id, item);
        }
        EasyLog.log("本地缓存加载完毕");
        Log.d("Local", "本地缓存加载完毕");
    }

    public static Song get(String id)
    {
        Item item = items.get(id);
        if(item == null)
        {
            return null;
        }
        EasyLog.log("本地缓存命中：" + "[" + item.providerName + "] " + id);
        List<Provider> providers = Provider.getProviders(null);
        Provider targetProvider = null;
        for (Provider provider : providers)
        {
            if(provider.getProviderName().equals(item.providerName))
            {
                targetProvider = provider;
                break;
            }
        }
        Song song = null;
        if(targetProvider != null)
        {
            song = targetProvider.fetchSongByJson(item.jsonObject);
            if(song == null)
            {
                items.remove(id);
                EasyLog.log("本地缓存失效：" + "[" + item.providerName + "] " + id);
            }
        }
        return song;
    }

    public static void put(String id, String providerName, JSONObject jsonObject)
    {
        if(items.containsKey(id))
        {
            return;
        }
        try
        {
            FileOutputStream outputStream = new FileOutputStream(getCacheFile(), true);
            String line = id + " " + providerName + " " + jsonObject.toString() + "\n";
            outputStream.write(line.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void clear()
    {
        items.clear();
        getCacheFile().delete();
    }
}
