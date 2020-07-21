package org.ndroi.easy163.vpn.block;

import java.util.HashSet;
import java.util.Set;

/* not used yet
 * TODO: HTTP-DNS
 * */
public class BlockHttps
{
    private static BlockHttps instance = new BlockHttps();

    public static BlockHttps getInstance()
    {
        return instance;
    }

    private Set<String> hosts = new HashSet<>();

    public void addHost(String host)
    {
        hosts.add(host);
    }

    public boolean check(String ip)
    {
        return false;
    }
}
