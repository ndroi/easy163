package org.ndroi.easy163.hooks;

import android.util.Log;

import org.ndroi.easy163.vpn.hookhttp.Hook;
import org.ndroi.easy163.vpn.hookhttp.Request;

import java.util.Random;

public abstract class BaseHook extends Hook
{
    private static final String ip = String.format("175.17.%d.%d", new Random().nextInt(256), new Random().nextInt(256));

    @Override
    public void hookRequest(Request request)
    {
        request.getHeaderFields().remove("X-NAPM-RETRY");
        request.getHeaderFields().remove("Accept-Encoding");
        request.getHeaderFields().put("X-Real-IP", ip);
    }
}