package org.ndroi.easy163.hooks;

import org.ndroi.easy163.vpn.hookhttp.Hook;
import org.ndroi.easy163.vpn.hookhttp.Request;

public abstract class BaseHook extends Hook
{
    public boolean isRewind = false;
    @Override
    public void hookRequest(Request request)
    {
        try
        {
            request.getHeaderFields().remove("X-NAPM-RETRY");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}