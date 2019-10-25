package io.github.jroy.tagger.events;

import io.github.jroy.tagger.sql.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

@RequiredArgsConstructor
public class TaggerTagUpdateEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  @Getter
  private final UUID playerUuid;
  @Getter
  private final Tag tag;

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
