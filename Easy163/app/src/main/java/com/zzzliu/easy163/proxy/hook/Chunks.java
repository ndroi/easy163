package com.zzzliu.easy163.proxy.hook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andro on 2020/5/3.
 */
public class Chunks
{
    static private class Chunk
    {
        public byte[] data;
        public int recvLen = 0;

        public Chunk(int length)
        {
            data = new byte[length];
        }

        public void put(byte b)
        {
            if(isComplete()) return;
            if(recvLen < data.length)
            {
                data[recvLen] = b;
            }
            recvLen++;
        }

        public boolean isComplete()
        {
            /* include crlf */
            return recvLen >= data.length + 2;
        }

        public int getLength()
        {
            return data.length;
        }

        public byte[] getData()
        {
            return data;
        }
    }

    static private class ChunkLength
    {
        private byte[] data = new byte[16];
        private int recvLen = 0;

        public void reset()
        {
            recvLen = 0;
        }

        public void put(byte b)
        {
            if(isComplete()) return;
            data[recvLen] = b;
            recvLen++;
        }

        public boolean isComplete()
        {
            return recvLen > 0 && data[recvLen - 1] == '\n';
        }

        public int getLength()
        {
            if(!isComplete()) return 0;
            int length = Integer.parseInt(new String(data, 0, recvLen - 2), 16);
            return length;
        }
    }

    private List<Chunk> chunkList = new ArrayList<>();
    private boolean isComplete = false;
    private ChunkLength curChunkLength = new ChunkLength();

    private Chunk getLastChunk()
    {
        if(chunkList.size() == 0) return null;
        Chunk lastChunk = chunkList.get(chunkList.size() - 1);
        return lastChunk;
    }

    private Chunk getLastUnCompleteChunk()
    {
        if(chunkList.size() == 0) return null;
        Chunk lastChunk = chunkList.get(chunkList.size() - 1);
        if(lastChunk.isComplete()) return null;
        return lastChunk;
    }

    private void putData(byte b)
    {
        if(isComplete()) return;
        Chunk lastUnCompleteChunk = getLastUnCompleteChunk();
        if (!curChunkLength.isComplete())
        {
            curChunkLength.put((byte) b);
        } else
        {
            if (lastUnCompleteChunk == null)
            {
                int chunkLength = curChunkLength.getLength();
                if (chunkLength == 0)
                {
                    isComplete = true;
                    return;
                }
                Chunk newChunk = new Chunk(chunkLength);
                newChunk.put((byte)b);
                chunkList.add(newChunk);
            } else
            {
                lastUnCompleteChunk.put((byte) b);
            }
            Chunk lastChunk = getLastChunk();
            if(lastChunk != null && lastChunk.isComplete())
            {
                curChunkLength.reset();
            }
        }
    }

    public void putData(byte[] data)
    {
        putData(data, data.length);
    }

    public void putData(byte[] data, int length)
    {
        for (int i = 0; i < length; i++)
        {
            putData(data[i]);
        }
    }

    public void putInputStream(InputStream is)
    {
        if(isComplete()) return;
        try
        {
            while (true)
            {
                int b = is.read();
                if(b == -1)
                {
                    break;
                }
                putData((byte)b);
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isComplete()
    {
        return isComplete;
    }

    public byte[] trans2Content()
    {
        int lengthSum = 0;
        for (Chunk chunk : chunkList)
        {
            lengthSum += chunk.getLength();
        }
        byte[] content = new byte[lengthSum];
        int offset = 0;
        for (Chunk chunk : chunkList)
        {
            System.arraycopy(chunk.getData(), 0, content, offset, chunk.getLength());
            offset += chunk.getLength();
        }
        return content;
    }
}
