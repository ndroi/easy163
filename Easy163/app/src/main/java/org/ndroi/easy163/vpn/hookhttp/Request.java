package org.ndroi.easy163.vpn.hookhttp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by andro on 2020/5/25.
 */
public class Request
{
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private Map<String, String> headerFields = new LinkedHashMap<>();
    private int headerLen = 0;
    private String method;
    private String uri;
    private String version;
    private byte[] content = null;

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
        if (finished()) return;
        byteArrayOutputStream.write(bytes, offset, length);
        if (!headerReceived())
        {
            tryDecode();
        }
        if (finished() && content != null)
        {
            byte[] data = byteArrayOutputStream.toByteArray();
            System.arraycopy(data, headerLen, content, 0, content.length);
        }
    }

    public boolean finished()
    {
        if (headerLen == 0)
        {
            return false;
        }
        int contentLen = (content == null ? 0 : content.length);
        return byteArrayOutputStream.size() >= headerLen + contentLen;
    }

    public byte[] getContent()
    {
        return content;
    }

    public void setContent(byte[] content)
    {
        headerFields.put("Content-Length", content.length + "");
        this.content = content;
    }

    private boolean headerReceived()
    {
        return headerLen != 0;
    }

    public String getMethod()
    {
        return method;
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
        if (crlf != -1)
        {
            headerLen = crlf + 4;
            decode();
            if (method.equals("POST"))
            {
                int contentLen = Integer.parseInt(headerFields.get("Content-Length"));
                content = new byte[contentLen];
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

    private void decode()
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String headerStr = new String(bytes, 0, headerLen - 4);
        String[] lines = headerStr.split("\r\n");
        String requestLine = lines[0];
        int _p = requestLine.indexOf(' ');
        method = requestLine.substring(0, _p);
        requestLine = requestLine.substring(_p + 1);
        _p = requestLine.indexOf(' ');
        uri = requestLine.substring(0, _p);
        version = requestLine.substring(_p + 1);
        for (int i = 1; i < lines.length; i++)
        {
            String line = lines[i];
            _p = line.indexOf(':');
            String key = line.substring(0, _p).trim();
            String value = line.substring(_p + 1).trim();
            headerFields.put(key, value);
        }
    }

    private void encode()
    {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(method + " " + uri + " " + version + "\r\n");
        for (String key : headerFields.keySet())
        {
            String value = headerFields.get(key);
            stringBuffer.append(key + ": " + value + "\r\n");
        }
        stringBuffer.append("\r\n");
        try
        {
            byteArrayOutputStream.reset();
            byteArrayOutputStream.write(stringBuffer.toString().getBytes());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public byte[] dump()
    {
        encode();
        if (content != null)
        {
            try
            {
                byteArrayOutputStream.write(content);
                byteArrayOutputStream.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}
