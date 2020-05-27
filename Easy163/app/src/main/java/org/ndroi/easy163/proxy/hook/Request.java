package org.ndroi.easy163.proxy.hook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by andro on 2020/5/25.
 */
public class Request
{
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private Map<String, String> headerFields = new LinkedHashMap<>();
    private int headerLen = 0;
    private int contentLen = 0;
    private String method;
    private String uri;
    private String version;

    public Map<String, String> getHeaderFields()
    {
        return headerFields;
    }

    public void putBytes(byte[] bytes)
    {
        putBytes(bytes, 0, bytes.length);
    }

    public void putBytes(byte[] bytes, int offset, int length)
    {
        byteArrayOutputStream.write(bytes, offset, length);
        if(!headerReceived())
        {
            tryDecode();
        }
    }

    public boolean finished()
    {
        if(headerLen == 0)
        {
            return false;
        }
        return byteArrayOutputStream.size() >= headerLen + contentLen;
    }

    public void writeContentTo(OutputStream outputStream)
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        try
        {
            outputStream.write(bytes, headerLen, bytes.length - headerLen);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean headerReceived()
    {
        return headerLen != 0;
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

    private void tryDecode()
    {
        int crlf = checkCRLF();
        if(crlf != -1)
        {
            headerLen = crlf + 4;
            decode();
            if(method.equals("POST"))
            {
                contentLen = Integer.parseInt(headerFields.get("Content-Length"));
            }
        }
    }

    private int checkCRLF()
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        for (int i = 0; i < bytes.length - 3; i++)
        {
            if (bytes[i] == 13 && bytes[i + 1] == 10 &&
                    bytes[i + 2] == 13 && bytes[i + 3] == 10)
            {
                return i;
            }
        }
        return -1;
    }

    protected void decode()
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String headerStr = new String(bytes, 0, headerLen - 4);
        String[] lines = headerStr.split("\r\n");
        String first_line = lines[0];
        int _p = first_line.indexOf(' ');
        method = first_line.substring(0, _p);
        first_line = first_line.substring(_p + 1);
        _p = first_line.indexOf(' ');
        uri = first_line.substring(0, _p);
        version = first_line.substring(_p + 1);
        for(int i = 1; i < lines.length; i++)
        {
            String line = lines[i];
            _p = line.indexOf(':');
            String key = line.substring(0, _p).trim();
            String value = line.substring(_p + 1).trim();
            headerFields.put(key, value);
        }
    }
}

