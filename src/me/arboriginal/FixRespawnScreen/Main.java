package me.arboriginal.FixRespawnScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.minecraft.server.v1_14_R1.DimensionManager;
import net.minecraft.server.v1_14_R1.EnumGamemode;
import net.minecraft.server.v1_14_R1.PacketPlayOutRespawn;
import net.minecraft.server.v1_14_R1.WorldType;

public class Main extends JavaPlugin implements Listener {
  private FileConfiguration                                 config;
  private HashMap<String, HashMap<String, ArrayList<UUID>>> chunks;

  // -- JavaPlugin methods --------------------------------------------------------------------------------------------

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("frs-reload")) {
      sender.sendMessage("Configuration reloaded");
      reloadConfig();
      return true;
    }

    return super.onCommand(sender, command, label, args);
  }

  @Override
  public void onDisable() {
    super.onDisable();
    HandlerList.unregisterAll((JavaPlugin) this);

    for (String worldName : chunks.keySet()) {
      World world = getServer().getWorld(worldName);

      for (String chunkKey : chunks.get(worldName).keySet()) {
        Chunk chunk = getChunkByKey(world, chunkKey);
        if (chunk != null) chunk.setForceLoaded(false);
      }
    }
  }

  @Override
  public void onEnable() {
    super.onEnable();
    reloadConfig();

    chunks = new HashMap<String, HashMap<String, ArrayList<UUID>>>();

    for (World world : getServer().getWorlds()) {
      chunks.put(world.getName(), new HashMap<String, ArrayList<UUID>>());

      for (Player player : world.getPlayers())
        if (player.isDead()) {
          lockChunk(player);
          obfuscateRespawnScreen(player);
        }
    }

    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();

    saveDefaultConfig();
    config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();
  }

  // -- Listener methods ----------------------------------------------------------------------------------------------

  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();

    lockChunk(player);
    obfuscateRespawnScreen(player);
  }

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (!player.isDead()) return;

    lockChunk(player);
  }

  @EventHandler
  private void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    if (player.isDead()) releaseChunk(player);
  }

  @EventHandler
  private void onPlayerRespawn(PlayerRespawnEvent event) {
    releaseChunk(event.getPlayer());
  }

  // -- Listener methods, monitoring ----------------------------------------------------------------------------------

  @EventHandler
  private void onChunkLoad(ChunkLoadEvent event) {
    log(event.getChunk(), "Unload");
  }

  @EventHandler
  private void onChunkUnload(ChunkUnloadEvent event) {
    log(event.getChunk(), "Unload");
  }

  // -- Helper methods ------------------------------------------------------------------------------------------------

  private String chunkKey(Chunk chunk) {
    return chunk.getX() + "_" + chunk.getZ();
  }

  private Chunk getChunkByKey(World world, String chunkKey) {
    Chunk    chunk = null;
    String[] parts = chunkKey.split("_");

    if (parts.length == 2) chunk = world.getChunkAt(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

    return chunk;
  }

  private void log(Chunk chunk, String action) {
    if (!config.getBoolean("monitor_chunks")) return;
    getLogger().info(action + " chunk: " + chunk.getX() + "/" + chunk.getZ() + "@" + chunk.getWorld());
  }

  private void lockChunk(Player player) {
    Chunk  chunk = player.getLocation().getChunk();
    String world = player.getWorld().getName(), chunkKey = chunkKey(chunk);

    if (!chunks.get(world).containsKey(chunkKey))
      chunks.get(world).put(chunkKey, new ArrayList<UUID>());

    chunks.get(world).get(chunkKey).add(player.getUniqueId());
    chunk.setForceLoaded(true);
  }

  private void releaseChunk(Player player) {
    UUID   uuid  = player.getUniqueId();
    Chunk  chunk = player.getLocation().getChunk();
    String world = player.getWorld().getName(), chunkKey = chunkKey(chunk);

    if (!chunks.get(world).containsKey(chunkKey) || !chunks.get(world).get(chunkKey).contains(uuid)) return;

    chunks.get(world).get(chunkKey).remove(uuid);
    if (chunks.get(world).get(chunkKey).size() == 0) chunk.setForceLoaded(false);
  }

  private void obfuscateRespawnScreen(Player player) {
    if (!config.getBoolean("fix_transparent_map")) return;

    DimensionManager dimension = (player.getWorld().getEnvironment() == World.Environment.THE_END)
        ? DimensionManager.NETHER
        : DimensionManager.THE_END;

    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
        new PacketPlayOutRespawn(dimension, WorldType.NORMAL, EnumGamemode.ADVENTURE));
  }
}
