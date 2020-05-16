package com.zzzliu.easy163.proxy.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threadsafe Class
 *
 * Part of the code refers to https://github.com/midonet/midonet/blob/master/netlink/src/main/java/org/midonet/netlink/BufferPool.java
 *
 * This implementation removes the checking ownership checking of byte buffer when it is released
 */
public class DirectChannelBufferPool implements ChannelBufferPool<ChannelBuffer> {
  private static final int KB = 1024;
  private final int minNumBuffers;
  private final int maxNumBuffers;
  private final int bufferSize;
  private final BlockingQueue<ByteBuffer> pool;
  private final AtomicInteger numBuffers;
  private final AtomicInteger usedBuffers;

  /**
   * @param bufferSize in KB
   */
  public DirectChannelBufferPool(int minNumBuffers, int maxNumBuffers, int bufferSize) {
//    Preconditions.checkArgument(
//        minNumBuffers < maxNumBuffers,
//        "minNumBuffers should not greater than maxNumBuffers"
//    );
//    Preconditions.checkArgument(
//        minNumBuffers > 0 && maxNumBuffers > 0 && bufferSize > 0,
//        "minNumBuffers, maxNumBuffers, bufferSize should > 0"
//    );
    this.minNumBuffers = minNumBuffers;
    this.maxNumBuffers = maxNumBuffers;
    this.bufferSize = bufferSize * KB;
    this.pool = new ArrayBlockingQueue<>(maxNumBuffers);
    this.numBuffers = new AtomicInteger(0);
    this.usedBuffers = new AtomicInteger(0);
    init();
  }

  private void init() {
    while (numBuffers.getAndIncrement() < minNumBuffers) {
      pool.offer(ByteBuffer.allocate(bufferSize));
    }
  }

  @Override
  public int size() {
    return numBuffers.get();
  }

  @Override
  public int numUsedBuffers() {
    return usedBuffers.get();
  }

  @Override
  public ChannelBuffer take() {
    usedBuffers.incrementAndGet();
    ByteBuffer byteBuffer = pool.poll();
    if (null == byteBuffer) {
      if (numBuffers.incrementAndGet() <= maxNumBuffers) {
        byteBuffer = ByteBuffer.allocateDirect(bufferSize);
      } else {
        numBuffers.decrementAndGet();
        byteBuffer = ByteBuffer.allocate(bufferSize);
      }
    }
    byteBuffer.clear();
    return new DirectChannelBuffer(byteBuffer, this);
  }

  @Override
  public void release(ChannelBuffer channelBuffer) {
    channelBuffer.free();
  }

  private void doRelease(ByteBuffer byteBuffer) {
    usedBuffers.decrementAndGet();
    if (null == byteBuffer || !byteBuffer.isDirect()) {
      return;
    }
    pool.offer(byteBuffer);
  }

  private static class DirectChannelBuffer implements ChannelBuffer {
    private final ByteBuffer internal;
    private final DirectChannelBufferPool pool;
    private boolean isFree = false;

    public DirectChannelBuffer(ByteBuffer byteBuffer, DirectChannelBufferPool pool) {
      this.internal = byteBuffer;
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
      return new IncrementalInputStream(internal.asReadOnlyBuffer());
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
      pool.doRelease(internal);
      isFree = true;
    }

    /**
     * this input stream will not read everything in the byte buffer into the on heap cache.
     * Instead, it will read it CACHE_SIZE each time when there is not enough data in the on heap
     * cache.
     */
    private static class IncrementalInputStream extends InputStream {
      private static int CACHE_SIZE = 1024;
      private final byte[] onHeapCache = new byte[CACHE_SIZE];
      private final ByteBuffer byteBuffer;
      private int currentLimit;
      private int i;

      IncrementalInputStream(ByteBuffer readOnlyByteBuffer) {
        readOnlyByteBuffer.flip();
        this.byteBuffer = readOnlyByteBuffer;
        this.currentLimit = 0;
        this.i = 0;
      }

      @Override
      public int read() throws IOException {
        if (i == currentLimit) {
          i = 0;
          this.currentLimit = Math.min(byteBuffer.remaining(), CACHE_SIZE);
          if (this.currentLimit == 0) {
            return -1;
          }
          byteBuffer.get(onHeapCache, 0, currentLimit);
        }
        return onHeapCache[i++] & 0xff;
      }
    }
  }
}
