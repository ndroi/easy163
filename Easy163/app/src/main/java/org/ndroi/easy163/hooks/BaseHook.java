package org.ndroi.easy163.hooks;

import org.ndroi.easy163.vpn.hookhttp.Hook;
import org.ndroi.easy163.vpn.hookhttp.Request;

public abstract class BaseHook extends Hook
{
    @Override
    public void hookRequest(Request request)
    {
        request.getHeaderFields().remove("X-NAPM-RETRY");
        request.getHeaderFields().remove("Accept-Encoding");
        request.getHeaderFields().put("X-Real-IP", "175.17.223.14");
    }
}