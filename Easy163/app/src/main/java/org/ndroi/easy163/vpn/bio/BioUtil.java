package org.ndroi.easy163.vpn.bio;

import org.ndroi.easy163.vpn.config.Config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BioUtil
{

    private static final String TAG = BioUtil.class.getSimpleName();

    public static int write(SocketChannel channel, ByteBuffer byteBuffer) throws IOException
    {
        int len = channel.write(byteBuffer);
        //Log.i(TAG, String.format("write %d %s ", len, channel.toString()));
        return len;
    }

    public static int read(SocketChannel channel, ByteBuffer byteBuffer) throws IOException
    {
        int len = channel.read(byteBuffer);
        if (Config.logRW)
        {
            //Log.d(TAG, String.format("read %d %s ", len, channel.toString()));
        }
        return len;
    }

    public static String byteToString(byte[] data, int off, int len)
    {
        len = Math.min(128, len);
        StringBuilder sb = new StringBuilder();
        for (int i = off; i < off + len; i++)
        {
            sb.append(String.format("%02x ", data[i]));
        }
        return sb.toString();
    }

}
