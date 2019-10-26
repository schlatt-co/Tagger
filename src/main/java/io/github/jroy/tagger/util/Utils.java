package io.github.jroy.tagger.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

  public static ItemStack item(Material material, String name, String... lore) {
    return item(material, name, Arrays.asList(lore));
  }

  public static ItemStack item(Material material, String name, List<String> lore) {
    ItemStack itemStack = new ItemStack(material);
    ItemMeta itemMeta = itemStack.getItemMeta();
    //noinspection ConstantConditions
    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
    List<String> lores = new ArrayList<>();
    for (String curLore : lore) {
      lores.add(ChatColor.translateAlternateColorCodes('&', curLore));
    }
    itemMeta.setLore(lores);
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  public static String format(String message) {
    return ChatColor.AQUA + "Tagger>> " + ChatColor.YELLOW + message;
  }
}
