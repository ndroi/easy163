package org.ndroi.easy163.core;

import org.ndroi.easy163.hooks.DownloadHook;
import org.ndroi.easy163.hooks.ForceCloseHook;
import org.ndroi.easy163.hooks.PlaylistHook;
import org.ndroi.easy163.hooks.SongPlayHook;
import org.ndroi.easy163.proxy.NIOHttpProxy;

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
        /* block log upload */
        proxy.addHttpsBlock("apm.music.163.com");
        proxy.addHttpsBlock("apm3.music.163.com");
        proxy.addHttpsBlock("clientlog3.music.163.com");
        proxy.addHttpsBlock("clientlog.music.163.com");
    }

    private void setHttpBlock()
    {
        /* block log upload */
        proxy.addHttpBlock("clientlog3.music.163.com");
        proxy.addHttpBlock("clientlog.music.163.com");
        proxy.addHttpBlock("apm3.music.163.com");
    }

    private void protectMyself()
    {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
    }

    private Server()
    {
        protectMyself();
        setHooks();
        setHttpsBlock();
        setHttpBlock();
    }

    public void start()
    {
        proxy.start();
    }
}
