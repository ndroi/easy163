package com.zzzliu.easy163.proxy.hook;

import com.zzzliu.easy163.proxy.buffer.ChannelBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by andro on 2020/5/2.
 */
public class HookHttp
{
    private static HookHttp instance = new HookHttp();

    public static HookHttp getInstance()
    {
        return instance;
    }

    private Map<SocketChannel, HookManager> registerTargets = new HashMap<>();
    private List<Hook> hooks = new ArrayList<>();
    private List<String> httpsBlockList = new ArrayList<>();

    private void cleanInvalidTargets()
    {
        Iterator<SocketChannel> iter = registerTargets.keySet().iterator();
        while (iter.hasNext())
        {
            if (!iter.next().isOpen())
            {
                iter.remove();
            }
        }
    }

    public void addHttpsBlock(String host)
    {
        httpsBlockList.add(host);
    }

    public boolean isHttpsBlocked(String host)
    {
        return httpsBlockList.contains(host);
    }

    public void addHook(Hook hook)
    {
        hooks.add(hook);
    }

    /* only register the fist rule-satisfying hook */
    public void checkAndRegisterTarget(SocketChannel channel, String uri)
    {
        //System.out.print("hook check:" + uri + "\r\n");
        for (Hook hook : hooks)
        {
            if (hook.rule(uri))
            {
                registerTargets.put(channel, new HookManager(channel, hook));
                System.out.print("hook register:" + uri + " " + channel.hashCode() + "\r\n");
                break;
            }
        }
    }

    public void UnRegisterTarget(SocketChannel channel)
    {
        registerTargets.remove(channel);
    }

    public boolean checkAndHookTarget(SocketChannel channel, ChannelBuffer buffer)
    {
        if (!registerTargets.containsKey(channel))
        {
            return false;
        }
        System.out.print("registered hook:" + channel.hashCode() + "\r\n");
        HookManager hookManager = registerTargets.get(channel);
        hookManager.collectDataAndHook(buffer.toViewInputStream());
        buffer.clear();
        cleanInvalidTargets();
        return true;
    }
}
