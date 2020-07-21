package org.ndroi.easy163.vpn.hookhttp;

import org.ndroi.easy163.vpn.bio.BioTcpHandler;
import org.ndroi.easy163.vpn.block.BlockHttp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HookHttp
{
    private static HookHttp instance = new HookHttp();

    public static HookHttp getInstance()
    {
        return instance;
    }

    class Session
    {
        public BioTcpHandler.TcpTunnel tcpTunnel;
        public Request request = new Request();
        public Response response = new Response();
        public Hook hook = null;

        public Session(BioTcpHandler.TcpTunnel tcpTunnel)
        {
            this.tcpTunnel = tcpTunnel;
        }
    }

    private List<Hook> hooks = new ArrayList<>();
    private Map<BioTcpHandler.TcpTunnel, Session> sessions = new HashMap();

    private Hook findHook(Request request)
    {
        for (Hook hook : hooks)
        {
            if (hook.rule(request))
            {
                return hook;
            }
        }
        return null;
    }

    public void addHook(Hook hook)
    {
        hooks.add(hook);
    }

    public ByteBuffer checkAndHookRequest(BioTcpHandler.TcpTunnel tcpTunnel, ByteBuffer byteBuffer)
    {
        Session session = sessions.get(tcpTunnel);
        if (session == null)
        {
            session = new Session(tcpTunnel);
            sessions.put(tcpTunnel, session);
        }
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        session.request.putBytes(bytes);
        if (session.request.finished())
        {
            if (BlockHttp.getInstance().check(session.request))
            {
                sessions.remove(session.tcpTunnel);
                return byteBuffer;
            }
            session.hook = findHook(session.request);
            if (session.hook != null)
            {
                session.hook.hookRequest(session.request);
            } else
            {
                sessions.remove(session.tcpTunnel);
            }
            byte[] requestBytes = session.request.dump();
            byteBuffer = ByteBuffer.wrap(requestBytes);
        }
        return byteBuffer;
    }

    public ByteBuffer checkAndHookResponse(BioTcpHandler.TcpTunnel tcpTunnel, ByteBuffer byteBuffer)
    {
        Session session = sessions.get(tcpTunnel);
        if (session == null)
        {
            return byteBuffer;
        }
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        session.response.putBytes(bytes);
        if (session.response.finished())
        {
            session.hook.hookResponse(session.response);
            byte[] responseBytes = session.response.dump();
            sessions.remove(session.tcpTunnel);
            byteBuffer = ByteBuffer.wrap(responseBytes);
        }
        return byteBuffer;
    }
}
