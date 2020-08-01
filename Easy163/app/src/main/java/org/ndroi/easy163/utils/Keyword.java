package org.ndroi.easy163.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andro on 2020/5/5.
 */
public class Keyword
{
    public String songName;
    public List<String> singers = new ArrayList<>();
    public String extra = null; // extra songName info in (xxx)

    public void applyRawSongName(String rawSongName)
    {
        int p = rawSongName.indexOf('(');
        if(p == -1)
        {
            p = rawSongName.indexOf('（');
        }
        if (p != -1)
        {
            songName = rawSongName.substring(0, p).trim();
            int q = rawSongName.indexOf(')', p);
            if(q == -1)
            {
                q = rawSongName.indexOf('）', p);
            }
            if(q != -1)
            {
                extra = rawSongName.substring(p + 1, q);
            }
        }else
        {
            songName = rawSongName.trim();
            extra = null;
        }
    }

    @Override
    public String toString()
    {
        String str = songName + ":";
        for (String singer : singers)
        {
            str += singer + '/';
        }
        str = str.substring(0, str.length() - 1);
        return str;
    }
}
