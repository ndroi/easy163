package org.ndroi.easy163.core;

import androidx.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ndroi.easy163.utils.Keyword;
import org.ndroi.easy163.utils.Song;

public class Cache {

  interface AddAction {
    Object add(String id);
  }

  private static final Object LOCK = new Object();
  private Map<String, Object> items = new LinkedHashMap<>();
  private AddAction addAction;

  public Cache(AddAction addAction) {
    this.addAction = addAction;
  }

  public void add(String id, Object value) {
    synchronized (LOCK) {
      items.put(id, value);
    }
  }

  public Object get(String id) {
    synchronized (LOCK) {
      if (items.containsKey(id)) {
        return items.get(id);
      }
      if (addAction == null) {
        return null;
      }
      Object value = addAction.add(id);
      if (value != null) {
        add(id, value);
      }
      return value;
    }
  }

  /* id --> Keyword */
  @Nullable
  public static Cache neteaseKeywords = null;

  /* id --> ProviderSong */
  @Nullable
  public static Cache providerSongs = null;

  public static void init() {
    neteaseKeywords = new Cache(Find::find);

    providerSongs = new Cache(id -> {
      Song song = Local.get(id);
      if (song != null) {
        return song;
      }
      Keyword keyword = (Keyword) neteaseKeywords.get(id);
      return Search.search(keyword);
    });
  }

  public static void clear() {
    if (providerSongs != null) {
        providerSongs.items.clear();
    }
  }
}
