package com.zzzliu.easy163.proxy;

import com.zzzliu.easy163.proxy.context.WorkerContext;
import com.zzzliu.easy163.proxy.utils.Common;
import com.zzzliu.easy163.proxy.utils.SelectionKeyUtils;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Worker implements Runnable {
  private final WorkerContext context;

  public Worker(WorkerContext context) {
    this.context = context;
  }

  @Override
  public void run() {
    try {
      context.setName(Thread.currentThread().getName());
      Selector selector = context.getSelector();
      while (true) {
        int selected = selector.select();
        synchronized (context.getWakeupBarrier()) {
        }
        context.setNumConnections(selector.keys().size());
        if (0 == selected) {
          continue;
        }
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          if (!key.isValid()) {
          } else {
            ((EventHandler) key.attachment()).execute(key);
          }
          iterator.remove();
        }
      }
    } catch (Exception e) {
      synchronized (context.getContextSetMonitor()) {
        context.getContextSet().remove(context);
      }
      /**
       * close of selector will not immediate close the socket channel,
       * so we need to cleanup
       */
      for (SelectionKey key : context.getSelector().keys()) {
        SocketChannel socketChannel = SelectionKeyUtils.getSocketChannel(key);
        Common.close(socketChannel);
      }
      Common.close(context.getSelector());
    }
  }
}
