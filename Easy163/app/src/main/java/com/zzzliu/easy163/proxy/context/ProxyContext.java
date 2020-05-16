package com.zzzliu.easy163.proxy.context;

import com.zzzliu.easy163.proxy.buffer.ConnectionBuffer;

import java.nio.channels.Selector;

public class ProxyContext {
  private final ConnectionBuffer connectionBuffer;
  private ConnectionContext client;
  private ConnectionContext host;
  private boolean isHttps = false;

  public ProxyContext(SystemContext systemContext) {
    this.connectionBuffer = new ConnectionBuffer(systemContext.getBufferPool());
  }

  public ConnectionContext getClient() {
    return client;
  }

  public void setClient(ConnectionContext client) {
    this.client = client;
  }

  public ConnectionContext getHost() {
    return host;
  }

  public void setHost(ConnectionContext host) {
    this.host = host;
  }

  public void markAsHttps() {
    this.isHttps = true;
  }

  public boolean isHttps() {
    return isHttps;
  }

  public ConnectionBuffer getConnectionBuffer() {
    return connectionBuffer;
  }

  public void cleanup() {
    if (!client.isOpen() && (null == host || !host.isOpen())) {
      connectionBuffer.free();
    }
  }

  public Selector selector() {
    return getClient().getKey().selector();
  }
}
