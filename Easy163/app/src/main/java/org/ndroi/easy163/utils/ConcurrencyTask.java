package org.ndroi.easy163.utils;

import java.util.ArrayList;
import java.util.List;

public class ConcurrencyTask
{
    private List<Thread> threads = new ArrayList<>();

    public void addTask(Thread thread)
    {
        threads.add(thread);
        thread.start();
    }

    public boolean isAllFinished()
    {
        for (Thread thread : threads)
        {
           if(thread.isAlive())
           {
               return false;
           }
        }
        return true;
    }

    public void waitAll()
    {
        try
        {
            for (Thread thread : threads)
            {
                thread.join();
            }
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
