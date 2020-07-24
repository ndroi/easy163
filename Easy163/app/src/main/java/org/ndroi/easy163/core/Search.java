package org.ndroi.easy163.core;

import android.util.Log;

import org.ndroi.easy163.providers.KuwoMusic;
import org.ndroi.easy163.providers.MiguMusic;
import org.ndroi.easy163.providers.Provider;
import org.ndroi.easy163.providers.QQMusic;
import org.ndroi.easy163.utils.ConcurrencyTask;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* search Song from providers */
public class Search
{
    private static List<Provider> providers = Arrays.asList(
            new KuwoMusic(),
            new MiguMusic(),
            new QQMusic()
    );

    public static Song search(Keyword keyword)
    {
        Log.d("search", "start to search: " + keyword.toString());
        List<Song> songs = new ArrayList<>();
        ConcurrencyTask concurrencyTask = new ConcurrencyTask();
        for (Provider provider : providers)
        {
            concurrencyTask.addTask(new Thread(){
                @Override
                public void run()
                {
                    super.run();
                    Song song = provider.match(keyword);
                    if (song != null)
                    {
                        synchronized (songs)
                        {
                            songs.add(song);
                        }
                    }
                }
            });
        }
        long startTime = System.currentTimeMillis();
        /* just busy wait util first search successfully or all threads finish or 10 seconds */
        while (songs.isEmpty())
        {
            if(concurrencyTask.isAllFinished())
            {
                Log.d("search", "all providers finish");
                break;
            }
            long endTime = System.currentTimeMillis();
            if (endTime - startTime > 10 * 1000)
            {
                Log.d("search", "timeout");
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
        if (!songs.isEmpty())
        {
            Log.d("search", "from provider:\n" + songs.get(0).toString());
            return songs.get(0);
        }
        return null;
    }
}
