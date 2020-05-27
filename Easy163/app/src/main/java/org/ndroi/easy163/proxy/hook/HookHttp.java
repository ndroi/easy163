package org.ndroi.easy163.proxy.hook;

import android.util.Log;

import org.ndroi.easy163.proxy.buffer.ChannelBuffer;
import org.ndroi.easy163.proxy.context.ConnectionContext;
import org.ndroi.easy163.proxy.context.ProxyContext;

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
    public interface HttpsBlockRule
    {
        boolean rule(String host);
    }
    HttpsBlockRule httpsBlockRule = null;

    private List<Hook> hooks = new ArrayList<>();
    private List<String> httpsBlockList = new ArrayList<>();
    private List<String> httpBlockList = new ArrayList<>();

    public void addHttpsBlock(String host)
    {
        httpsBlockList.add(host);
    }

    public void setHttpsBlockRule(HttpsBlockRule rule)
    {
        httpsBlockRule = rule;
    }

    public void addHttpBlock(String host)
    {
        httpBlockList.add(host);
    }

    public boolean isHttpsBlocked(String host)
    {
        if(httpsBlockList.contains(host))
        {
            return true;
        }
        return httpsBlockRule != null && httpsBlockRule.rule(host);
    }

    public boolean isHttpBlocked(String host)
    {
        return httpBlockList.contains(host);
    }

    public void addHook(Hook hook)
    {
        hooks.add(hook);
    }

    /* only apply the fist rule-satisfying hook */
    public boolean checkAndHookTarget(ProxyContext context, String uri)
    {
        Log.d("hook check", uri);
        for (Hook hook : hooks)
        {
            if (hook.rule(uri))
            {
                Log.d("hook register", uri);
                new HookManager(context, hook).asyncHook();
                return true;
            }
        }
        return false;
    }
}
