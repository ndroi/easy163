package org.ndroi.easy163.hooks;

import org.ndroi.easy163.vpn.hookhttp.Request;
import org.ndroi.easy163.vpn.hookhttp.Response;

public class MiguFileHook extends BaseHook
{
    @Override
    public boolean rule(Request request)
    {
        String host = request.getHeaderFields().get("Host");
        return host.equals("tyst.migu.cn");
    }

    @Override
    public void hookResponse(Response response)
    {
        super.hookResponse(response);
        response.setCode("206");
    }
}