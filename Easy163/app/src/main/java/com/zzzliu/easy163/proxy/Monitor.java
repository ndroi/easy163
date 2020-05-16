package com.zzzliu.easy163.proxy;

import com.zzzliu.easy163.proxy.context.ConnectionContext;

public interface Monitor {
  void collectChannelPair(ConnectionContext client, ConnectionContext host);

  String dumpStats();
}
