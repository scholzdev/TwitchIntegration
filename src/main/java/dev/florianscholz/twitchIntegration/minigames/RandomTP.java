package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RandomTP extends GameEvent {

    public RandomTP(TwitchIntegration plugin) {
        super(plugin);
    }

    private void teleportNearby(Player player) {
        Location current = player.getLocation();
        Location random = current.clone().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
        
        player.getWorld().spawnParticle(Particle.PORTAL, current, 50);
        player.teleport(random, PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT);
        player.getWorld().spawnParticle(Particle.PORTAL, random, 50);
        player.playSound(random, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    @Override
    protected void onStart() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!isRunning()) {
                    cancel();
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> {
                    teleportNearby(player);
                });
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    @Override
    protected void onFinish() {

    }

    @Override
    public String getName() {
        return "Random TP";
    }

    @Override
    public String getDescription() {
        return "Teleports you randomly to nearby locations";
    }

    @Override
    public long getDuration() {
        return 200;
    }

    @Override
    public String getVotingName() {
        return "random_tp";
    }
}
