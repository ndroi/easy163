package com.zzzliu.easy163.proxy.utils;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public enum SelectionKeyUtils {
  ;

  public static SocketChannel getSocketChannel(SelectionKey key) {
    return getChannel(key);
  }

  public static ServerSocketChannel getServerSocketChannel(SelectionKey key) {
    return getChannel(key);
  }

  private static <T extends SelectableChannel> T getChannel(SelectionKey key) {
    return Common.downCast(key.channel());
  }

  public static String getName(SelectionKey key) {
    return SocketChannelUtils.getName(getSocketChannel(key));
  }

  public static int removeInterestOps(SelectionKey key, int ops) {
    if (!key.isValid()) {
      return -1;
    }
    int newOps = key.interestOps() & ~ops;
    key.interestOps(newOps);
    return newOps;
  }

  public static int addInterestOps(SelectionKey key, int ops) {
    if (!key.isValid()) {
      return -1;
    }
    int newOps = key.interestOps() | ops;
    key.interestOps(newOps);
    return newOps;
  }
}
