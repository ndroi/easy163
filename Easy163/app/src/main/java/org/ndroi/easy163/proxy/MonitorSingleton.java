package org.ndroi.easy163.proxy;

import org.ndroi.easy163.proxy.buffer.DirectChannelBufferPool;
import org.ndroi.easy163.proxy.context.ConnectionContext;
import org.ndroi.easy163.proxy.context.SystemContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MonitorSingleton {
  private static Monitor INSTANCE = null;

  private MonitorSingleton() {
  }

  public static Monitor get() {
    return INSTANCE;
  }

  public static void init(final SystemContext context) {
    INSTANCE = context.enableMonitor() ? create(context) : createDummy();
  }

  private static Monitor create(SystemContext context) {
    return new Monitor() {
      private final Object channelMapMonitor = new Object();
      private final Map<ConnectionContext, ConnectionContext> CHANNEL_STATS = new HashMap<>();
      private final ConnectionContext dummyContext = new ConnectionContext();

      @Override
      public void collectChannelPair(ConnectionContext client, ConnectionContext host) {
        synchronized (channelMapMonitor) {
          CHANNEL_STATS.put(client, null == host ? dummyContext : host);
        }
      }

      /**
       * This method can always dump the latest state of the channel, because the "toString" method
       * of socketChannel has internal locking ! (channel is thread-safe class)
       */
      @Override
      public String dumpStats() {
        StringBuilder sb = new StringBuilder();
        synchronized (channelMapMonitor) {
          Iterator<Map.Entry<ConnectionContext, ConnectionContext>> iterator =
              CHANNEL_STATS.entrySet().iterator();
          int activeChannels = 0;
          while (iterator.hasNext()) {
            Map.Entry<ConnectionContext, ConnectionContext> next = iterator.next();
            ConnectionContext client = next.getKey();
            ConnectionContext host = next.getValue();
            if (!client.isOpen() && (dummyContext.equals(host) || !host.isOpen())) {
              iterator.remove();
            } else {
              activeChannels++;
              String line = String.format(
                  "%s -> %s\n", client.getChannel(),
                  dummyContext.equals(host) ? "un-register" : host.getChannel());
              sb.append(line.replaceAll("java\\.nio\\.channels\\.SocketChannel", ""));
            }
          }
          sb.append("active channels <" + activeChannels + ">\n");
        }
        if (context.isUseDirectBuffer()) {
          DirectChannelBufferPool bufferPool = (DirectChannelBufferPool) context.getBufferPool();
          sb.append(String.format("un-release buffers <%d>\n", bufferPool.numUsedBuffers()));
        }
        return sb.toString();
      }
    };
  }

  private static Monitor createDummy() {
    return new Monitor() {
      @Override
      public void collectChannelPair(ConnectionContext client, ConnectionContext host) {

      }

      @Override
      public String dumpStats() {
        return "";
      }
    };
  }
}
