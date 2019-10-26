package io.github.jroy.tagger.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

  public static ItemStack item(Material material, String name, String... lore) {
    return item(material, name, false, lore);
  }

  public static ItemStack item(Material material, String name, boolean shine, String... lore) {
    return item(material, name, shine, Arrays.asList(lore));
  }

  public static ItemStack item(Material material, String name, boolean shine, List<String> lore) {
    ItemStack itemStack = new ItemStack(material);
    ItemMeta itemMeta = itemStack.getItemMeta();
    //noinspection ConstantConditions
    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
    List<String> lores = new ArrayList<>();
    for (String curLore : lore) {
      lores.add(ChatColor.translateAlternateColorCodes('&', curLore));
    }
    itemMeta.setLore(lores);
    Enchantment enchantment = EnchantmentWrapper.getByName("shine");
    if (shine && enchantment != null) {
      itemMeta.addEnchant(enchantment, 1, true);
    }
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  public static String format(String message) {
    return ChatColor.AQUA + "Tagger>> " + ChatColor.YELLOW + message;
  }
}
