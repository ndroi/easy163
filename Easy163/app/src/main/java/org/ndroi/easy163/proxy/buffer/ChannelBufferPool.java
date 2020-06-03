package org.ndroi.easy163.proxy.buffer;

public interface ChannelBufferPool<T extends ChannelBuffer> {
  T take();
  void release(T buffer);

  int size();

  /**
   * return the current number of used buffers
   */
  int numUsedBuffers();
}
