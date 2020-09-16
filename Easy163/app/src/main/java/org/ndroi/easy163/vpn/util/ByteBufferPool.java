package org.ndroi.easy163.vpn.util;

import java.nio.ByteBuffer;

public class ByteBufferPool
{
    public static final int BUFFER_SIZE = 16384; // XXX: Is this ideal?

    public static ByteBuffer acquire()
    {
        //return ByteBuffer.allocate(BUFFER_SIZE);
        return ByteBuffer.allocateDirect(BUFFER_SIZE);
    }
}

