package org.ndroi.easy163.providers.utils;

import org.ndroi.easy163.utils.Keyword;

/**
 * Created by andro on 2020/5/7.
 */
public class KeywordMatch
{
    private static String matchStrPreProcess(String str)
    {
        str = str.toLowerCase().trim();
        str = str.replace('，', ',').replace('？', '?');
        str = str.replace(", ", ",");
        return str;
    }

    public static boolean match(String a, String b)
    {
        if(a == null || b == null || a.isEmpty() || b.isEmpty())
        {
            return false;
        }
        a = matchStrPreProcess(a);
        b = matchStrPreProcess(b);
        boolean isContain = a.contains(b) || b.contains(a);
        if(isContain)
        {
            return true;
        }
        if(a.contains(" "))
        {
            a = a.split(" ")[0];
        }
        if(b.contains(" "))
        {
            b = b.split(" ")[0];
        }
        return a.contains(b) || b.contains(a);
    }

    public static boolean match(Keyword a, Keyword b)
    {
        boolean nameMatch = match(a.songName, b.songName);
        if (!nameMatch)
        {
            return false;
        }
        for (String aSinger : a.singers)
        {
            for (String bSinger : b.singers)
            {
                if (match(aSinger, bSinger))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
