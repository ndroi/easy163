package org.ndroi.easy163.core;

import org.ndroi.easy163.hooks.CloseHook;
import org.ndroi.easy163.hooks.CollectHook;
import org.ndroi.easy163.hooks.DownloadHook;
import org.ndroi.easy163.hooks.MiguFileHook;
import org.ndroi.easy163.hooks.PlaylistHook;
import org.ndroi.easy163.hooks.SongPlayHook;
import org.ndroi.easy163.proxy.NIOHttpProxy;

public class Server
{
    private int port = 8163;
    private NIOHttpProxy proxy = new NIOHttpProxy(port);
    private static Server instance = new Server();

    public static Server getInstance()
    {
        return instance;
    }

    private void setHooks()
    {
        proxy.addHook(new PlaylistHook());
        proxy.addHook(new SongPlayHook());
        proxy.addHook(new CollectHook());
        proxy.addHook(new DownloadHook());
        proxy.addHook(new MiguFileHook());
        proxy.addHook(new CloseHook());
    }

    private void setHttpsBlock()
    {
        /* block https api */
        proxy.addHttpsBlock("music.163.com");
        proxy.addHttpsBlock("interface3.music.163.com");
        proxy.addHttpsBlock("interface.music.163.com");
        /* block log upload */
        proxy.addHttpsBlock("apm3.music.163.com");
        proxy.addHttpsBlock("apm.music.163.com");
        proxy.addHttpsBlock("clientlog3.music.163.com");
        proxy.addHttpsBlock("clientlog.music.163.com");
    }

    private void setHttpBlock()
    {
        /* for some bugs, and the reasons are not known clearly */
        proxy.addHttpBlock("127.0.0.1:" + port);
        proxy.addHttpBlock("127.0.0.1:2017");
        proxy.addHttpBlock("localhost:" + port);
        proxy.addHttpBlock("localhost:2017");
        /* block log upload */
        proxy.addHttpBlock("clientlog3.music.163.com");
        proxy.addHttpBlock("clientlog.music.163.com");
        proxy.addHttpBlock("apm3.music.163.com");
        proxy.addHttpBlock("apm.music.163.com");
    }

    private void protectMyself()
    {
        /* make this app self bypass proxy */
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
