package com.zzzliu.easy163.proxy.hook;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by andro on 2020/5/2.
 */

abstract public class Header
{
    protected Map<String, String> items = null;
    protected byte[] headerData = new byte[8192*10];
    protected int recvLen = 0;
    protected int headerLen = 0; // <= recvLen

    public Map<String, String> getItems()
    {
        return items;
    }

    private int checkCRLF()
    {
        for (int i = 0; i < recvLen - 3; i++)
        {
            if (headerData[i] == 13 &&
                    headerData[i + 1] == 10 &&
                    headerData[i + 2] == 13 &&
                    headerData[i + 3] == 10)
            {
                return i;
            }
        }
        return -1;
    }

    protected abstract void decode();

    public abstract byte[] encode();

    private void tryDecode()
    {
        int crlf = checkCRLF();
        if(crlf == -1) return;
        headerLen = crlf + 4;
        decode();
    }

    public void putData(byte[] data)
    {
        putData(data, data.length);
    }

    public void putData(byte[] data, int length)
    {
        System.arraycopy(data, 0, headerData, recvLen, length);
        recvLen += length;
        tryDecode();
    }

    public void putInputStream(InputStream is)
    {
        try
        {
            while (true)
            {
                int b = is.read();
                if(b == -1) break;
                headerData[recvLen] = (byte)b;
                recvLen++;
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        }
        tryDecode();
    }

    public byte[] getRemainingData()
    {
        if(headerLen == 0) return null;
        byte[] rem = new byte[recvLen - headerLen];
        System.arraycopy(headerData, headerLen, rem, 0, rem.length);
        return rem;
    }

    public boolean isComplete()
    {
        return items != null;
    }
}

