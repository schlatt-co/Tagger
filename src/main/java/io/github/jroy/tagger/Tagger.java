package io.github.jroy.tagger;

import fr.minuskube.inv.InventoryManager;
import io.github.jroy.tagger.command.TagCommand;
import io.github.jroy.tagger.sql.DatabaseManager;
import io.github.jroy.tagger.util.ShineEnchantment;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Objects;

public class Tagger extends JavaPlugin {

  public static Economy economy;

  @Override
  public void onEnable() {
    getLogger().info("Loading Config...");
    getConfig().addDefault("mysql.username", "");
    getConfig().addDefault("mysql.password", "");
    getConfig().options().copyDefaults(true);
    saveConfig();
    reloadConfig();
    getLogger().info("Loaded Config!");
    getLogger().info("Loading DatabaseManager...");
    getLogger().info("Registering Glow Enchantment...");
    ShineEnchantment shineEnchantment;
    try {
      Field acceptingNew = Enchantment.class.getDeclaredField("acceptingNew");
      acceptingNew.setAccessible(true);
      acceptingNew.set(null, true);
      shineEnchantment = new ShineEnchantment();
      EnchantmentWrapper.registerEnchantment(shineEnchantment);
    } catch (Exception e) {
      getLogger().info("Error while registering glow enchantment, disabling...");
      e.printStackTrace();
      getPluginLoader().disablePlugin(this);
      return;
    }
    DatabaseManager databaseManager;
    try {
      databaseManager = new DatabaseManager(this, shineEnchantment);
    } catch (ClassNotFoundException | SQLException e) {
      getLogger().severe("Error while initializing DatabaseManager, disabling...");
      e.printStackTrace();
      getPluginLoader().disablePlugin(this);
      return;
    }
    getServer().getPluginManager().registerEvents(databaseManager, this);
    getLogger().info("Loaded DatabaseManager!");
    InventoryManager inventoryManager = new InventoryManager(this);
    inventoryManager.init();
    TagCommand tagCommand = new TagCommand(databaseManager, inventoryManager);
    Objects.requireNonNull(getCommand("tags")).setExecutor(tagCommand);
    Objects.requireNonNull(getCommand("tags")).setTabCompleter(tagCommand);
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp != null) {
      economy = rsp.getProvider();
    }

  }
}
