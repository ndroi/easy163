package com.zzzliu.easy163.proxy.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;

public interface ChannelBuffer {
  /**
   * [Channel -> Buffer] Read from channel and write to buffer
   */
  int read(SocketChannel channel) throws IOException;

  /**
   * [Buffer -> Channel] Read from buffer and write to buffer
   */
  int write(SocketChannel channel) throws IOException;

  /**
   * Put {code bytes} into buffer
   */
  void put(byte[] bytes);

  /**
   * Create a "view" input stream, which means the read operation of the input stream will not modify
   * the actual pointer of the internal buffer
   */
  InputStream toViewInputStream();

  /**
   * return the size of unconsumed data
   */
  int size();

  boolean empty();

  void clear();

  void free();
}

