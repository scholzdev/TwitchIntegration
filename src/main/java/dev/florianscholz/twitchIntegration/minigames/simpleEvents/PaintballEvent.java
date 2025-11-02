package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PaintballEvent extends AbstractSimpleGameProvider {

    private static final int SNOWBALLS_PER_PLAYER = 16;

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Paintball")
                .description("Hit other players with snowballs to score points!")
                .votingName("paintball")
                .duration(20 * 60 * 5)
                .cleanupSpawned()
                .saveAndRestoreInventory()
                .onTeleport("paintball", 67, -48, 75)
                .onStartWithEvent(event -> {
                    World paintballWorld = Bukkit.getWorld("paintball");
                    if(paintballWorld == null) {
                        Bukkit.getLogger().severe("Could not find world 'paintball'! Event cannot start properly.");
                        return;
                    } 
                    
                    Bukkit.getLogger().info("Found world 'paintball'! Starting event...");

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        Location spawnLocation = getRandomSpawnLocation(paintballWorld);
                        player.teleport(spawnLocation);
                        
                        player.getInventory().addItem(new ItemStack(Material.SNOWBALL, SNOWBALLS_PER_PLAYER));
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    
                })
                .on(ProjectileLaunchEvent.class, (launchEvent, gameEvent) -> {
                    if (launchEvent.getEntity() instanceof Snowball && launchEvent.getEntity().getShooter() instanceof Player) {
                        Player shooter = (Player) launchEvent.getEntity().getShooter();
                        
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (gameEvent.isRunning() && shooter.isOnline()) {
                                shooter.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
                            }
                        }, 1L);
                    }
                })
                .on(ProjectileHitEvent.class, (hitEvent, gameEvent) -> {
                    if (hitEvent.getEntity() instanceof Snowball && hitEvent.getHitEntity() instanceof Player) {
                        Player target = (Player) hitEvent.getHitEntity();
                        
                        if (hitEvent.getEntity().getShooter() instanceof Player) {
                            Player shooter = (Player) hitEvent.getEntity().getShooter();
                            
                            if (!shooter.equals(target)) {
                                gameEvent.trackKill(shooter);
                                
                                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                                shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                                
                                shooter.sendMessage("§aYou hit §e" + target.getName() + "§a! Kills: §e" + gameEvent.getKillCount(shooter));
                                target.sendMessage("§cYou were hit by §e" + shooter.getName() + "§c!");
                                
                                shooter.getInventory().addItem(new ItemStack(Material.SNOWBALL, 2));
                                
                                World paintballWorld = Bukkit.getWorld("paintball");
                                if (paintballWorld == null) {
                                    return;
                                }
                                Location randomLocation = getRandomSpawnLocation(paintballWorld);
                                
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    target.teleport(randomLocation);
                                    target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
                                    target.sendMessage("§eYou have been teleported to a random location!");
                                }, 5L);
                            }
                        }
                    }
                })
                .on(EntityDamageByEntityEvent.class, (damageEvent, gameEvent) -> {
                    if (damageEvent.getDamager() instanceof Snowball) {
                        damageEvent.setCancelled(true);
                    }
                })
                .on(PlayerDeathEvent.class, (deathEvent, gameEvent) -> {
                    Player player = deathEvent.getEntity();
                    Bukkit.getLogger().info(player.getName() + " ist während des Paintball-Events gestorben.");
                    
                    deathEvent.getDrops().clear();
                    deathEvent.setDroppedExp(0);
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage("§c" + player.getName() + " §eist gestorben und wird respawnen!");
                    }
                })
                .on(PlayerRespawnEvent.class, (respawnEvent, gameEvent) -> {
                    if (gameEvent.isRunning()) {
                        Player player = respawnEvent.getPlayer();
                        World world = Bukkit.getWorld("paintball");
                        
                        if (world != null) {
                            Location spawnLocation = getRandomSpawnLocation(world);
                            respawnEvent.setRespawnLocation(spawnLocation);
                            
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                player.getInventory().addItem(new ItemStack(Material.SNOWBALL, SNOWBALLS_PER_PLAYER));
                                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
                                player.sendMessage("§eYou have respawned at a random location!");
                            }, 5L); 
                        }
                    }
                })
                .finishWhen(event -> {
                    int targetKills = Bukkit.getOnlinePlayers().size() * 5;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (event.getKillCount(player) >= targetKills) {
                            return true;
                        }
                    }
                    return false;
                })
                .withReward(
                    (player, gameEvent) -> {
                        int playerKills = gameEvent.getKillCount(player);
                        int maxKills = 0;
                        
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int kills = gameEvent.getKillCount(p);
                            if (kills > maxKills) {
                                maxKills = kills;
                            }
                        }
                        
                        return playerKills > 0 && playerKills == maxKills;
                    },
                    (player, gameEvent) -> {
                        int kills = gameEvent.getKillCount(player);
                        player.getInventory().addItem(new ItemStack(Material.DIAMOND, kills));
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage("§6" + player.getName() + " §ewon the Paintball event with §6" + kills + " §ehits!");
                        }
                    }
                )
                .build();
    }

    /**
     * Gets a random spawn location in the paintball arena
     * @param world The paintball world
     * @return A random spawn location
     */
    private Location getRandomSpawnLocation(World world) {
        if (world == null) {
            Bukkit.getLogger().severe("Cannot get spawn location: world is null!");
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        
        Random random = new Random();
        List<Location> spawnLocations = new ArrayList<>();
        
        spawnLocations.add(new Location(world, 37, -48, 30, 45, 0));
        spawnLocations.add(new Location(world, 95, -48, 29, 135, 0));
        spawnLocations.add(new Location(world, 99, -48, 119, 225, 0));
        spawnLocations.add(new Location(world, 39, -48, 116, 315, 0));

        return spawnLocations.get(random.nextInt(spawnLocations.size()));
    }
}
