package com.zzzliu.easy163;

import com.zzzliu.easy163.hooks.DownloadHook;
import com.zzzliu.easy163.hooks.ForceCloseHook;
import com.zzzliu.easy163.hooks.PlaylistHook;
import com.zzzliu.easy163.hooks.SongPlayHook;
import com.zzzliu.easy163.proxy.NIOHttpProxy;

public class Server
{
    private NIOHttpProxy proxy = new NIOHttpProxy(8080);
    private static Server instance = new Server();

    public static Server getInstance()
    {
        return instance;
    }

    private void setHooks()
    {
        proxy.addHook(new PlaylistHook());
        proxy.addHook(new SongPlayHook());
        proxy.addHook(new DownloadHook());
        proxy.addHook(new ForceCloseHook());
    }

    private void setHttpsBlock()
    {
        proxy.addHttpsBlock("music.163.com");
        proxy.addHttpsBlock("interface.music.163.com");
        proxy.addHttpsBlock("interface3.music.163.com");
        proxy.addHttpsBlock("apm.music.163.com");
        proxy.addHttpsBlock("apm3.music.163.com");
    }

    private Server()
    {
        setHooks();
        setHttpsBlock();
    }

    public void start()
    {
        proxy.start();
    }
}
