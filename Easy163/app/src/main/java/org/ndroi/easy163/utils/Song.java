package org.ndroi.easy163.utils;

/**
 * Created by andro on 2020/5/5.
 */
/* Song::url is enough for playing normally in PC,
 *  but Android client play need a correct size.
 *  for Android client download, md5 is must.
 * */
public class Song
{
    public int size = 10 * 1000 * 1000;
    public int br = 192000; // fake
    public String url = "";
    public String md5 = "";

    @Override
    public String toString()
    {
        String info = "url: " + url + "\nsize: " + size + "\nbr: " + br + "\nmd5: " + md5 + "\n";
        return info;
    }
}
