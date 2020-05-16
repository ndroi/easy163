package com.zzzliu.easy163.proxy.hook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by andro on 2020/5/2.
 */
public class ResponseContent
{
    private byte[] contentData = null;
    private Chunks chunks = null;
    private int recvLen = 0;
    private ResponseHeader header;

    public ResponseContent(ResponseHeader header)
    {
        this.header = header;
        String val = header.getItems().get("content-length");
        if(val != null)
        {
            int length = Integer.parseInt(val);
            contentData = new byte[length];
        }else
        {
            chunks = new Chunks();
        }
    }

    public void putData(byte[] data)
    {
        putData(data, data.length);
    }

    public void putData(byte[] data, int length)
    {
        if(chunks == null)
        {
            if(recvLen >= contentData.length) return;
            System.arraycopy(data, 0, contentData, recvLen, length);
            recvLen += length;
        }else
        {
            chunks.putData(data, length);
        }
        if(isComplete())
        {
            reform();
        }
    }

    public void putInputStream(InputStream is)
    {
        if(chunks == null)
        {
            if(recvLen >= contentData.length) return;
            try
            {
                while (true)
                {
                    int b = is.read();
                    if(b == -1)
                    {
                        break;
                    }
                    contentData[recvLen] = (byte)b;
                    recvLen++;
                }
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        } else
        {
            chunks.putInputStream(is);
        }
        if(isComplete())
        {
            reform();
        }
    }

    private void reform()
    {
        if(chunks != null)
        {
            contentData = chunks.trans2Content();
            header.getItems().remove("transfer-encoding");
        }
        if(header.getItems().containsKey("content-encoding"))
        {
            byte[] unzip_contentData = GZIP.unzip(contentData);
            if(unzip_contentData != null)
            {
                contentData = unzip_contentData;
                header.getItems().remove("content-encoding");
            }
        }
        header.getItems().put("content-length", "" + contentData.length);
    }

    public byte[] getContentData()
    {
        return contentData;
    }

    public int getLength()
    {
        return contentData.length;
    }

    public boolean isComplete()
    {
        if(chunks == null)
        {
            return recvLen >= contentData.length;
        }
        else
        {
            return chunks.isComplete();
        }
    }
}
