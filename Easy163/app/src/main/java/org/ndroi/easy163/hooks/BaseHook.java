package org.ndroi.easy163.hooks;

import org.ndroi.easy163.vpn.hookhttp.Hook;
import org.ndroi.easy163.vpn.hookhttp.Request;

public abstract class BaseHook extends Hook
{
    @Override
    public void hookRequest(Request request)
    {
        request.getHeaderFields().remove("X-NAPM-RETRY");
    }
}