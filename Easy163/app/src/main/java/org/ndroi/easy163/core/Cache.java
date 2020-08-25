package org.ndroi.easy163.core;

import android.util.Log;
import org.ndroi.easy163.ui.MainActivity;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Song;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cache
{
    interface AddAction
    {
        Object add(String id);
    }

    private static abstract class DiskSaver
    {
        public abstract void load(Map<String, Object> items);
        public abstract void update(String id, Object value);
    }

    private Map<String, Object> items = new LinkedHashMap<>();
    private AddAction addAction;
    private DiskSaver diskSaver = null;

    public Cache(AddAction addAction)
    {
        this.addAction = addAction;
    }

    public Cache(AddAction addAction, DiskSaver diskSaver)
    {
        this.addAction = addAction;
        this.diskSaver = diskSaver;
        diskSaver.load(items);
    }

    public void add(String id, Object value)
    {
        synchronized (items)
        {
            items.put(id, value);
            if(diskSaver != null)
            {
                diskSaver.update(id, value);
            }
        }
    }

    public Object get(String id)
    {
        synchronized (items)
        {
            if (items.containsKey(id))
            {
                return items.get(id);
            }
            if (addAction == null)
            {
                return null;
            }
            Object value = addAction.add(id);
            if (value != null)
            {
                add(id, value);
            }
            return value;
        }
    }

    /* id --> Keyword */
    public static Cache neteaseKeywords = null;

    /* id --> ProviderSong */
    public static Cache providerSongs = null;

    public static void Init()
    {
        neteaseKeywords = new Cache(new AddAction()
        {
            @Override
            public Object add(String id)
            {
                return Find.find(id);
            }
        });

        providerSongs = new Cache(new AddAction()
        {
            @Override
            public Object add(String id)
            {
                Keyword keyword = (Keyword) neteaseKeywords.get(id);
                return Search.search(keyword);
            }
        }, new DiskSaver()
        {
            private String diskFilename = "easy163_provider_songs";

            private File getCacheFile()
            {
                File cacheDir = MainActivity.getContext().getCacheDir();
                return new File(cacheDir, diskFilename);
            }

            @Override
            public void load(Map<String, Object> items)
            {
                String data = "";
                try
                {
                    FileInputStream inputStream = new FileInputStream(getCacheFile());
                    byte[] bytes = ReadStream.read(inputStream);
                    data = new String(bytes);
                    inputStream.close();
                } catch (FileNotFoundException e)
                {
                    //e.printStackTrace();
                    EasyLog.log("未发现本地缓存");
                    Log.d("DiskSaver", "未发现本地缓存");
                    return;
                } catch (IOException e)
                {
                    e.printStackTrace();
                    EasyLog.log("本地缓存读取失败");
                    Log.d("DiskSaver", "本地缓存读取失败");
                    return;
                }
                String[] lines = data.split("\n");
                for (String line : lines)
                {
                    String[] vs = line.split(" ");
                    String id = vs[0];
                    Song song = new Song();
                    song.url = vs[1];
                    song.size = Integer.valueOf(vs[2]);
                    song.br = Integer.valueOf(vs[3]);
                    song.md5 = vs[4];
                    items.put(id, song);
                    Log.d("DiskSaver", song.toString());
                }
                EasyLog.log("本地缓存加载完毕");
                Log.d("DiskSaver", "本地缓存加载完毕");
            }

            @Override
            public void update(String id, Object value)
            {
                Song song = (Song) value;
                try
                {
                    FileOutputStream outputStream = new FileOutputStream(getCacheFile(), true);
                    String line = id + " " + song.url + " " + song.size + " " + song.br + " " + song.md5 + "\n";
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
        });
    }
}
