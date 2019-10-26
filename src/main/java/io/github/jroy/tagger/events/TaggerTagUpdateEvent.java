package io.github.jroy.tagger.events;

import io.github.jroy.tagger.sql.Tag;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class TaggerTagUpdateEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  @Getter
  private final UUID playerUuid;
  @Getter
  private final Tag tag;

  public TaggerTagUpdateEvent(UUID playerUuid, Tag tag) {
    super(!Bukkit.getServer().isPrimaryThread());
    this.playerUuid = playerUuid;
    this.tag = tag;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
