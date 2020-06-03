package org.ndroi.easy163.proxy.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by andro on 2020/5/3.
 */
abstract public class Hook
{
    abstract public boolean rule(String method, String uri);
    abstract public byte[] hook(byte[] bytes) throws Exception;

    /* http://www.baidu.com/xyz?id=x --> /xyz */
    protected String uri2Path(String uri)
    {
        int p1 = uri.indexOf("http://");
        if(p1 == -1) return "";
        p1 += "http://".length();
        p1 = uri.indexOf('/', p1);
        if(p1 == -1) return "";
        int p2 = uri.indexOf("?");
        if(p2 == -1) p2 = uri.length();
        String path = uri.substring(p1, p2);
        return path;
    }

    /* http://www.baidu.com/xyz?id=x --> www.baidu.com */
    protected String uri2Host(String uri)
    {
        int p = uri.indexOf("http://");
        if(p == -1) return "";
        uri = uri.substring(p + "http://".length());
        p = uri.indexOf("/");
        if(p == -1) p = uri.length();
        String host = uri.substring(0, p);
        return host;
    }
}
