package org.ndroi.easy163.proxy.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread Safe Class
 */
public class HeapChannelBufferPool implements ChannelBufferPool<ChannelBuffer> {
  private final int bufferSize;
  private final AtomicInteger usedBuffers;

  public HeapChannelBufferPool(int bufferSize) {
    this.bufferSize = bufferSize;
    this.usedBuffers = new AtomicInteger(0);
  }

  @Override
  public ChannelBuffer take() {
    usedBuffers.incrementAndGet();
    return new SimpleChannelBuffer(bufferSize, this);
  }

  @Override
  public void release(ChannelBuffer buffer) {
    buffer.free();
  }

  private void doRelease() {
    usedBuffers.decrementAndGet();
  }

  @Override
  public int size() {
    return usedBuffers.get();
  }

  @Override
  public int numUsedBuffers() {
    return size();
  }

  private static class SimpleChannelBuffer implements ChannelBuffer {
    private static final int KB = 1024;
    private final ByteBuffer internal;
    private boolean isFree = false;
    private final HeapChannelBufferPool pool;

    public SimpleChannelBuffer(int size, HeapChannelBufferPool pool) {
      this.internal = ByteBuffer.allocate(size * KB);
      this.pool = pool;
    }

    /**
     * Invariant: 0 index should point to the first available byte in the buffer internal.position()
     * should points to first free space
     */
    @Override
    public int read(SocketChannel channel) throws IOException {
      return channel.read(internal);
    }

    /**
     * Invariant: 0 index should point to the first available byte in the buffer internal.position()
     * should points to first free space
     */
    @Override
    public int write(SocketChannel channel) throws IOException {
      internal.flip();
      int ret = channel.write(internal);
      internal.compact();
      return ret;
    }

    /**
     * Invariant: 0 index should point to the first available byte in the buffer internal.position()
     * should points to first free space
     */
    @Override
    public void put(byte[] bytes) {
      internal.put(bytes);
    }

    /**
     * Invariant: @{code internal}'s state should not be modified
     */
    @Override
    public InputStream toViewInputStream() {
      return new InputStream() {
        int limit = internal.position();
        int i = 0;

        @Override
        public int read() throws IOException {
          if (i < limit) {
            return internal.array()[i++] & 0xff;
          } else {
            return -1;
          }
        }
      };
    }

    @Override
    public ByteBuffer getInternalBuffer()
    {
      return internal;
    }

    /**
     * Invariant: 0 index should point to the first available byte in the buffer internal.position()
     * should points to first free space
     */
    @Override
    public int size() {
      return internal.position(); // it is obvious from the invariant
    }

    @Override
    public boolean empty() {
      return size() == 0;
    }

    @Override
    public void clear() {
      internal.clear();
    }

    @Override
    public void free() {
      if (isFree) {
        return;
      }
      isFree = true;
      pool.doRelease();
    }
  }
}
