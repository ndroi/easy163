package org.ndroi.easy163.proxy;

import org.ndroi.easy163.proxy.context.ConnectionContext;

public interface Monitor {
  void collectChannelPair(ConnectionContext client, ConnectionContext host);

  String dumpStats();
}
