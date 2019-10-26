package io.github.jroy.tagger.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import io.github.jroy.tagger.events.TaggerTagUpdateEvent;
import io.github.jroy.tagger.sql.DatabaseManager;
import io.github.jroy.tagger.sql.Tag;
import io.github.jroy.tagger.util.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TagSelectorGUI implements InventoryProvider {

  private final DatabaseManager databaseManager;
  private final InventoryManager inventoryManager;

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public void init(Player player, InventoryContents contents) {
    contents.fillRow(0, ClickableItem.empty(Utils.item(Material.BLACK_STAINED_GLASS_PANE, " ")));
    contents.fillRow(5, ClickableItem.empty(Utils.item(Material.BLACK_STAINED_GLASS_PANE, " ")));

    List<ClickableItem> tags = new ArrayList<>();

    for (Tag tag : databaseManager.getOwnedTags().get(player.getUniqueId())) {
      tags.add(ClickableItem.of(Utils.item(Material.NAME_TAG, StringUtils.capitalize(tag.getName()), "&aClick to equip!", "", "&fTag: " + tag.getText()), inventoryClickEvent -> {
        try {
          databaseManager.setActiveTag(player.getUniqueId(), tag);
          inventoryManager.getInventory(player).get().close(player);
          Bukkit.getPluginManager().callEvent(new TaggerTagUpdateEvent(player.getUniqueId(), tag));
          player.sendMessage(Utils.format("Updated your tag successfully to: " + StringUtils.capitalize(tag.getName())));
        } catch (SQLException e) {
          e.printStackTrace();
          player.sendMessage(Utils.format(ChatColor.RED + "Error while setting your tag!"));
        }
      }));
    }

    ClickableItem[] items = new ClickableItem[tags.size()];
    int i = 0;
    for (ClickableItem item : tags) {
      items[i] = item;
      i++;
    }

    Pagination pagination = contents.pagination();
    pagination.setItems(items);
    pagination.setItemsPerPage(36);
    pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 0));
    contents.set(5, 3, ClickableItem.of(Utils.item(Material.ARROW, "Previous page"),
        e -> inventoryManager.getInventory(player).get().open(player, pagination.previous().getPage())));
    contents.set(5, 5, ClickableItem.of(Utils.item(Material.ARROW, "Next page"),
        e -> inventoryManager.getInventory(player).get().open(player, pagination.next().getPage())));
    contents.set(0, 4, ClickableItem.of(Utils.item(Material.EMERALD, "&dClick to Shop", "&eClick to visit the shop and", "&ebrowse available tags!"), e -> {
      SmartInventory.builder()
          .id("tagShopGui")
          .provider(new TagShopGUI(databaseManager, inventoryManager))
          .manager(inventoryManager)
          .title("Tag Shop")
          .build().open(player);
    }));
  }

  @Override
  public void update(Player player, InventoryContents contents) {
  }
}
