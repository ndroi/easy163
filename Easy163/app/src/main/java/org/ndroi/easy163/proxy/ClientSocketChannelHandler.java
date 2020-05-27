package org.ndroi.easy163.proxy;

import android.util.Log;

import org.ndroi.easy163.proxy.buffer.ChannelBuffer;
import org.ndroi.easy163.proxy.context.ConnectionContext;
import org.ndroi.easy163.proxy.context.ProxyContext;
import org.ndroi.easy163.proxy.hook.HookHttp;
import org.ndroi.easy163.proxy.utils.Common;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientSocketChannelHandler implements EventHandler
{
    private final ProxyContext context;
    private HandlerState state = HandlerState.PARSING_INITIAL_REQUEST;

    public ClientSocketChannelHandler(ProxyContext context)
    {
        this.context = context;
    }

    @Override
    public void execute(SelectionKey selectionKey)
    {
        state = state.perform(context);
        context.cleanup();
    }

    private enum HandlerState
    {
        PARSING_INITIAL_REQUEST
                {
                    @Override
                    public HandlerState perform(ProxyContext context)
                    {
                        ConnectionContext client = context.getClient();
                        if (!client.isReadable())
                        {
                            return PARSING_INITIAL_REQUEST;
                        }
                        ChannelBuffer upstreamBuffer = context.getConnectionBuffer().upstream();
                        if (-1 == client.read(upstreamBuffer))
                        {
                            client.closeIO();
                            return PARSING_INITIAL_REQUEST;
                        }
                        Optional<List<String>> header =
                                parseInitialRequestHeader(upstreamBuffer.toViewInputStream());
                        if (!header.isPresent())
                        {
                            return PARSING_INITIAL_REQUEST;
                        }
                        Optional<RequestLine> requestLine = RequestLine.construct(header.get().get(0));
                        if (!requestLine.isPresent())
                        {
                            client.closeIO();
                            return PARSING_INITIAL_REQUEST;
                        }
                        /**
                         * for https request, discard the initial request
                         */
                        if (requestLine.get().isHttps)
                        {
                            upstreamBuffer.clear();
                        }
                        if (!createHostAndRegisterChannel(requestLine.get(), context))
                        {
                            client.closeIO();
                            return PARSING_INITIAL_REQUEST;
                        }
                        /**
                         * Wait Host to be connected
                         */
                        client.unregister(SelectionKey.OP_READ);
                        return BRIDGING;
                    }
                },

        /**
         * Host is connected
         */
        BRIDGING
                {
                    @Override
                    public HandlerState perform(ProxyContext context)
                    {
                        ConnectionContext host = context.getHost();
                        ConnectionContext client = context.getClient();
                        /**
                         * [Client -- IS --> Proxy] -- OS --> Host
                         */
                        if (client.isReadable())
                        {
                            if (host.isOutputShutdown())
                            {
                                client.shutdownIS();
                            } else
                            {
                                /* block https begin */
                                if (context.isHttps() && HookHttp.getInstance().isHttpsBlocked(host.getName()))
                                {
                                    Log.d("block https", host.getName());
                                    host.closeIO();
                                    client.closeIO();
                                    /*try
                                    {
                                        String remoteAddress = host.getChannel().getRemoteAddress().toString();
                                        Log.d("https remote address", remoteAddress);
                                    }catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }*/
                                }
                                /* block https end */
                                else
                                {
                                    ChannelBuffer upstreamBuffer = context.getConnectionBuffer().upstream();
                                    if (-1 == client.read(upstreamBuffer))
                                    {
                                        client.shutdownIS();
                                    }
                                    /**
                                     * Read Event always trigger output stream to listen on write event
                                     */
                                    host.register(SelectionKey.OP_WRITE);
                                }
                            }
                        }

                        /**
                         * [Client <-- OS -- Proxy] < -- IS -- Host
                         */
                        if (client.isWritable())
                        {
                            ChannelBuffer downstreamBuffer = context.getConnectionBuffer().downstream();
                            if (host.isInputShutdown() && downstreamBuffer.empty())
                            {
                                client.shutdownOS();
                            } else
                            {
                                if (downstreamBuffer.empty())
                                {
                                    // keep cpu free
                                    client.unregister(SelectionKey.OP_WRITE);
                                } else if (-1 == client.write(downstreamBuffer))
                                {
                                    client.shutdownOS();
                                    /**
                                     * error on output stream should always immediately terminate its corresponding input stream
                                     */
                                    host.shutdownIS();
                                }
                            }
                        }
                        return BRIDGING;
                    }
                },;

        private static Optional<List<String>>
        parseInitialRequestHeader(InputStream inputStream)
        {
            Scanner scanner = new Scanner(inputStream, "utf-8");
            List<String> ret = new ArrayList<>();
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                ret.add(line);
                if (line.equals(""))
                {
                    break;
                }
            }
            if (ret.isEmpty() || !ret.get(ret.size() - 1).equals(""))
            {
                return Optional.empty();
            } else
            {
                return Optional.of(ret);
            }
        }

        private static final Pattern PROTOCOL_MATCHER = Pattern.compile("^(https|http).*");

        private static String uri2Host(String uri)
        {
            int p = uri.indexOf("http://");
            if(p == -1) return "";
            uri = uri.substring(p + "http://".length());
            p = uri.indexOf("/");
            if(p == -1) p = uri.length();
            String host = uri.substring(0, p);
            return host;
        }

        private static boolean
        createHostAndRegisterChannel(RequestLine line, ProxyContext context)
        {
            String uri = line.uri;
            /**
             * Java URL cannot parse uri without protocol
             */
            if (line.isHttps && !PROTOCOL_MATCHER.matcher(uri).matches())
            {
                uri = "https://" + uri;
                context.markAsHttps();
            }
            /* Hook http block */
            {
                if(!line.isHttps)
                {
                    String host = uri2Host(line.uri);
                    if(HookHttp.getInstance().isHttpBlocked(host))
                    {
                        context.getClient().closeIO();
                        Log.d("block http", line.uri);
                        return false;
                    }
                }
            }
            /* Hook register */
            {
                if (!line.isHttps)
                {
                    boolean needHook = HookHttp.getInstance().checkAndHookTarget(context, uri);
                    if(needHook)
                    {
                        return true;
                    }
                }
            }
            SocketChannel hostSocketChannel = null;
            try
            {
                InetSocketAddress inetSocketAddress = constructInetSocketAddress(new URL(uri));
                hostSocketChannel = SocketChannel.open();
                hostSocketChannel.configureBlocking(false);
                HostSocketChannelHandler handler = new HostSocketChannelHandler(context);
                SelectionKey hostKey = hostSocketChannel.register(
                        context.selector(), SelectionKey.OP_CONNECT, handler);
                context.setHost(new ConnectionContext(hostKey, inetSocketAddress.getHostName()));
                hostSocketChannel.connect(inetSocketAddress);
                return true;
            } catch (MalformedURLException e)
            {
            } catch (IOException e)
            {
            } catch (UnresolvedAddressException e)
            {
            }
            Common.close(hostSocketChannel);
            return false;
        }

        private static InetSocketAddress constructInetSocketAddress(URL url)
        {
            int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
            return new InetSocketAddress(url.getHost(), port);
        }

        abstract HandlerState perform(ProxyContext context);

        /**
         * Reference https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
         */
        private static class RequestLine
        {
            private static final Pattern CONNECT_MATCHER = Pattern.compile("^connect.*", Pattern.CASE_INSENSITIVE);
            private static final Pattern SPLIT_MATCHER = Pattern.compile("\\s+");
            private final String method;
            private final String uri;
            private final String version;
            private final boolean isHttps;

            private RequestLine(String method, String uri, String version)
            {
                this.method = method;
                this.uri = uri;
                this.version = version;
                this.isHttps = CONNECT_MATCHER.matcher(method).matches();
            }

            private static Optional<RequestLine> construct(String requestLine)
            {
                String[] split = SPLIT_MATCHER.split(requestLine, 3);
                return (3 == split.length) ? Optional.of(
                        new RequestLine(split[0], split[1], split[2])) : Optional.empty();
            }
        }
    }

}
