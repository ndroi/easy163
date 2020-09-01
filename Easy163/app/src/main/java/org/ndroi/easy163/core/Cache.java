package org.ndroi.easy163.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ndroi.easy163.utils.Keyword;

public class Cache {

  interface AddAction {
    Object add(String id);
  }

  private static abstract class DiskSaver {

    public abstract void load(Map<String, Object> items);

    public abstract void update(String id, Object value);

    public abstract void onCacheHit(String id, Object value);
  }

  private Map<String, Object> items = new LinkedHashMap<>();
  private @NonNull
  AddAction addAction;
  private @Nullable
  DiskSaver diskSaver = null;
  private final Object LOCK = new Object();

  public Cache(@NonNull AddAction addAction) {
    this.addAction = addAction;
  }

  public Cache(@NonNull AddAction addAction, @NonNull DiskSaver diskSaver) {
    this.addAction = addAction;
    this.diskSaver = diskSaver;
    diskSaver.load(items);
  }

  public void add(String id, Object value) {
    synchronized (LOCK) {
      items.put(id, value);
      if (diskSaver != null) {
        diskSaver.update(id, value);
      }
    }
  }

  public Object get(String id) {
    synchronized (LOCK) {
      if (items.containsKey(id)) {
        Object value = items.get(id);

        if (diskSaver != null) {
          diskSaver.onCacheHit(id, value);
        }
        return value;
      }

      Object value = addAction.add(id);

      if (value != null) {
        add(id, value);
      }
      return value;
    }
  }

  /* id --> Keyword */
  public static Cache neteaseKeywords = null;

  /* id --> ProviderSong */
  public static Cache providerSongs = null;

  public static void Init() {
    neteaseKeywords = new Cache(Find::find);

    providerSongs = new Cache(id -> {
      Keyword keyword = (Keyword) neteaseKeywords.get(id);
      return Search.search(keyword);
    });
  }

  public static void Clear() {
    providerSongs.items.clear();
  }
}
