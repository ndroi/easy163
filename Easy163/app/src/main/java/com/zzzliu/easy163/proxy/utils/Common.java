package com.zzzliu.easy163.proxy.utils;

import java.io.Closeable;
import java.io.IOException;

public enum Common {
  ;

  @SuppressWarnings("unchecked")
  public static <Super, Sub extends Super> Sub downCast(Super sp) {
    return (Sub) sp;
  }

  public static void close(Closeable closeable, String name) {
    if (null == closeable) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
    }
  }

  public static void close(Closeable closeable) {
    close(closeable, null);
  }
}
