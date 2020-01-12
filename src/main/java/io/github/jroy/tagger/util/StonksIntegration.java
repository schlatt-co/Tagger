package io.github.jroy.tagger.util;

import dev.tycho.stonks.api.StonksAPI;
import io.github.jroy.tagger.Tagger;
import io.github.jroy.tagger.sql.DatabaseManager;
import org.bukkit.Bukkit;

import static org.bukkit.Bukkit.getLogger;

public class StonksIntegration {
  public StonksIntegration(Tagger tagger, DatabaseManager databaseManager) {
    getLogger().info("Loading Stonks Integration...");
    CompanyTagPerk perk = new CompanyTagPerk(tagger, databaseManager);
    Bukkit.getServer().getPluginManager().registerEvents(perk, tagger);
    StonksAPI.registerPerk(perk);
  }
}
