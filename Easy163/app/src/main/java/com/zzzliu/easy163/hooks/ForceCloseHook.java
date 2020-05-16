package com.zzzliu.easy163.hooks;

import com.zzzliu.easy163.proxy.hook.Hook;

/**
 * Created by andro on 2020/5/3.
 */
/*
* make http connection not be keep-alive
* */
public class ForceCloseHook extends Hook
{
    public ForceCloseHook()
    {
        type = Type.ForceClose;
    }

    @Override
    public boolean rule(String uri)
    {
        String host = uri2Host(uri);
        return host.endsWith("music.163.com");
    }

    @Override
    public byte[] hook(byte[] bytes) throws Exception
    {
        return bytes;
    }
}
