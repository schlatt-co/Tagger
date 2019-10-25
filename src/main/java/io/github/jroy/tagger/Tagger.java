package io.github.jroy.tagger;

import io.github.jroy.tagger.sql.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class Tagger extends JavaPlugin {

  @Override
  public void onEnable() {
    getLogger().info("Enabling Tagger...");
    getLogger().info("Loading Config...");
    getConfig().addDefault("mysql.username", "");
    getConfig().addDefault("mysql.password", "");
    getConfig().options().copyDefaults(true);
    saveConfig();
    reloadConfig();
    getLogger().info("Loaded Config!");
    getLogger().info("Loading DatabaseManager...");
    DatabaseManager databaseManager;
    try {
      databaseManager = new DatabaseManager(this);
    } catch (ClassNotFoundException | SQLException e) {
      getLogger().severe("Error while initializing DatabaseManager, disabling...");
      e.printStackTrace();
      getPluginLoader().disablePlugin(this);
      return;
    }
    getServer().getPluginManager().registerEvents(databaseManager, this);
    getLogger().info("Loaded DatabaseManager!");
  }
}
