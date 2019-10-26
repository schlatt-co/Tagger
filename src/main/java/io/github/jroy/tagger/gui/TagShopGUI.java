package io.github.jroy.tagger.gui;

import dev.tycho.stonks.managers.DatabaseHelper;
import dev.tycho.stonks.model.core.Account;
import dev.tycho.stonks.model.core.AccountLink;
import dev.tycho.stonks.model.logging.Transaction;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import io.github.jroy.tagger.Tagger;
import io.github.jroy.tagger.sql.DatabaseManager;
import io.github.jroy.tagger.sql.Tag;
import io.github.jroy.tagger.util.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TagShopGUI implements InventoryProvider {

  private final DatabaseManager databaseManager;
  private final InventoryManager inventoryManager;

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public void init(Player player, InventoryContents contents) {
    contents.fillRow(0, ClickableItem.empty(Utils.item(Material.BLACK_STAINED_GLASS_PANE, " ")));
    contents.fillRow(5, ClickableItem.empty(Utils.item(Material.BLACK_STAINED_GLASS_PANE, " ")));

    List<Tag> allTags = new ArrayList<>(databaseManager.getCachedTags().values());
    List<String> ownedTags = new ArrayList<>();
    for (Tag tag : databaseManager.getOwnedTags().get(player.getUniqueId())) {
      ownedTags.add(tag.getName());
    }

    List<ClickableItem> tags = new ArrayList<>();
    for (Tag tag : allTags) {
      if (tag.getPrice() == 0 || ownedTags.contains(tag.getName())) {
        continue;
      }
      tags.add(ClickableItem.of(Utils.item(Material.NAME_TAG, StringUtils.capitalize(tag.getName()), "&aClick to purchase!", "", "&cPrice: $" + tag.getPrice(), "&fText: " + tag.getText()), inventoryClickEvent -> new ConfirmationGUI.Builder()
          .title()
          .info(Arrays.asList("&eClicking \"YES\" will accept a fee", "&eof &a$" + tag.getPrice() + " &eto purchase this tag!"))
          .inventoryManager(inventoryManager)
          .onChoiceMade(aBoolean -> {
            if (aBoolean) {
              if (!Tagger.economy.has(player, tag.getPrice())) {
                player.sendMessage(Utils.format("Insufficient Funds"));
                return;
              }

              Tagger.economy.withdrawPlayer(player, tag.getPrice());
              Optional<AccountLink> optional = DatabaseHelper.getInstance().getCompanyByName("Admins").getAccounts().stream().filter(accountLink -> accountLink.getAccount().getName().equalsIgnoreCase("Main")).findAny();
              if (optional.isPresent()) {
                AccountLink accountLink = optional.get();
                Account account = accountLink.getAccount();
                account.addBalance(tag.getPrice());
                DatabaseHelper.getInstance().getDatabaseManager().updateAccount(account);
                DatabaseHelper.getInstance().getDatabaseManager().logTransaction(new Transaction(accountLink, player.getUniqueId(), "Purchase of \"" + tag.getName() + "\" tag from " + player.getName(), tag.getPrice()));
              }
              player.sendMessage(Utils.format("Successfully purchased tag!"));
            }
          })
          .open(player)));
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
  }

  @Override
  public void update(Player player, InventoryContents contents) {

  }
}