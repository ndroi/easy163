package org.ndroi.easy163.core;

import org.ndroi.easy163.hooks.CollectHook;
import org.ndroi.easy163.hooks.DownloadHook;
import org.ndroi.easy163.hooks.MiguFileHook;
import org.ndroi.easy163.hooks.PlaylistHook;
import org.ndroi.easy163.hooks.SongPlayHook;
import org.ndroi.easy163.vpn.block.BlockHttp;
import org.ndroi.easy163.vpn.block.BlockHttps;
import org.ndroi.easy163.vpn.hookhttp.HookHttp;

public class Server
{
    private static Server instance = new Server();

    public static Server getInstance()
    {
        return instance;
    }

    private void setHooks()
    {
        HookHttp.getInstance().addHook(new PlaylistHook());
        HookHttp.getInstance().addHook(new SongPlayHook());
        HookHttp.getInstance().addHook(new CollectHook());
        HookHttp.getInstance().addHook(new DownloadHook());
        HookHttp.getInstance().addHook(new MiguFileHook());
    }

    private void setHttpsBlock()
    {
        BlockHttps.getInstance().addHost("music.163.com");
        BlockHttps.getInstance().addHost("interface3.music.163.com");
        BlockHttps.getInstance().addHost("interface.music.163.com");
        BlockHttps.getInstance().addHost("apm.music.163.com");
        BlockHttps.getInstance().addHost("apm3.music.163.com");
        BlockHttps.getInstance().addHost("clientlog3.music.163.com");
        BlockHttps.getInstance().addHost("clientlog.music.163.com");
    }

    private void setHttpBlock()
    {
        
    }

    public void start()
    {
        setHooks();
        setHttpsBlock();
        setHttpBlock();
    }
}
