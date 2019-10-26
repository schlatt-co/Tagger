package io.github.jroy.tagger.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

public class ShineEnchantment extends EnchantmentWrapper {

  public ShineEnchantment() {
    super("shine");
  }

  @Override
  public boolean canEnchantItem(ItemStack item) {
    return true;
  }

  @Override
  public boolean conflictsWith(Enchantment other) {
    return false;
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public EnchantmentTarget getItemTarget() {
    return null;
  }

  @Override
  public int getMaxLevel() {
    return 10;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public int getStartLevel() {
    return 1;
  }
}