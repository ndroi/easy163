package com.zzzliu.easy163.proxy.hook;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by andro on 2020/5/2.
 */
public class ResponseHeader extends Header
{
    private String version;
    private String status;
    private String reason;

    public String getVersion()
    {
        return version;
    }

    public String getStatus()
    {
        return status;
    }

    public String getReason()
    {
        return reason;
    }

    @Override
    protected void decode()
    {
        byte[] bytes = new byte[headerLen - 4];
        System.arraycopy(headerData, 0, bytes, 0, bytes.length);
        String header_str = new String(bytes);
        items = new LinkedHashMap<>();
        String[] lines = header_str.split("\r\n");
        String first_line = lines[0];
        int _p = first_line.indexOf(' ');
        version = first_line.substring(0, _p);
        first_line = first_line.substring(_p + 1);
        _p = first_line.indexOf(' ');
        status = first_line.substring(0, _p);
        reason = first_line.substring(_p + 1);
        for(int i = 1; i < lines.length; i++)
        {
            String line = lines[i];
            _p = line.indexOf(':');
            String key = line.substring(0, _p).trim().toLowerCase();
            String value = line.substring(_p + 1).trim().toLowerCase();
            items.put(key, value);
        }
    }

    @Override
    public byte[] encode()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(version + " " + status + " " + reason + "\r\n");
        for(Map.Entry kv : items.entrySet())
        {
            sb.append(kv.getKey() + ": " + kv.getValue() + "\r\n");
        }
        sb.append("\r\n");
        byte[] data = sb.toString().getBytes();
        return data;
    }
}

