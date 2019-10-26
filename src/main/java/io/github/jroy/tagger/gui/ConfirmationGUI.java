package io.github.jroy.tagger.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import io.github.jroy.tagger.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConfirmationGUI implements InventoryProvider {

  private SmartInventory inventory;
  private Consumer<Boolean> onSelection;
  private List<String> info;

  //turn this consumer into two consumers.
  private ConfirmationGUI(Consumer<Boolean> onSelection, String title, List<String> info, Player player, InventoryManager inventoryManager) {
    this.onSelection = onSelection;
    this.info = info;
    this.inventory = SmartInventory.builder()
        .id("ConfirmationGui")
        .provider(this)
        .manager(inventoryManager)
        .size(3, 9)
        .title(title)
        .build();
    inventory.open(player);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    contents.set(1, 3, ClickableItem.of(Utils.item(Material.GREEN_WOOL, "&aYES"),
        e -> {
          inventory.close(player);
          onSelection.accept(true);
        }));
    contents.set(1, 5, ClickableItem.of(Utils.item(Material.RED_WOOL, "&cNO"),
        e -> {
          inventory.close(player);
          onSelection.accept(false);
        }));

    if (info.size() > 0) {
      contents.set(0, 4, ClickableItem.empty(Utils.item(Material.PAPER, "&c&lWarning!", false, info)));
    }
  }

  @Override
  public void update(Player player, InventoryContents contents) {

  }

  static class Builder {
    private String title = "Confirm";
    private List<String> info = new ArrayList<>();
    private Consumer<Boolean> onSelected = e -> {
    };
    private InventoryManager inventoryManager = null;

    ConfirmationGUI.Builder onChoiceMade(Consumer<Boolean> onSelected) {
      this.onSelected = onSelected;
      return this;
    }

    ConfirmationGUI.Builder title() {
      this.title = "Confirmation";
      return this;
    }

    ConfirmationGUI.Builder info(List<String> info) {
      this.info = info;
      return this;
    }

    ConfirmationGUI.Builder inventoryManager(InventoryManager inventoryManager) {
      this.inventoryManager = inventoryManager;
      return this;
    }

    void open(Player player) {
      new ConfirmationGUI(onSelected, title, info, player, inventoryManager);
    }
  }


}
