package org.ndroi.easy163.core;

import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cache
{
    interface AddAction
    {
        Object add(String id);
    }

    private Map<String, Object> items = new LinkedHashMap<>();
    private AddAction addAction;

    public Cache(AddAction addAction)
    {
        this.addAction = addAction;
    }

    public void add(String id, Object value)
    {
        synchronized (items)
        {
            items.put(id, value);
        }
    }

    public Object get(String id)
    {
        synchronized (items)
        {
            if (items.containsKey(id))
            {
                Object value = items.get(id);
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

    public static void init()
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
                Song song = Local.get(id);
                if(song != null)
                {
                    return song;
                }
                Keyword keyword = (Keyword) neteaseKeywords.get(id);
                return Search.search(keyword);
            }
        });
    }

    public static void clear()
    {
        providerSongs.items.clear();
    }
}
