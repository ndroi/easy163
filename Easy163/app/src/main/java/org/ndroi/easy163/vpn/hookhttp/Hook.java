package org.ndroi.easy163.vpn.hookhttp;

abstract public class Hook
{
    public abstract boolean rule(Request request);

    public void hookRequest(Request request)
    {

    }

    public void hookResponse(Response response)
    {

    }

    protected String getPath(Request request)
    {
        String path = request.getUri();
        int p = path.indexOf("?");
        if (p != -1)
        {
            path = path.substring(0, p);
        }
        return path;
    }
}
