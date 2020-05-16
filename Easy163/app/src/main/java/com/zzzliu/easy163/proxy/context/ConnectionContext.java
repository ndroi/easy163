package com.zzzliu.easy163.proxy.context;

import com.zzzliu.easy163.proxy.buffer.ChannelBuffer;
import com.zzzliu.easy163.proxy.utils.SelectionKeyUtils;
import com.zzzliu.easy163.proxy.utils.SocketChannelUtils;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ConnectionContext {
  private final SelectionKey key;
  private final String name;
  private final SocketChannel channel;

  public ConnectionContext(SelectionKey key, String name) {
    this.key = key;
    this.name = name;
    channel = SelectionKeyUtils.getSocketChannel(key);
  }

  public ConnectionContext() {
    this.key = null;
    this.name = "";
    this.channel = null;
  }

  public int register(int ops) {
    return SelectionKeyUtils.addInterestOps(key, ops);
  }

  public int unregister(int ops) {
    return SelectionKeyUtils.removeInterestOps(key, ops);
  }

  public int read(ChannelBuffer buffer) {
    return SocketChannelUtils.readFromChannel(channel, buffer);
  }

  public int write(ChannelBuffer buffer) {
    return SocketChannelUtils.writeToChannel(channel, buffer);
  }

  public boolean isInputShutdown() {
    Socket socket = channel.socket();
    return socket.isClosed() || !socket.isConnected() || socket.isInputShutdown();
  }

  public boolean isOutputShutdown() {
    Socket socket = channel.socket();
    return socket.isClosed() || !socket.isConnected() || socket.isOutputShutdown();
  }

  public boolean isConnected() {
    return channel.isConnected();
  }

  public boolean isOpen() {
    return channel.isOpen();
  }

  public boolean isIOShutdown() {
    return isInputShutdown() && isOutputShutdown();
  }

  public SelectionKey getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public SocketChannel getChannel() {
    return channel;
  }

  public boolean isReadable() {
    return key.isValid() && key.isReadable();
  }

  public boolean isAcceptable() {
    return key.isValid() && key.isAcceptable();
  }

  public boolean isWritable() {
    return key.isValid() && key.isWritable();
  }

  public boolean isConnectable() {
    return key.isValid() && key.isConnectable();
  }

  public void shutdownIS() {
    tryShutdownIS();
    closeIfNeed();
  }

  private void tryShutdownIS() {
    if (!isInputShutdown()) {
      try {
        channel.shutdownInput();
        unregister(SelectionKey.OP_READ);
      } catch (IOException e) {
      }
    }
  }

  public void shutdownOS() {
    tryShutdownOS();
    closeIfNeed();
  }

  private void tryShutdownOS() {
    if (!isOutputShutdown()) {
      try {
        channel.shutdownOutput();
        unregister(SelectionKey.OP_WRITE);
      } catch (IOException e) {
      }
    }
  }

  public void closeIfNeed() {
    if (isInputShutdown() && isOutputShutdown()) {
      closeIO();
    }
  }

  public void closeIO() {
    if (!isOpen()) {
      return;
    }
    tryShutdownIS();
    tryShutdownOS();
    try {
      channel.close();
    } catch (IOException e) {
    }
  }
}
