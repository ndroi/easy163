package org.ndroi.easy163.proxy.context;

import java.nio.channels.Selector;
import java.util.Set;

public class WorkerContext {
  private final Object wakeupBarrier = new Object();
  private final Object contextSetMonitor;
  private final Set<WorkerContext> contextSet;
  private final Selector selector;
  private volatile String name = "unnamed (not-start)";
  private volatile int numConnections = 0;

  private WorkerContext(Builder builder) {
    this.selector = builder.selector;
    this.contextSet = builder.contextSet;
    this.contextSetMonitor = builder.contextSetMonitor;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getNumConnections() {
    return numConnections;
  }

  public void setNumConnections(int numConnections) {
    this.numConnections = numConnections;
  }

  public Object getWakeupBarrier() {
    return wakeupBarrier;
  }

  public Selector getSelector() {
    return selector;
  }

  public Object getContextSetMonitor() {
    return contextSetMonitor;
  }

  public Set<WorkerContext> getContextSet() {
    return contextSet;
  }

  public static class Builder {
    private Selector selector;
    private Set<WorkerContext> contextSet;
    private Object contextSetMonitor;

    public Builder() {
    }

    public Builder selector(Selector selector) {
      this.selector = selector;
      return this;
    }

    public Builder contextSetMonitor(Object contextSetMonitor) {
      this.contextSetMonitor = contextSetMonitor;
      return this;
    }

    public Builder contextSet(Set<WorkerContext> contextSet) {
      this.contextSet = contextSet;
      return this;
    }

    public WorkerContext build() {
      return new WorkerContext(this);
    }
  }
}
