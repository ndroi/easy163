package com.zzzliu.easy163.proxy;

import com.zzzliu.easy163.proxy.context.SystemContext;
import com.zzzliu.easy163.proxy.hook.Hook;
import com.zzzliu.easy163.proxy.hook.HookHttp;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class NIOHttpProxy
{
    private final SystemContext systemContext;
    private final List<Thread> failThenTerminateJVM;

    public NIOHttpProxy(int port)
    {
        systemContext = new SystemContext.Builder()
                .clientQueue(new LinkedBlockingQueue<SocketChannel>())
                .numWorkers(Integer.parseInt(System.getProperty("worker", "8")))
                .port(port)
                .enableMonitor(Boolean.parseBoolean(System.getProperty("enableMonitor", "true")))
                /**
                 * Each proxy connection use 2 channelBufferS,
                 * one for upstream   [Client -> Host]
                 * one for downstream [Client <- Host]
                 */
                .minBuffers(Integer.parseInt(System.getProperty("minNumBuffers", "100")))
                .maxBuffers(Integer.parseInt(System.getProperty("maxNumBuffers", "200")))
                .bufferSize(Integer.parseInt(System.getProperty("bufferSize", "10"))) // unit KB
                .useDirectBuffer(Boolean.parseBoolean(System.getProperty("useDirectBuffer", "true")))
                .monitorUpdateInterval(Integer.parseInt(System.getProperty("monitorUpdateInterval", "30"))) // second
                .build();

        MonitorSingleton.init(systemContext);
        failThenTerminateJVM = Arrays.asList(
                new ConnectionListener(systemContext),
                new Dispatcher(systemContext),
                new MonitorThread(systemContext)
        );
    }

    public void start()
    {
        failThenTerminateJVM.forEach(Thread::start);
    }

    public void addHook(Hook hook)
    {
        HookHttp.getInstance().addHook(hook);
    }

    public void addHttpsBlock(String host)
    {
        HookHttp.getInstance().addHttpsBlock(host);
    }
}
