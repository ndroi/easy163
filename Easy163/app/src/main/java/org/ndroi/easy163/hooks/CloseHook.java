package org.ndroi.easy163.hooks;

import org.ndroi.easy163.proxy.hook.Hook;

public class CloseHook extends Hook
{
    @Override
    public boolean rule(String method, String uri)
    {
        return uri2Host(uri).endsWith("music.163.com");
    }
}