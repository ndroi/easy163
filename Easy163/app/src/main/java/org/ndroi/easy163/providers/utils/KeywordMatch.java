package org.ndroi.easy163.providers.utils;

import org.ndroi.easy163.utils.Keyword;

/**
 * Created by andro on 2020/5/7.
 */
public class KeywordMatch
{
    private static boolean match(String a, String b)
    {
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
