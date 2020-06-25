package org.ndroi.easy163.proxy;

import org.ndroi.easy163.proxy.context.SystemContext;
import org.ndroi.easy163.proxy.utils.SelectionKeyUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ConnectionListener extends Thread {
  private final SystemContext systemContext;

  public ConnectionListener(SystemContext systemContext) {
    super(ConnectionListener.class.getSimpleName());
    this.systemContext = systemContext;
  }

  @Override
  public void run() {
    try {
      Selector serverSocketSelector = Selector.open();
      SelectableChannel channel = ServerSocketChannel
          .open()
          .bind(new InetSocketAddress(systemContext.getPort()))
          .configureBlocking(false);
      channel.register(serverSocketSelector, SelectionKey.OP_ACCEPT, new Handler());
      while (true) {
        int selected = serverSocketSelector.select();
        if (0 == selected) {
          continue;
        }
        Iterator<SelectionKey> iterator = serverSocketSelector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          if (!key.isValid()) {
            throw new IllegalStateException("got invalid selection key.");
          }
          ((EventHandler) key.attachment()).execute(key);
          iterator.remove();
        }
      }
    } catch (Exception e) {
    } finally {
      System.exit(-1);
    }
  }

  private class Handler implements EventHandler {
    @Override
    public void execute(SelectionKey key) {
      if (!key.isAcceptable()) {
        return;
      }
      ServerSocketChannel server = SelectionKeyUtils.getServerSocketChannel(key);
      try {
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        try {
          systemContext.getClientQueue().put(client);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          client.close();
        }
      } catch (IOException e) {
      }
    }
  }
}
