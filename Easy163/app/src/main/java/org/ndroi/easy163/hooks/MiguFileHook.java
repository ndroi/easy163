package org.ndroi.easy163.hooks;

import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.proxy.hook.ResponseHookData;

public class MiguFileHook extends Hook
{
    @Override
    public boolean rule(String method, String uri)
    {
        return uri2Host(uri).equals("tyst.migu.cn");
    }

    @Override
    public void hookResponse(ResponseHookData data)
    {
        data.setCode("206");
    }
}