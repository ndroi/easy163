package com.zzzliu.easy163.core;

import com.zzzliu.easy163.providers.KuwoMusic;
import com.zzzliu.easy163.providers.MiguMusic;
import com.zzzliu.easy163.providers.Provider;
import com.zzzliu.easy163.providers.QQMusic;
import com.zzzliu.easy163.utils.Keyword;
import com.zzzliu.easy163.utils.Song;

import java.util.ArrayList;
import java.util.List;

/* search Song from providers */
public class Search
{
    private static Provider[] providers = new Provider[]{
            new QQMusic(),
            new KuwoMusic(),
            new MiguMusic(),
    };

    public static Song search(Keyword keyword)
    {
        List<Song> songs = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for(Provider provider : providers)
        {
            Thread thread = new Thread()
            {
                @Override
                public void run()
                {
                    super.run();
                    Song song = provider.match(keyword);
                    if(song != null)
                    {
                        synchronized (songs)
                        {
                            songs.add(song);
                        }
                    }
                }
            };
            thread.start();
            threads.add(thread);
        }
        long startTime =  System.currentTimeMillis();
        /* just busy wait: first finish or 10 sec */
        while (songs.isEmpty())
        {
            long endTime =  System.currentTimeMillis();
            if(endTime - startTime > 10*1000)
            {
                break;
            }
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            } 
        }
        if(!songs.isEmpty())
        {
            System.out.println("from provider: " + songs.get(0).url + "/" + + songs.get(0).size);
            return songs.get(0);
        }
        return null;
    }
}
