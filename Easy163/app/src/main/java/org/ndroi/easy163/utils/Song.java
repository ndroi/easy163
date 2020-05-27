package org.ndroi.easy163.utils;

/**
 * Created by andro on 2020/5/5.
 */
/* Song::url is enough for playing normally in PC,
*  but Android client need a correct size.
* */
public class Song
{
    public int size = 10*1000*1000;
    public int br = 192000; // fake
    public String url = "";
    public String md5 = "00000000000000000000000000000000"; // fake
}
