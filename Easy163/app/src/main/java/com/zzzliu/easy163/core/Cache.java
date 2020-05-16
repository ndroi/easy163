package com.zzzliu.easy163.core;

import com.zzzliu.easy163.utils.Keyword;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by andro on 2020/5/5.
 */

public class Cache
{
    interface AddAction
    {
        Object add(String id);
    }
    private Map<String, Object> items = new LinkedHashMap<>();
    private AddAction addAction = null;

    public Cache(){}

    public Cache(AddAction addAction)
    {
        this.addAction = addAction;
    }

    private void cleanExpired()
    {
        if(items.size() > 1000)
        {
            String firstKey = items.keySet().iterator().next();
            items.remove(firstKey);
        }
    }

    public void add(String id, Object value)
    {
        cleanExpired();
        items.put(id, value);
    }

    public Object get(String id)
    {
        if(items.containsKey(id))
        {
            return items.get(id);
        }
        if(addAction == null)
        {
            return null;
        }
        Object value = addAction.add(id);
        if(value != null)
        {
            items.put(id, value);
        }
        return value;
    }

    /* id --> Keyword */
    public static Cache neteaseKeywords = new Cache(new AddAction()
    {
        @Override
        public Object add(String id)
        {
            return Find.find(id);
        }
    });

    /* id --> ProviderSong */
    public static Cache providerSongs = new Cache(new AddAction()
    {
        @Override
        public Object add(String id)
        {
            Keyword keyword = (Keyword) neteaseKeywords.get(id);
            return Search.search(keyword);
        }
    });
}
