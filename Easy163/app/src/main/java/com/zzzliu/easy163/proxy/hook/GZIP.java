package com.zzzliu.easy163.proxy.hook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Created by andro on 2020/5/4.
 */
public class GZIP
{
    static public byte[] unzip(byte[] bytes)
    {
        byte[] result = null;
        try
        {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true)
            {
                int len = gzipInputStream.read(buffer);
                if(len == -1) break;
                byteArrayOutputStream.write(buffer, 0, len);
            }
            gzipInputStream.close();
            byteArrayOutputStream.close();
            result = byteArrayOutputStream.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return result;
    }
}
