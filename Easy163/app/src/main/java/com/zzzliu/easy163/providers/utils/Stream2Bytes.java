package com.zzzliu.easy163.providers.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by andro on 2020/5/5.
 */
public class Stream2Bytes
{
    public static byte[] stream2Bytes(InputStream is)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try
        {
            while (true)
            {
                int len = is.read(buffer);
                if(len == -1) break;
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }
}
