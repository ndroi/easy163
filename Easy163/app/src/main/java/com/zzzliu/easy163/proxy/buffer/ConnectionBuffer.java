package com.zzzliu.easy163.proxy.buffer;

public class ConnectionBuffer {
  private final ChannelBuffer downstream, upstream;
  private final ChannelBufferPool<ChannelBuffer> pool;

  public ConnectionBuffer(ChannelBufferPool<ChannelBuffer> pool) {
    this.downstream = pool.take();
    this.upstream = pool.take();
    this.pool = pool;
  }

  public ChannelBuffer downstream() {
    return downstream;
  }

  public ChannelBuffer upstream() {
    return upstream;
  }

  public void free() {
    pool.release(upstream);
    pool.release(downstream);
  }
}
