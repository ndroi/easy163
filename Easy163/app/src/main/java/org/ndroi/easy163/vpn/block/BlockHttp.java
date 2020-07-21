package org.ndroi.easy163.vpn.block;

import android.util.Log;

import org.ndroi.easy163.vpn.hookhttp.Request;

import java.util.HashSet;
import java.util.Set;

public class BlockHttp
{
    private static BlockHttp instance = new BlockHttp();

    public static BlockHttp getInstance()
    {
        return instance;
    }

    private Set<String> hosts = new HashSet<>();

    public void addHost(String host)
    {
        hosts.add(host);
    }

    public boolean check(Request request)
    {
        String host = request.getHeaderFields().get("Host");
        if(hosts.contains(host))
        {
            Log.d("BlockHttp", host);
            return true;
        }
        return false;
    }
}
