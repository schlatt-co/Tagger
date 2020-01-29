package io.github.jroy.tagger.sql;

import io.github.jroy.tagger.events.TaggerTagUpdateEvent;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DatabaseManager implements Listener {

  private static final Tag noneTag = new Tag(1, "none", 0, "");

  private BasicDataSource dataSource = new BasicDataSource();

  @Getter
  private HashMap<String, Tag> cachedTags;
  @Getter
  private HashMap<UUID, Tag> activeTags = new HashMap<>();
  @Getter
  private HashMap<UUID, List<Tag>> ownedTags = new HashMap<>();

  public DatabaseManager(JavaPlugin plugin) throws ClassNotFoundException, SQLException {
    plugin.getLogger().info("Connecting to database...");
    synchronized (this) {
      Class.forName("com.mysql.cj.jdbc.Driver");
      dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
      dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/tagger?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=GMT");
      dataSource.setUsername(plugin.getConfig().getString("mysql.username"));
      dataSource.setPassword(plugin.getConfig().getString("mysql.password"));
      dataSource.setMinIdle(5);
      dataSource.setMaxTotal(10);
      dataSource.setMaxOpenPreparedStatements(100);
    }
    plugin.getLogger().info("Connected!");
    plugin.getLogger().info("Initializing tables...");
    Connection connection = getConnection();
    connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `tags` ( `id` INT(15) NOT NULL AUTO_INCREMENT , `name` VARCHAR(255) NOT NULL , `price` INT(255) NOT NULL DEFAULT '0' , `text` VARCHAR(255) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
    if (!isTag("none")) {
      createTag("none", "", 0);
    }
    connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `usertags` ( `id` INT(15) NOT NULL AUTO_INCREMENT , `tagname` VARCHAR(255) NOT NULL , `uuid` VARCHAR(255) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
    connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `activetags` ( `id` INT(15) NOT NULL AUTO_INCREMENT , `uuid` VARCHAR(255) NOT NULL , `tagname` VARCHAR(255) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
    closeConnections(connection, null);
    plugin.getLogger().info("Initialized tables!");
    plugin.getLogger().info("Processing tags...");
    cachedTags = getTags();
    plugin.getLogger().info("Processed tags!");
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
    try {
      syncPlayer(event.getUniqueId());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void syncCache() throws SQLException {
    cachedTags = getTags();
    activeTags.clear();
    ownedTags.clear();
    for (Player curPlayer : Bukkit.getOnlinePlayers()) {
      syncPlayer(curPlayer.getUniqueId());
    }
  }

  private void syncPlayer(UUID uuid) throws SQLException {
    if (!hasActiveTag(uuid)) {
      createActiveTag(uuid);
    }

    Tag tag = getActiveTag(uuid);
    if (!hasPermission(uuid, tag)) {
      setActiveTag(uuid, noneTag);
      tag = noneTag;
    }
    activeTags.put(uuid, tag);
    ownedTags.put(uuid, getUserTags(uuid));
    Bukkit.getPluginManager().callEvent(new TaggerTagUpdateEvent(uuid, tag));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLeave(PlayerQuitEvent event) {
    activeTags.remove(event.getPlayer().getUniqueId());
    ownedTags.remove(event.getPlayer().getUniqueId());
  }

  public void setPrice(Tag tag, int newPrice) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("UPDATE `tags` SET price = ? WHERE `name` = ?;");
    statement.setInt(1, newPrice);
    statement.setString(2, tag.getName());
    statement.executeUpdate();
    syncCache();
    closeConnections(connection, statement);
  }

  public void setText(Tag tag, String text) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("UPDATE `tags` SET `text` = ? WHERE `name` = ?;");
    statement.setString(1, text);
    statement.setString(2, tag.getName());
    statement.executeUpdate();
    syncCache();
    closeConnections(connection, statement);
  }

  public void awardTag(UUID uuid, Tag tag) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("INSERT INTO `usertags` (uuid, tagname) VALUES (?, ?);");
    statement.setString(1, uuid.toString());
    statement.setString(2, tag.getName());
    statement.executeUpdate();
    ownedTags.get(uuid).add(tag);
    closeConnections(connection, statement);
  }

  public void revokeTag(UUID uuid, Tag tag) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("DELETE FROM `usertags` WHERE uuid = ? AND tagname = ?");
    statement.setString(1, uuid.toString());
    statement.setString(2, tag.getName());
    statement.executeUpdate();
    ownedTags.get(uuid).remove(tag);
    closeConnections(connection, statement);
  }

  private List<Tag> getUserTags(UUID uuid) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("SELECT  * FROM `usertags` WHERE uuid = ?;");
    statement.setString(1, uuid.toString());
    ResultSet set = statement.executeQuery();
    List<Tag> tags = new ArrayList<>();
    tags.add(noneTag);
    while (set.next()) {
      tags.add(cachedTags.get(set.getString("tagname")));
    }
    set.close();
    closeConnections(connection, statement);
    return tags;
  }

  public boolean hasPermission(UUID uuid, Tag tag) {
    if (tag.getName().equalsIgnoreCase("none")) {
      return true;
    }

    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM `usertags` WHERE uuid = ? AND tagname = ?;");
      statement.setString(1, uuid.toString());
      statement.setString(2, tag.getName());
      boolean hasPerm = statement.executeQuery().next();
      closeConnections(connection, statement);
      return hasPerm;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public void setActiveTag(UUID uuid, Tag tag) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("UPDATE `activetags` SET tagname = ? WHERE uuid = ?;");
    statement.setString(1, tag.getName());
    statement.setString(2, uuid.toString());
    statement.executeUpdate();
    activeTags.put(uuid, tag);
    closeConnections(connection, statement);
  }

  private boolean hasActiveTag(UUID uuid) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM `activetags` WHERE uuid = ?;");
      statement.setString(1, uuid.toString());
      boolean hasTag = statement.executeQuery().next();
      closeConnections(connection, statement);
      return hasTag;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  private void createActiveTag(UUID uuid) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("INSERT INTO `activetags` (uuid, tagname) VALUES (?, ?);");
    statement.setString(1, uuid.toString());
    statement.setString(2, "none");
    statement.executeUpdate();
    closeConnections(connection, statement);
  }

  private Tag getActiveTag(UUID uuid) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM `activetags` WHERE uuid = ?;");
      statement.setString(1, uuid.toString());
      ResultSet set = statement.executeQuery();
      set.next();
      Tag tag = getTag(set.getString("tagname"));
      set.close();
      closeConnections(connection, statement);
      return tag;
    } catch (SQLException e) {
      e.printStackTrace();
      return noneTag;
    }
  }

  private HashMap<String, Tag> getTags() throws SQLException {
    Connection connection = getConnection();
    ResultSet set = connection.createStatement().executeQuery("SELECT * FROM `tags`;");
    HashMap<String, Tag> tags = new HashMap<>();
    while (set.next()) {
      tags.put(set.getString("name"), new Tag(set.getInt("id"), set.getString("name"), set.getInt("price"), set.getString("text")));
    }
    set.close();
    closeConnections(connection, null);
    return tags;
  }

  private boolean isTag(String tagName) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM `tags` WHERE `name` = ?;");
      statement.setString(1, tagName);
      boolean isTag = statement.executeQuery().next();
      closeConnections(connection, statement);
      return isTag;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public void createTag(String name, String text, int price) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement("INSERT INTO `tags` (`name`, `text`, `price`) VALUE (?, ?, ?);");
    statement.setString(1, name);
    statement.setString(2, text);
    statement.setInt(3, price);
    statement.executeUpdate();
    cachedTags = getTags();
    closeConnections(connection, statement);
  }

  private Tag getTag(String name) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM `tags` WHERE `name` = ?;");
    statement.setString(1, name);
    ResultSet set = statement.executeQuery();
    set.next();
    Tag tag = new Tag(set.getInt("id"), set.getString("name"), set.getInt("price"), set.getString("text"));
    set.close();
    closeConnections(connection, statement);
    return tag;
  }

  private Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @SneakyThrows
  private void closeConnections(Connection connection, PreparedStatement statement) {
    if (connection.isClosed()) {
      connection.close();
    }
    if (statement != null && statement.isClosed()) {
      statement.close();
    }
  }
}
