package org.ndroi.easy163.proxy.hook;

import android.util.Log;
import org.ndroi.easy163.proxy.buffer.ChannelBuffer;
import org.ndroi.easy163.proxy.context.ProxyContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by andro on 2020/5/3.
 */
public class HookManager
{
    private ProxyContext context;
    private Hook hook;
    private List<String> removedRequestHeader = Arrays.asList("Proxy-Connection", "Accept-Encoding", "x-napm-retry");

    public HookManager(ProxyContext context, Hook hook)
    {
        this.context = context;
        this.hook = hook;
    }

    public void asyncHook()
    {
        new Thread(){
            @Override
            public void run()
            {
                super.run();
                Request request = receiveRequest();
                HttpURLConnection connection = getRemoteConnection(request);
                writeBack(connection);
            }
        }.start();
    }

    private Request receiveRequest()
    {
        Request request = new Request();
        ChannelBuffer channelBuffer = context.getConnectionBuffer().upstream();
        ByteBuffer byteBuffer = channelBuffer.getInternalBuffer();
        while (!request.finished())
        {
            byteBuffer.flip();
            byte[] bytes=new byte[byteBuffer.limit()];
            byteBuffer.get(bytes);
            request.putBytes(bytes);
            byteBuffer.clear();
            int readLen = context.getClient().read(channelBuffer);
            if(readLen == -1)
            {
                context.getClient().closeIO();
                break;
            }else if(readLen == 0)
            {
                try
                {
                    Thread.sleep(10);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return request;
    }

    private HttpURLConnection getRemoteConnection(Request request)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) new URL(request.getUri()).openConnection();
            connection.setRequestMethod(request.getMethod());
            for (String key : request.getHeaderFields().keySet())
            {
                if(!removedRequestHeader.contains(key))
                {
                    connection.setRequestProperty(key, request.getHeaderFields().get(key));
                }
            }
            connection.setRequestProperty("Connection", "Close");
            connection.connect();
            if(request.getMethod().equals("POST"))
            {
                request.writeContentTo(connection.getOutputStream());
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return connection;
    }

    private void writeBack(HttpURLConnection connection)
    {
        Map<String, String> headerFields = new LinkedHashMap<>();
        for (String key : connection.getHeaderFields().keySet())
        {
            headerFields.put(key, connection.getHeaderFields().get(key).get(0));
        }
        String responseLine = headerFields.get(null);
        headerFields.remove(null);
        headerFields.remove("Transfer-Encoding");
        headerFields.remove("Content-Encoding");
        try
        {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int code = connection.getResponseCode();
            InputStream inputStream = null;
            if(200 <= code && code < 300)
            {
                inputStream = connection.getInputStream();
            }else
            {
                inputStream = connection.getErrorStream();
            }
            byte[] buffer = new byte[1024];
            while (true)
            {
                int len = inputStream.read(buffer);
                if(len == -1) break;
                byteArrayOutputStream.write(buffer, 0, len);
            }
            byte[] content = byteArrayOutputStream.toByteArray();
            try
            {
                content = hook.hook(content);
            }catch (Exception e)
            {
                Log.d("writeBack", "Hook failed");
            }
            byteArrayOutputStream.reset();
            headerFields.put("Content-Length", content.length + "");
            byteArrayOutputStream.write((responseLine + "\r\n").getBytes());
            for (String key : headerFields.keySet())
            {
                String value = headerFields.get(key);
                String item = key + ": " + value + "\r\n";
                byteArrayOutputStream.write(item.getBytes());
            }
            byteArrayOutputStream.write("\r\n".getBytes());
            byteArrayOutputStream.close();
            byte[] header = byteArrayOutputStream.toByteArray();
            doWriteBack(header);
            doWriteBack(content);
            context.getClient().closeIO();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void doWriteBack(byte[] bytes)
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        SocketChannel socketChannel = context.getClient().getChannel();
        try
        {
            while (byteBuffer.hasRemaining())
            {
                int writeLen = socketChannel.write(byteBuffer);
                if(writeLen == -1)
                {
                    context.getClient().closeIO();
                    break;
                }else if(writeLen == 0)
                {
                    try
                    {
                        Thread.sleep(10);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
