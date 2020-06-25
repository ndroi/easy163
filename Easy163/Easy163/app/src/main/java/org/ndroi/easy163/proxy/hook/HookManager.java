package org.ndroi.easy163.proxy.hook;

import android.util.Log;
import org.ndroi.easy163.proxy.buffer.ChannelBuffer;
import org.ndroi.easy163.proxy.context.ProxyContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andro on 2020/5/3.
 */
public class HookManager
{
    private ProxyContext context;
    private Hook hook;
    private List<String> RequestHeaderToRemove = Arrays.asList(
            "Connection",
            "Proxy-Connection",
            "Accept-Encoding",
            "X-NAPM-RETRY");
    private List<String> ResponseHeaderToRemove = Arrays.asList(
            null,
            "Transfer-Encoding",
            "Content-Encoding");

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
        while (true)
        {
            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.limit()];
            byteBuffer.get(bytes);
            Log.d("receiveRequest::phase_1", new String(bytes));
            request.putBytes(bytes);
            if(request.finished())
            {
                break;
            }
            byteBuffer.clear();
            int readLen = context.getClient().read(channelBuffer);
            Log.d("receiveRequest::phase_2", new String(bytes));
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
        RequestHookData requestHookData = new RequestHookData();
        for (String key : request.getHeaderFields().keySet())
        {
            if(!RequestHeaderToRemove.contains(key))
            {
                requestHookData.getHeaderFields().put(key, request.getHeaderFields().get(key));
            }
        }
        requestHookData.setMethod(request.getMethod());
        requestHookData.setUri(request.getUri());
        requestHookData.setVersion(request.getVersion());
        if(requestHookData.getMethod().equals("POST"))
        {
            requestHookData.setContent(request.getContent());
        }
        HttpURLConnection connection = null;
        try
        {
            hook.hookRequest(requestHookData);
            connection = (HttpURLConnection) new URL(requestHookData.getUri()).openConnection();
            connection.setRequestMethod(requestHookData.getMethod());
            for (String key : requestHookData.getHeaderFields().keySet())
            {
                connection.setRequestProperty(key, requestHookData.getHeaderFields().get(key));
            }
            connection.setRequestProperty("Connection", "Close");
            connection.connect();
            if(requestHookData.getMethod().equals("POST"))
            {
                connection.getOutputStream().write(requestHookData.getContent());
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return connection;
    }

    private void writeBack(HttpURLConnection connection)
    {
        ResponseHookData responseHookData = new ResponseHookData();
        String responseLine = connection.getHeaderFields().get(null).get(0);
        responseHookData.applyResponseLine(responseLine);
        for (String key : connection.getHeaderFields().keySet())
        {
            if(!ResponseHeaderToRemove.contains(key))
            {
                responseHookData.getHeaderFields().put(key, connection.getHeaderFields().get(key).get(0));
            }
        }
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
            byte[] bytes = new byte[4096];
            while (true)
            {
                int len = inputStream.read(bytes);
                if(len == -1) break;
                byteArrayOutputStream.write(bytes, 0, len);
            }
            responseHookData.setContent(byteArrayOutputStream.toByteArray());
            try
            {
                hook.hookResponse(responseHookData);
                responseHookData.getHeaderFields().put("Content-Length", responseHookData.getContent().length + "");
            }catch (Exception e)
            {
                Log.d("hookResponse", "Hook failed");
                e.printStackTrace();
            }
            byteArrayOutputStream.reset();
            byteArrayOutputStream.write((responseHookData.generateResponseLine() + "\r\n").getBytes());
            for (String key : responseHookData.getHeaderFields().keySet())
            {
                String value = responseHookData.getHeaderFields().get(key);
                String item = key + ": " + value + "\r\n";
                byteArrayOutputStream.write(item.getBytes());
            }
            byteArrayOutputStream.write("\r\n".getBytes());
            byteArrayOutputStream.close();
            doWriteBack(byteArrayOutputStream.toByteArray());
            doWriteBack(responseHookData.getContent());
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