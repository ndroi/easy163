package com.zzzliu.easy163.proxy.hook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by andro on 2020/5/3.
 */
public class HookManager
{
    SocketChannel channel;
    private Hook hook;
    private ResponseHeader responseHeader = new ResponseHeader();
    private ResponseContent responseContent = null;
    Thread thread = null;

    public HookManager(SocketChannel channel, Hook hook)
    {
        this.channel = channel;
        this.hook = hook;
    }

    public void collectDataAndHook(InputStream is)
    {
        if (!responseHeader.isComplete())
        {
            responseHeader.putInputStream(is);
        }
        if (responseHeader.isComplete() && responseContent == null)
        {
            if (hook.type == Hook.Type.NORMAL)
            {
                responseContent = new ResponseContent(responseHeader);
                responseContent.putData(responseHeader.getRemainingData());
            } else if (hook.type == Hook.Type.ForceClose)
            {
                startHook();
            }
        }
        if (responseContent != null && !responseContent.isComplete())
        {
            responseContent.putInputStream(is);
        }
        if (responseContent != null && responseContent.isComplete())
        {
            startHook();
        }
    }

    private void writeBack(byte[] bytes) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining())
        {
            int w = channel.write(buffer);
            if (w == 0)
            {
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            } else if (w == -1) throw new IOException();
        }
    }

    private void startHook()
    {
        HookHttp.getInstance().UnRegisterTarget(channel);
        hook.forceClose(responseHeader);
        if (hook.type == Hook.Type.NORMAL)
        {
            thread = new Thread()
            {
                @Override
                public void run()
                {
                    super.run();
                    byte[] contentData = null;
                    try
                    {
                        contentData = hook.hook(responseContent.getContentData());
                        responseHeader.getItems().put("content-length", "" + contentData.length);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                        contentData = responseContent.getContentData();
                    }
                    byte[] headerData = responseHeader.encode();
                    try
                    {
                        writeBack(headerData);
                        writeBack(contentData);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                        try
                        {
                            channel.close();
                        } catch (IOException e1)
                        {
                            e1.printStackTrace();
                        }
                    }
                }
            };
            thread.start();
        } else if (hook.type == Hook.Type.ForceClose)
        {
            try
            {
                writeBack(responseHeader.encode());
                writeBack(responseHeader.getRemainingData());
            } catch (IOException e)
            {
                e.printStackTrace();
                try
                {
                    channel.close();
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
        }
    }
}