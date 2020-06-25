package org.ndroi.easy163.proxy.hook;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseHookData
{
    private Map<String, String> headerFields = new LinkedHashMap<>();
    private String version;
    private String code;
    private String desc;
    private byte[] content = null;

    public void applyResponseLine(String line)
    {
        int _p = line.indexOf(' ');
        version = line.substring(0, _p);
        line = line.substring(_p + 1);
        _p = line.indexOf(' ');
        code = line.substring(0, _p);
        desc = line.substring(_p + 1);
    }

    public String generateResponseLine()
    {
        return version + " " + code + " " + desc;
    }

    public byte[] getContent()
    {
        return content;
    }

    public void setContent(byte[] content)
    {
        this.content = content;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public void setDesc(String desc)
    {
        this.desc = desc;
    }

    public Map<String, String> getHeaderFields()
    {

        return headerFields;
    }

    public String getVersion()
    {
        return version;
    }

    public String getCode()
    {
        return code;
    }

    public String getDesc()
    {
        return desc;
    }
}