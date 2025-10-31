package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OneHP extends GameEvent {

    private final Map<UUID, Double> originalHealth = new HashMap<>();

    public OneHP(TwitchIntegration plugin) {
        super(plugin);
    }

    @Override
    protected void onStart() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            originalHealth.put(player.getUniqueId(), player.getHealth());
            player.setHealth(1.0);
        });


        new BukkitRunnable() {
            @Override
            public void run() {
                if(!isRunning()) {
                    cancel();
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> {
                    if(player.getHealth() > 1.0) {
                        player.setHealth(1.0);
                    }
                });
            }
        }.runTaskTimer(plugin, 0, 10);

    }

    @Override
    protected void onFinish() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setHealth(originalHealth.get(player.getUniqueId()));
        });
    }

    @EventHandler
    public void onRegenerate(EntityRegainHealthEvent event) {
        if(event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @Override
    public String getName() {
        return "One HP";
    }

    @Override
    public String getDescription() {
        return "Survive at 1 HP";
    }

    @Override
    public long getDuration() {
        return 600;
    }

    @Override
    public String getVotingName() {
        return "one_hp";
    }
}
