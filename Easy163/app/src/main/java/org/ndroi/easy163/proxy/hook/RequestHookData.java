package org.ndroi.easy163.proxy.hook;

import java.util.LinkedHashMap;
import java.util.Map;

public class RequestHookData
{
    private Map<String, String> headerFields = new LinkedHashMap<>();
    private String method;
    private String uri;
    private String version;
    private byte[] content = null;

    public byte[] getContent()
    {
        return content;
    }

    public void setContent(byte[] content)
    {
        this.content = content;
    }

    public Map<String, String> getHeaderFields()
    {
        return headerFields;
    }

    public String getMethod()
    {
        return method;
    }

    public String getUri()
    {
        return uri;
    }

    public String getVersion()
    {
        return version;
    }

    public void setMethod(String method)
    {

        this.method = method;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}