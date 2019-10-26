package io.github.jroy.tagger.command;

import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import io.github.jroy.tagger.gui.TagSelectorGUI;
import io.github.jroy.tagger.gui.TagShopGUI;
import io.github.jroy.tagger.sql.DatabaseManager;
import io.github.jroy.tagger.util.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TagCommand implements CommandExecutor, TabCompleter {

  private final DatabaseManager databaseManager;
  private final InventoryManager inventoryManager;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;

      if (args.length == 0) {
        SmartInventory.builder()
            .id("tagSelectorGui")
            .provider(new TagSelectorGUI(databaseManager, inventoryManager))
            .manager(inventoryManager)
            .title("Tag Selector")
            .build().open(player);
        return true;
      }

      switch (args[0]) {
        case "shop": {
          SmartInventory.builder()
              .id("tagSelectorGui")
              .provider(new TagShopGUI(databaseManager, inventoryManager))
              .manager(inventoryManager)
              .title("Tag Shop")
              .build().open(player);
          break;
        }
        case "create": {
          if (!player.hasPermission("tagger.admin")) {
            player.sendMessage(Utils.format("Insufficient Permissions!"));
            return true;
          }
          if (args.length < 4) {
            player.sendMessage(Utils.format("Correct Usage: /tags create <name> <price> <tag>"));
            return true;
          }

          String name = args[1];
          if (databaseManager.getCachedTags().containsKey(name)) {
            player.sendMessage(Utils.format("Tag name is already in use!"));
            return true;
          }

          if (!StringUtils.isNumeric(args[2])) {
            player.sendMessage("Price must be a number!");
            return true;
          }
          int price = Integer.parseInt(args[2]);

          StringBuilder tagBuilder = new StringBuilder();
          for (String arg : args) {
            tagBuilder.append(arg).append(" ");
          }
          String tag = tagBuilder.toString();
          tag = tag.replaceFirst(args[0] + " ", "").replaceFirst(args[1] + " ", "").replaceFirst(args[2] + " ", "");
          try {
            databaseManager.createTag(name, tag, price);
            player.sendMessage(Utils.format("Successfully created tag with name: " + ChatColor.RED + name + ChatColor.YELLOW + ", price: " + ChatColor.RED + "$" + price + ChatColor.YELLOW + ", and text, " + ChatColor.RED + tag));
          } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(Utils.format(ChatColor.RED + "Error while creating tag!"));
          }
          break;
        }
        default: {
          player.sendMessage(Utils.format("Invalid Argument!"));
          break;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    if (args.length == 1) {
      return copyPartialMatches(args[0], Arrays.asList("create", "shop"));
    }
    return completions;
  }

  private List<String> copyPartialMatches(String search, Iterable<String> stack) {
    List<String> matches = new ArrayList<>();
    StringUtil.copyPartialMatches(search, stack, matches);
    return matches;
  }
}
