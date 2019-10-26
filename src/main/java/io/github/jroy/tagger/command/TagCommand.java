package io.github.jroy.tagger.command;

import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import io.github.jroy.tagger.gui.TagSelectorGUI;
import io.github.jroy.tagger.gui.TagShopGUI;
import io.github.jroy.tagger.sql.DatabaseManager;
import io.github.jroy.tagger.sql.Tag;
import io.github.jroy.tagger.util.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
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
import java.util.Optional;

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
              .id("tagShopGui")
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
          tag = tag.replaceFirst(args[0] + " ", "").replaceFirst(args[1] + " ", "").replaceFirst(args[2] + " ", "").trim();
          try {
            databaseManager.createTag(name, tag, price);
            player.sendMessage(Utils.format("Successfully created tag with name: " + ChatColor.RED + name + ChatColor.YELLOW + ", price: " + ChatColor.RED + "$" + price + ChatColor.YELLOW + ", and text, " + ChatColor.RED + tag));
          } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(Utils.format(ChatColor.RED + "Error while creating tag!"));
          }
          break;
        }
        case "award": {
          if (!player.hasPermission("tagger.admin")) {
            player.sendMessage(Utils.format("Insufficient Permissions!"));
            return true;
          }
          if (args.length < 3) {
            player.sendMessage(Utils.format("Correct Usage: /tags award <player> <name>"));
            return true;
          }

          Optional<? extends Player> optionalPlayer = Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equalsIgnoreCase(args[1])).findFirst();
          if (optionalPlayer.isEmpty()) {
            player.sendMessage(Utils.format("Player not found!"));
            return true;
          }
          Player target = optionalPlayer.get();

          String name = args[2];
          if (!databaseManager.getCachedTags().containsKey(name)) {
            player.sendMessage(Utils.format("Invalid tag!"));
            return true;
          }
          Tag tag = databaseManager.getCachedTags().get(name);

          if (databaseManager.hasPermission(target.getUniqueId(), tag)) {
            player.sendMessage(Utils.format("Player already has this tag!"));
            return true;
          }

          try {
            databaseManager.awardTag(target.getUniqueId(), tag);
            player.sendMessage(Utils.format("Successfully awarded tag!"));
            target.sendMessage(Utils.format("You've been awarded the " + StringUtils.capitalize(tag.getName()) + " tag! Use " + ChatColor.RED + "/tags" + ChatColor.YELLOW + " to apply it!"));
          } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(Utils.format(ChatColor.RED + "Error while awarding tag!"));
          }
          break;
        }
        case "revoke": {
          if (!player.hasPermission("tagger.admin")) {
            player.sendMessage(Utils.format("Insufficient Permissions!"));
            return true;
          }
          if (args.length < 3) {
            player.sendMessage(Utils.format("Correct Usage: /tags revoke <player> <name>"));
            return true;
          }

          Optional<? extends Player> optionalPlayer = Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equalsIgnoreCase(args[1])).findFirst();
          if (optionalPlayer.isEmpty()) {
            player.sendMessage(Utils.format("Player not found!"));
            return true;
          }
          Player target = optionalPlayer.get();

          String name = args[2];
          if (!databaseManager.getCachedTags().containsKey(name)) {
            player.sendMessage(Utils.format("Invalid tag!"));
            return true;
          }
          Tag tag = databaseManager.getCachedTags().get(name);

          if (!databaseManager.hasPermission(target.getUniqueId(), tag)) {
            player.sendMessage(Utils.format("Player doesn't have this tag!"));
            return true;
          }

          try {
            databaseManager.revokeTag(target.getUniqueId(), tag);
            player.sendMessage(Utils.format("Successfully revoked tag!"));
            target.sendMessage(Utils.format("You've been revoked of the " + StringUtils.capitalize(tag.getName()) + " tag!"));
          } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(Utils.format(ChatColor.RED + "Error while revoking tag!"));
          }
          break;
        }
        case "edit": {
          if (!player.hasPermission("tagger.admin")) {
            player.sendMessage(Utils.format("Insufficient Permissions!"));
            return true;
          }

          if (args.length < 4) {
            player.sendMessage(Utils.format("Correct Usage: /tags edit <tag> <price/text> <value>"));
            return true;
          }

          if (!databaseManager.getCachedTags().containsKey(args[1])) {
            player.sendMessage(Utils.format("Invalid tag!"));
            return true;
          }
          Tag tag = databaseManager.getCachedTags().get(args[1]);

          if (args[2].equalsIgnoreCase("price")) {
            if (!StringUtils.isNumeric(args[3])) {
              player.sendMessage(Utils.format("Invalid Price!"));
              return true;
            }
            try {
              databaseManager.setPrice(tag, Integer.parseInt(args[3]));
              player.sendMessage(Utils.format("Updated price successfully!"));
            } catch (SQLException e) {
              e.printStackTrace();
              player.sendMessage(Utils.format(ChatColor.RED + "Error while editing tag!"));
            }
          } else if (args[2].equalsIgnoreCase("text")) {
            StringBuilder tagBuilder = new StringBuilder();
            for (String arg : args) {
              tagBuilder.append(arg).append(" ");
            }
            String newText = tagBuilder.toString();
            newText = newText.replaceFirst(args[0] + " ", "").replaceFirst(args[1] + " ", "").replaceFirst(args[2] + " ", "").trim();
            try {
              databaseManager.setText(tag, newText);
              player.sendMessage(Utils.format("Updated text successfully!"));
            } catch (SQLException e) {
              e.printStackTrace();
              player.sendMessage(Utils.format(ChatColor.RED + "Error while editing tag!"));
            }
          } else {
            player.sendMessage(Utils.format("Invalid edit option!"));
            return true;
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
      return copyPartialMatches(args[0], Arrays.asList("create", "shop", "award", "revoke", "edit"));
    }
    if (args[0].equalsIgnoreCase("award") || args[0].equalsIgnoreCase("revoke")) {
      if (args.length == 2) {
        return matchPlayerName(args[1]);
      } else if (args.length == 3) {
        return copyPartialMatches(args[2], databaseManager.getCachedTags().keySet());
      }
    } else if (args[0].equalsIgnoreCase("edit")) {
      if (args.length == 2) {
        return copyPartialMatches(args[1], databaseManager.getCachedTags().keySet());
      } else if (args.length == 3) {
        return copyPartialMatches(args[2], Arrays.asList("text", "price"));
      }
    }
    return completions;
  }

  private List<String> copyPartialMatches(String search, Iterable<String> stack) {
    List<String> matches = new ArrayList<>();
    StringUtil.copyPartialMatches(search, stack, matches);
    return matches;
  }

  private List<String> matchPlayerName(String search) {
    List<String> playerNames = new ArrayList<>();
    Bukkit.getOnlinePlayers().forEach(o -> playerNames.add(o.getName()));
    return copyPartialMatches(search, playerNames);
  }
}
