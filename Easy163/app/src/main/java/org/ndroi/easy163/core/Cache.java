package org.ndroi.easy163.core;

import android.util.Log;
import org.ndroi.easy163.ui.MainActivity;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.ReadStream;
import org.ndroi.easy163.utils.Song;
import org.ndroi.easy163.vpn.LocalVPNService;
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
        public abstract void onCacheHit(String id, Object value);
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
                Object value = items.get(id);
                if(diskSaver != null)
                {
                    diskSaver.onCacheHit(id, value);
                }
                return value;
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
        });
    }

    public static void Clear()
    {
        providerSongs.items.clear();
    }
}
