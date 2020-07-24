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
    public boolean isOriginalSong = true;

    public void applySongName(String rawSongName)
    {
        int p = rawSongName.indexOf('(');
        if(p == -1)
        {
            p = rawSongName.indexOf('（');
        }
        if (p != -1)
        {
            isOriginalSong = false;
            songName = rawSongName.substring(0, p);
        }else
        {
            isOriginalSong = true;
            songName = rawSongName;
        }
    }

    @Override
    public String toString()
    {
        String str = songName + ": ";
        for (String singer : singers)
        {
            str += '/' + singer;
        }
        return str;
    }
}
