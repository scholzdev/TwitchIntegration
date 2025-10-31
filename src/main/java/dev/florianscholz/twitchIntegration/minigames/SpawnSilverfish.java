package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Silverfish;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SpawnSilverfish extends GameEvent {

    private final TwitchIntegration plugin;
    private final int amount = 10;
    private BukkitTask task;

    public SpawnSilverfish(TwitchIntegration plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Silverfish";
    }

    @Override
    public String getDescription() {
        return "Spawn Silverfish";
    }

    @Override
    public long getDuration() {
        return 200;
    }

    @Override
    public String getVotingName() {
        return "silverfish";
    }

    @Override
    protected void onStart() {
        for(int i = 0; i < amount; i++) {
            spawnSilverfish();
        }
    }

    private void spawnSilverfish() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.getWorld().spawn(player.getLocation(), Silverfish.class);
                });
            }
        }.runTaskLater(plugin, 20);
    }


    @Override
    protected void onFinish() {

    }


}
