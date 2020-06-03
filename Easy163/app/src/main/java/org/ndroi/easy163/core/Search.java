package org.ndroi.easy163.core;

import android.util.Log;

import org.ndroi.easy163.providers.KuwoMusic;
import org.ndroi.easy163.providers.MiguMusic;
import org.ndroi.easy163.providers.Provider;
import org.ndroi.easy163.providers.QQMusic;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;

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
            Log.d("search","from provider: " + songs.get(0).url + "/" + songs.get(0).md5);
            return songs.get(0);
        }
        return null;
    }
}
