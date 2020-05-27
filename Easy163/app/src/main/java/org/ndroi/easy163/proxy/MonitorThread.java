package org.ndroi.easy163.proxy;

import org.ndroi.easy163.proxy.context.SystemContext;

public class MonitorThread extends Thread {
  private final SystemContext context;
  private final int SECOND = 1000;

  public MonitorThread(SystemContext context) {
    super(MonitorThread.class.getSimpleName());
    this.context = context;
  }

  @Override
  public void run() {
    if (!context.enableMonitor()) {
      return;
    }
    while (true) {
      try {
        sleep(context.getMonitorUpdateInterval() * SECOND);
      } catch (Exception e) {
      }
    }
  }
}
