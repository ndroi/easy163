package org.ndroi.easy163.proxy.utils;

import org.ndroi.easy163.proxy.hook.HookHttp;
import org.ndroi.easy163.proxy.buffer.ChannelBuffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public enum SocketChannelUtils {
  ;

  public static SocketAddress getRemoteAddress(SocketChannel socketChannel) {
    return socketChannel.socket().getRemoteSocketAddress();
  }

  public static int readFromChannel(SocketChannel channel, ChannelBuffer buffer) {
    try {
      int numOfRead = buffer.read(channel);
      if (-1 == numOfRead) {
      }
      return numOfRead;
    } catch (IOException e) {
      return -1;
    }
  }

  public static int writeToChannel(SocketChannel channel, ChannelBuffer buffer) {
    try {
      return buffer.write(channel);
    } catch (IOException e) {
      return -1;
    }
  }

  public static String getName(SocketChannel channel) {
    SocketAddress remoteSocketAddress = channel.socket().getRemoteSocketAddress();
    return null == remoteSocketAddress ? "unconnected" : remoteSocketAddress.toString();
  }
}
