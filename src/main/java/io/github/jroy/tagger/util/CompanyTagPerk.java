package io.github.jroy.tagger.util;

import dev.tycho.stonks.api.event.CompanyKickEvent;
import dev.tycho.stonks.api.event.CompanyRenameEvent;
import dev.tycho.stonks.api.perks.CompanyPerk;
import dev.tycho.stonks.model.core.Company;
import dev.tycho.stonks.model.core.Member;
import io.github.jroy.tagger.sql.DatabaseManager;
import io.github.jroy.tagger.sql.Tag;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class CompanyTagPerk extends CompanyPerk implements Listener {

  private final DatabaseManager databaseManager;

  public CompanyTagPerk(Plugin plugin, DatabaseManager databaseManager) {
    super(plugin, "Company Tag", Material.NAME_TAG, 300000, "This perk allows you and your company members to have",
        "a custom tag in front if your name. This tag cam be selected from",
        "the /tags menu. This tag has your company name as the tag.");
    this.databaseManager = databaseManager;
  }

  @Override
  public void onPurchase(Company company, Member purchaser) {
    try {
      databaseManager.createTag("company_" + company.pk, fetchTag(company.name), 0);
      Tag tag = databaseManager.getCachedTags().get("company_" + company.pk);
      for (Member member : company.members) {
        databaseManager.awardTag(member.playerUUID, tag);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @EventHandler
  public void onCompanyRename(CompanyRenameEvent event) {
    if (databaseManager.getCachedTags().containsKey("company_" + event.getCompany().pk)) {
      try {
        databaseManager.setText(databaseManager.getCachedTags().get("company_" + event.getCompany().pk), fetchTag(event.getNewName()));
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  @EventHandler
  public void onCompanyKick(CompanyKickEvent event) {
    if (databaseManager.getCachedTags().containsKey("company_" + event.getCompany().pk)
        && databaseManager.getOwnedTags().containsKey(event.getKickedPlayer().getUniqueId())
        && databaseManager.getOwnedTags().get(event.getKickedPlayer().getUniqueId()).stream().anyMatch(p -> p.getName().equals("company_" + event.getCompany().pk))) {
      try {
        databaseManager.revokeTag(event.getKickedPlayer().getUniqueId(), databaseManager.getCachedTags().get("company_" + event.getCompany().pk));
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  private static String fetchTag(String name) {
    return "&0[&7" + name + "&0]";
  }
}
