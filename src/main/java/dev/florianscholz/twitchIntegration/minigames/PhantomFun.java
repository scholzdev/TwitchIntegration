package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class PhantomFun extends GameEvent {

    private BukkitRunnable spawner;

    public PhantomFun(TwitchIntegration plugin) {
        super(plugin);
    }

    @Override
    protected void onStart() {

        spawner = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning()) {
                    cancel();
                    return;
                }

                Random random = new Random();

                Bukkit.getOnlinePlayers().forEach(player -> {
                    int count = 2 + random.nextInt(2);
                    Location loc = player.getLocation().add(random.nextDouble(4), random.nextDouble(2) + 1, random.nextDouble(4));

                    for(int i = 0; i < count; i++) {
                        Phantom phantom = player.getWorld().spawn(loc, Phantom.class);
                        phantom.setTarget(player);
                        phantom.setPersistent(true);
                        phantom.setAware(true);
                        phantom.customName(Component.text("Nightmare"));
                    }
                });

            }
        };

        spawner.runTaskTimer(plugin, 0, 60);
    }

    @Override
    protected void onFinish() {
        if(spawner != null) {
            spawner.cancel();
        }

        Bukkit.getWorlds().forEach(w -> w.getEntities().stream()
                .filter(e -> e instanceof Phantom && e.customName() != null && e.customName().equals(Component.text("Nightmare")))
                .forEach(Entity::remove));
    }

    @Override
    public String getName() {
        return "Phantom Fun";
    }

    @Override
    public String getDescription() {
        return "Fun with phantoms";
    }

    @Override
    public long getDuration() {
        return 200;
    }

    @Override
    public String getVotingName() {
        return "phantom_fun";
    }
}
