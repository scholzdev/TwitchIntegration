package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SpleefEvent extends AbstractSimpleGameProvider {

    private static final int ARENA_RADIUS = 15;
    private static final Material ARENA_MATERIAL = Material.SNOW_BLOCK;
    private static final Material FLOOR_MATERIAL = Material.LAVA;
    private final Set<UUID> activePlayers = new HashSet<>();
    private static final Map<Location, Material> brokenBlocks = new HashMap<>();
    private static UUID winnerUUID = null;
    
    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Spleef")
                .description("Break snow blocks under other players to make them fall!")
                .votingName("spleef")
                .duration(20 * 60 * 5)
                .cleanupSpawned()
                .saveAndRestoreInventory()
                .onTeleport("spleef", 137, 62, 7)
                .onStartWithEvent(event -> {
                    World world = Bukkit.getWorld("spleef");
                    if (world == null) {
                        Bukkit.getLogger().severe("Could not find world 'spleef'! Event cannot start properly.");
                        return;
                    }
                    
                    Bukkit.getLogger().info("Found world 'spleef'! Starting Spleef event...");
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        Location spawnLoc = getRandomSpawnLocation(world);
                        player.teleport(spawnLoc);
                        
                        ItemStack shovel = new ItemStack(Material.NETHERITE_SHOVEL);
                        ItemMeta meta = shovel.getItemMeta();
                        if (meta != null) {
                            meta.setUnbreakable(true);
                            meta.displayName(net.kyori.adventure.text.Component.text("Spleef Shovel").color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE));
                            meta.addEnchant(Enchantment.EFFICIENCY, 4, true);
                            shovel.setItemMeta(meta);
                        }
                        player.getInventory().addItem(shovel);
                        player.setGameMode(GameMode.SURVIVAL);
                        
                        activePlayers.add(player.getUniqueId());
                    }
                })
                .on(BlockBreakEvent.class, (breakEvent, gameEvent) -> {
                    Block block = breakEvent.getBlock();
                    
                    if (block.getType() != ARENA_MATERIAL) {
                        breakEvent.setCancelled(true);
                        return;
                    }
                    
                    brokenBlocks.put(block.getLocation().clone(), block.getType());
                    
                    breakEvent.setDropItems(false);
                    
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);
                    block.getWorld().spawnParticle(Particle.SNOWFLAKE, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0.05);
                })
                .on(PlayerMoveEvent.class, (moveEvent, gameEvent) -> {
                    Player player = moveEvent.getPlayer();
                    
                    if (!activePlayers.contains(player.getUniqueId())) {
                        return;
                    }
                    
                    if (moveEvent.getTo() != null && 
                        (moveEvent.getTo().getBlock().getType() == FLOOR_MATERIAL || 
                        moveEvent.getTo().clone().subtract(0, 0.1, 0).getBlock().getType() == FLOOR_MATERIAL)) {
                        
                        activePlayers.remove(player.getUniqueId());
                        
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage("§c" + player.getName() + " §egot eliminated!");
                        }
                        
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.5f, 1.0f);
                        }
                        
                        player.setGameMode(GameMode.SPECTATOR);
                        
                        if (activePlayers.size() == 1) {
                            UUID winnerId = activePlayers.iterator().next();
                            Player winner = Bukkit.getPlayer(winnerId);
                            
                            if (winner != null) {
                                World spleefWorld = Bukkit.getWorld("spleef");
                                if (spleefWorld == null) {
                                    return;
                                }
                                Location randomLocation = getRandomSpawnLocation(spleefWorld);
                                winner.teleport(randomLocation);
                                winnerUUID = winner.getUniqueId();
                                
                                Bukkit.getScheduler().runTaskLater(plugin, gameEvent::stop, 40L);
                            }
                        }
                    }
                })
                .on(PlayerDeathEvent.class, (deathEvent, gameEvent) -> {
                    Player player = deathEvent.getEntity();
                    
                    activePlayers.remove(player.getUniqueId());
                    
                    deathEvent.getDrops().clear();
                    deathEvent.setDroppedExp(0);
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage("§c" + player.getName() + " §egot eliminated!");
                    }
                })
                .on(PlayerRespawnEvent.class, (respawnEvent, gameEvent) -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        respawnEvent.getPlayer().setGameMode(GameMode.SPECTATOR);
                    }, 1L);
                })
                .withReward(
                    (player, gameEvent) -> player.getUniqueId().equals(winnerUUID),
                    (player, gameEvent) -> {
                        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage("§6" + player.getName() + " §ewon spleef!");
                        }
                    }
                )
                .onFinish(() -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!brokenBlocks.isEmpty()) {
                            Bukkit.getLogger().info("Restoring " + brokenBlocks.size() + " blocks for Spleef arena...");
                            
                            for (Map.Entry<Location, Material> entry : brokenBlocks.entrySet()) {
                                entry.getKey().getBlock().setType(entry.getValue());
                            }
                            
                            brokenBlocks.clear();
                            Bukkit.getLogger().info("Spleef arena restored successfully!");
                        }
                        
                        winnerUUID = null;
                    }, 10L);
                })
                .build();
    }
    
    private Location getRandomSpawnLocation(World world) {
        if (world == null) {
            return new Location(Bukkit.getWorlds().get(0), 0, 100, 0);
        }
        
        Random random = new Random();
        Location arenaCenter = new Location(world, 137, 62, 7);
        
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * (ARENA_RADIUS - 2); 
        
        int x = (int) (Math.cos(angle) * distance);
        int z = (int) (Math.sin(angle) * distance);
        
        return new Location(
            world, 
            arenaCenter.getX() + x, 
            arenaCenter.getY() + 5, 
            arenaCenter.getZ() + z,
            random.nextFloat() * 360,
            0
        );
    }
}
