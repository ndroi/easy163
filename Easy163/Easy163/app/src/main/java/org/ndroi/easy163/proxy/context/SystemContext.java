package org.ndroi.easy163.proxy.context;

import org.ndroi.easy163.proxy.buffer.ChannelBuffer;
import org.ndroi.easy163.proxy.buffer.ChannelBufferPool;
import org.ndroi.easy163.proxy.buffer.DirectChannelBufferPool;
import org.ndroi.easy163.proxy.buffer.HeapChannelBufferPool;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

/**
 * Thread Safe Class
 */
public class SystemContext {

  private final BlockingQueue<SocketChannel> clientQueue;
  private final ChannelBufferPool<ChannelBuffer> bufferPool;
  private final int port;
  private final int numWorkers;
  private final boolean enableMonitor;
  private final boolean useDirectBuffer;
  private final int minBuffers;
  private final int maxBuffers;
  private final int bufferSize;
  private final int monitorUpdateInterval;

  private SystemContext(Builder builder) {
    this.clientQueue = builder.clientQueue;
    this.port = builder.port;
    this.numWorkers = builder.numWorkers;
    this.enableMonitor = builder.enableMonitor;
    this.maxBuffers = builder.maxBuffers;
    this.minBuffers = builder.minBuffers;
    this.bufferSize = builder.bufferSize;
    this.useDirectBuffer = builder.useDirectBuffer;
    this.monitorUpdateInterval = builder.monitorUpdateInterval;
    this.bufferPool = createBufferPoll();
  }

  private ChannelBufferPool<ChannelBuffer> createBufferPoll() {
    return useDirectBuffer ? new DirectChannelBufferPool(minBuffers, maxBuffers, bufferSize) :
        new HeapChannelBufferPool(bufferSize);
  }

  public ChannelBufferPool<ChannelBuffer> getBufferPool() {
    return bufferPool;
  }

  public BlockingQueue<SocketChannel> getClientQueue() {
    return clientQueue;
  }

  public int getPort() {
    return port;
  }

  public int getNumWorkers() {
    return numWorkers;
  }

  public boolean enableMonitor() {
    return enableMonitor;
  }

  public boolean isUseDirectBuffer() {
    return useDirectBuffer;
  }

  public int getMonitorUpdateInterval() {
    return monitorUpdateInterval;
  }

  @Override
  public String toString() {
    return "";
  }

  public static class Builder {
    private BlockingQueue<SocketChannel> clientQueue;
    private int port;
    private int numWorkers;
    private boolean enableMonitor;
    private boolean useDirectBuffer;
    private int minBuffers;
    private int maxBuffers;
    private int bufferSize;
    private int monitorUpdateInterval;

    public Builder clientQueue(BlockingQueue<SocketChannel> queue) {
      this.clientQueue = queue;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder numWorkers(int num) {
      this.numWorkers = num;
      return this;
    }

    public Builder bufferSize(int size) {
      this.bufferSize = size;
      return this;
    }

    public Builder enableMonitor(boolean enableMonitor) {
      this.enableMonitor = enableMonitor;
      return this;
    }

    public Builder minBuffers(int minBuffers) {
      this.minBuffers = minBuffers;
      return this;
    }

    public Builder maxBuffers(int maxBuffers) {
      this.maxBuffers = maxBuffers;
      return this;
    }

    public Builder useDirectBuffer(boolean useDirectBuffer) {
      this.useDirectBuffer = useDirectBuffer;
      return this;
    }

    public Builder monitorUpdateInterval(int monitorUpdateInterval) {
      this.monitorUpdateInterval = monitorUpdateInterval;
      return this;
    }

    public SystemContext build() {
      return new SystemContext(this);
    }
  }
}
